# Enterprise Multi-Agent Data Integration Architecture
**Pattern: Configuration-Driven Apache Camel Saga & JSLT Transformation**

This architecture document outlines the design and prompt engineering required to use an agentic AI workflow to dynamically generate enterprise-grade data transformation pipelines. 

Instead of generating raw, compiled Java code (which introduces hallucination risks and CI/CD bottlenecks), the AI agents generate **declarative configuration files (YAML)** and **structural mapping scripts (JSLT)**. These artifacts are dynamically loaded by a static, high-performance Spring Boot execution engine.

---

## Part 1: The Target Application Blueprint
The AI agents construct configurations that fit into this exact Spring Boot directory structure. The Java shell remains static, while the `resources` folder is dynamically populated by the AI.

```text
data-integration-engine/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/wornkeys/integration/
│   │   │   └── IntegrationEngineApplication.java  <-- Static Bootloader
│   │   └── resources/
│   │       ├── application.yml                    <-- Static + AI Injected Env Vars
│   │       ├── camel/
│   │       │   └── <pipeline_id>.yaml             <-- AI Generated (Routing Agent)
│   │       └── mappings/
│   │           ├── <pipeline_id>-request.jslt     <-- AI Generated (Mapping Agent)
│   │           └── <pipeline_id>-response.jslt    <-- AI Generated (Mapping Agent)
│   └── test/
│       ├── java/com/wornkeys/integration/
│       │   └── DynamicPipelineTest.java           <-- Static Testcontainers Runner
│       └── resources/
│           ├── sample-inputs/
│           └── expected-outputs/
```

### The Runtime Execution Flow (Content Enricher Saga)
When the Spring Boot application runs, the Camel engine executes the AI-generated YAML route in this exact sequence:
1. **Ingest:** Consume from Kafka (Unmarshal XML to JSON if necessary).
2. **Dynamic Context:** Call `Validation API` (GET) to fetch external state.
3. **Validate:** Evaluate Kafka payload + API state against AI-generated rules. Route failures to DLQ.
4. **Transform Request:** Execute `<pipeline_id>-request.jslt` to build the target API contract.
5. **Transact:** Call `Final API` (POST/PUT).
6. **Transform Response:** Execute `<pipeline_id>-response.jslt` to build the final output contract.
7. **Egress:** Publish to Kafka (Marshal JSON back to XML if necessary).

---

## Part 2: The Agentic Workflow Configuration

Before triggering the AI agents, the Python orchestration layer (e.g., LangGraph, CrewAI) must aggregate all file samples, database configs, and rules into a single JSON context payload.

### The Global Context Payload
*Inject this object into the agent prompts below where variables are marked with `<INJECT_...>`.*

```json
{
  "pipeline_id": "trade-processing-v2",
  "input_format": "xml",
  "output_format": "json",
  "unified_input_samples": [... array of all input variations ...],
  "unified_output_samples": [... array of all expected outputs ...],
  "validation_api": { "url": "${VALIDATION_API_URL}", "method": "GET" },
  "final_api": { "url": "${FINAL_API_URL}", "method": "POST" },
  "overlay_rules": [
    "STATIC: Reject if 'amount' < 0",
    "DYNAMIC: Reject if 'input.accountId' status from validation_api is not 'ACTIVE'"
  ]
}
```

---

## Part 3: The System Prompt Chain

### Agent 1: The Dual-Contract Mapping Agent
**Role:** Generates the precise JSON-to-JSON structural mappings, completely decoupled from the transport layer.

**Prompt:**
```markdown
You are a Principal Data Contract Engineer. Your ONLY objective is to write two highly optimized JSLT scripts for an integration pipeline. 

You are strictly forbidden from writing Java, Python, or XML XSLT. You must assume all incoming data has already been unmarshaled into a standard JSON tree.

PIPELINE CONTEXT:
- Unified Input Samples: <INJECT_UNIFIED_INPUT_SAMPLES>
- Final Output Samples: <INJECT_UNIFIED_OUTPUT_SAMPLES>
- Final API Expected Request Body: <INJECT_FINAL_API_SCHEMA_OR_SAMPLE>

TASK 1: Generate `request-mapper.jslt`
Map the provided Input Samples to the Final API Request Body. 
- Handle missing fields gracefully using JSLT fallback operators (`| default`).

TASK 2: Generate `response-mapper.jslt`
Map the response of the Final API to the provided Final Output Samples structure.

OUTPUT FORMAT:
Return a strict JSON object with no markdown explanations outside of the object:
{
  "request_mapper_jslt": "<script string>",
  "response_mapper_jslt": "<script string>"
}
```

### Agent 2: The Enterprise Routing Agent
**Role:** Translates natural language rules into executable JSONLogic/Simple predicates and wires the Camel Saga YAML.

