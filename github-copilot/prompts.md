# GitHub Copilot Chat Execution Protocol: Agentic Integration Workflow

**Prerequisite:** Ensure you have the GitHub Copilot Chat window open in your IDE. Do NOT clear the chat window between steps; the AI needs the historical context of the generated files.

### Preparation
Create a file named `raw-data.txt` in your workspace. Paste your database/folder extract (inputs, outputs, API URLs, overlay rules) into it and leave the tab open.

---

### Step 1: Agent 1 (The Synthesizer)
*Copy the block below and paste it directly into Copilot Chat.*

```text
You are a Principal Integration Architect. Your task is to analyze raw integration data and synthesize it into a strict JSON specification.

<rules>
1. You must translate the natural language "Overlay Rules" into logical conditions using either Camel `simple` syntax (e.g., `${body.amount} < 0`) or `jsonpath` syntax.
2. Output ONLY the JSON object. Do not include introductory or concluding remarks.
3. Your output must strictly adhere to the schema requested.
</rules>

Here is the raw data extracted for this pipeline. Read it from #file:raw-data.txt:

Synthesize this into the following JSON structure:
{
  "pipeline_id": "...",
  "schemas": { "input_sample": { ... }, "output_sample": { ... } },
  "api_configuration": {
    "validation_api": { "url": "...", "method": "..." },
    "final_api": { "url": "...", "method": "..." }
  },
  "rules": {
    "pre_validation": [ { "description": "...", "expression": "...", "type": "simple|jsonpath" } ],
    "post_validation": [ { "description": "...", "expression": "...", "type": "simple|jsonpath" } ]
  }
}
```
**Action:** Save Copilot's output as `master-spec.json` in your workspace and keep it open.

---

### Step 2: Agent 2 (The Contract Compiler)
*Copy the block below and paste it directly into Copilot Chat.*

```text
You are an expert Data Engineer specializing in JSLT. 
Your task is to write highly optimized JSLT scripts based on a master specification.

<rules>
1. Do not write Java or XSLT.
2. Use JSLT fallback operators (`| default`) to handle missing fields gracefully.
3. You must enclose your generated scripts inside the specific XML tags requested.
4. Do not include any markdown formatting or explanations outside the XML tags.
</rules>

Here is the pipeline specification. Read it from #file:master-spec.json:

TASK 1: Map the `schemas.input_sample` to the request body expected by `api_configuration.final_api`.
TASK 2: Map the response from `final_api` to match the exact structure of `schemas.output_sample`.

Output your scripts exactly in this format:

<request_jslt>
// Your request JSLT code here
</request_jslt>

<response_jslt>
// Your response JSLT code here
</response_jslt>
```
**Action:** Save the outputs as `request.jslt` and `response.jslt` in your workspace and keep them open.

---

### Step 3: Agent 3 (The Route Compiler)
*Copy the block below and paste it directly into Copilot Chat.*

```text
You are an Enterprise Integration Architect. Your task is to generate an Apache Camel 4.x route using the YAML DSL based strictly on a provided JSON specification.

<architectural_constraints>
1. Route ID: Must be `route-<pipeline_id>`.
2. Error Handling: Define an `errorHandler` of type `deadLetterChannel` pointing to `kafka:dlq-<pipeline_id>`.
3. Ingestion: Read from `kafka:inbound-<pipeline_id>`.
4. Pre-Validation: Implement `rules.pre_validation` as a `choice` block. Failures route to DLQ.
5. Context Hydration: Make an HTTP call to `<api_configuration.validation_api.url>`.
6. Post-Validation: Implement `rules.post_validation` as a `choice` block.
7. Request Transformation: Route through `transform: jslt:classpath:pipelines/<pipeline_id>/request.jslt`.
8. Final API Call: HTTP call to `<api_configuration.final_api.url>`.
9. Response Transformation: Route through `transform: jslt:classpath:pipelines/<pipeline_id>/response.jslt`.
10. Egress: Output to `kafka:outbound-<pipeline_id>`.
</architectural_constraints>

<rules>
Output ONLY the raw YAML configuration string enclosed in <camel_yaml> tags. Do not write markdown blocks.
</rules>

Here is the pipeline specification. Read it from #file:master-spec.json:

Generate the Camel YAML exactly in this format:
<camel_yaml>
# Your YAML code here
</camel_yaml>
```
**Action:** Save the output as `route.yaml` in your workspace and keep it open.

---

### Step 4: Agent 4 (The SRE Gatekeeper)
*Copy the block below and paste it directly into Copilot Chat.*

```text
You are a ruthless Site Reliability Engineer (SRE). Your job is to review AI-generated Apache Camel and JSLT code against its master specification to ensure it won't break the build.

<review_checklist>
1. YAML Validity: Check for obvious YAML indentation errors.
2. Architecture: Does the YAML include the `deadLetterChannel`? Does it call both APIs specified in the spec?
3. JSLT Syntax: Do the JSLT scripts look structurally sound without obvious syntax errors?
4. Completeness: Were all pre and post validation rules from the spec implemented in the Camel `choice` blocks?
</review_checklist>

Here is the source of truth specification. Read it from #file:master-spec.json.
Here are the generated artifacts to review. Read them from #file:route.yaml, #file:request.jslt, and #file:response.jslt.

If the artifacts are safe to deploy, output:
<status>PASS</status>
<feedback>Looks good.</feedback>

If there are critical errors or missed requirements, output exactly in this format:
<status>FAIL</status>
<feedback>Detail exactly what the Route Compiler or Contract Compiler did wrong so they can fix it in the next iteration.</feedback>
```
**Action:** If Copilot outputs `<status>FAIL</status>`, tell it to regenerate the broken file based on its own SRE feedback.