**Prompt:**
```markdown
You are a strict Enterprise Integration Architect. Your task is to generate an Apache Camel 4.x route using the YAML DSL. You must implement a chained Content Enricher pattern.

CONFIGURATION:
- Pipeline ID: <INJECT_PIPELINE_ID>
- Input Format: <INJECT_INPUT_FORMAT>
- Output Format: <INJECT_OUTPUT_FORMAT>
- Validation API: <INJECT_VALIDATION_API>
- Final API: <INJECT_FINAL_API>
- Overlay Rules: <INJECT_OVERLAY_RULES>

ABSOLUTE ARCHITECTURAL CONSTRAINTS (DO NOT DEVIATE):
1. Error Handling: The route MUST start with an `errorHandler: deadLetterChannel` pointing to `kafka:dlq-<INJECT_PIPELINE_ID>`. Configure `maximumRedeliveries: 3` and `redeliveryDelay: 2000` with exponential backoff.
2. Tracing: You MUST inject an MDC (Mapped Diagnostic Context) logging property named `correlationId` using the Kafka message key immediately upon consumption.
3. Ingestion: If Input Format is XML, the first step is `unmarshal: jacksonXml`.
4. Dynamic Validation (API 1): Use the Camel HTTP component to call the Validation API. 
5. Rule Evaluation: Translate the Overlay Rules into a Camel `choice` block using JSONPath or Simple language. Invalid data must route to `kafka:rejected-<INJECT_PIPELINE_ID>`.
6. Request Transformation: Route valid payloads through `transform: jslt:classpath:mappings/<INJECT_PIPELINE_ID>-request.jslt`.
7. Final Transaction (API 2): Use the Camel HTTP component to call the Final API.
8. Response Transformation: Route the API 2 response through `transform: jslt:classpath:mappings/<INJECT_PIPELINE_ID>-response.jslt`.
9. Egress: If Output Format is XML, your last step before the output Kafka topic MUST be `marshal: jacksonXml`.

Output ONLY the valid Camel YAML configuration. Do not include markdown explanations.
```

### Agent 3: The Resiliency & QA Enforcer
**Role:** Acts as an automated Site Reliability Engineer to prevent the AI from committing brittle integrations.

**Prompt:**
```markdown
You are a ruthless Site Reliability Engineer (SRE). You are reviewing an Apache Camel YAML route and two JSLT scripts generated for Pipeline ID: <INJECT_PIPELINE_ID>.

ARTIFACTS:
- Route YAML: <INJECT_YAML_OUTPUT>
- Request JSLT: <INJECT_REQUEST_JSLT>
- Response JSLT: <INJECT_RESPONSE_JSLT>

SRE CHECKLIST (FAIL THE BUILD IF ANY ARE MISSING):
1. Exponential Backoff: Does the YAML contain a `deadLetterChannel` with `maximumRedeliveries` and `useExponentialBackOff: true`?
2. Observability: Is `correlationId` explicitly set in MDC or MDC logging enabled for the route?
3. Circuit Breaking/Timeouts: Do BOTH the Validation API and Final API HTTP calls include timeout configurations (e.g., `httpClient.socketTimeout`)?
4. JSLT Syntax: Are both JSLT files structurally sound without obvious syntax errors?
5. Formatting: Are the `unmarshal` and `marshal` steps present and correct based on the input/output formats?

If ANY check fails, return a JSON object: 
{"status": "FAIL", "reason": "Detailed explanation of what the Routing or Contract agent missed."}

If all checks pass perfectly, return:
{"status": "PASS", "reason": "Architecture compliant."}
```

### Agent 4: The GitOps Agent
**Role:** Packages the validated configurations and interfaces with the VCS provider to raise the Pull Request.

**Prompt:**
```markdown
You are a Lead DevOps Engineer. The integration pipeline has passed all QA checks.
Your task is to draft the Git commit message and the Pull Request description for this new configuration.

PIPELINE ID: <INJECT_PIPELINE_ID>
FILES TO COMMIT: 
- `src/main/resources/camel/<INJECT_PIPELINE_ID>.yaml`
- `src/main/resources/mappings/<INJECT_PIPELINE_ID>-request.jslt`
- `src/main/resources/mappings/<INJECT_PIPELINE_ID>-response.jslt`

REQUIREMENTS:
1. Generate a conventional commit message (e.g., `feat(routing): add <pipeline_id> enricher flow`).
2. Generate a Pull Request description in Markdown. The PR must include:
   - A brief summary of the pipeline's purpose.
   - The input and output topics.
   - The API endpoints being integrated.
   - The validation rules being applied.
   - A checklist for the human reviewer (e.g., "Verify API timeouts in higher environments").

Format your response strictly as a JSON object:
{
  "commit_message": "...",
  "pr_title": "...",
  "pr_body": "..."
}
```