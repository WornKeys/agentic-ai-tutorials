Prompt 1: Project Scaffolding & Infrastructure

@workspace Create the project scaffolding for a Python-based Model Context Protocol (MCP) server that acts as a configuration generator for a target Java application. 

Strict Constraints:
- Use `uv` for dependency management. Generate the `pyproject.toml`.
- Dependencies required: `fastapi`, `uvicorn`, `langgraph`, `langchain-core`, `langchain-openai` (or equivalent), `pydantic`, `mcp`.
- Directory structure must follow modern Python backend practices (e.g., `src/agentic_generator/`, `api/`, `core/`, `graph/`).
- Provide the FastAPI `main.py` that sets up a standard REST endpoint (`/generate`) AND exposes the application as an MCP server using SSE (Server-Sent Events) transport.
- The application MUST NOT generate any Java code. Its sole purpose is to output YAML and JSON configurations.


Prompt 2: State Definition & Schema

@workspace Now, define the LangGraph State and the data models in the `src/agentic_generator/core/` directory.

Requirements:
1. Create a `WorkflowState` using `TypedDict`. It must include:
   - `request_body`: dict (containing English rules, input event schema, mutable API OpenAPI specs)
   - `extracted_apis`: list (APIs to call for data enrichment)
   - `generated_jolt_spec`: dict (The JOLT mapping rules)
   - `generated_json_logic`: dict (The overlay rules)
   - `generated_camel_yaml`: str (The final Apache Camel route)
   - `errors`: list

2. Create strict Pydantic models for the LLM structured outputs:
   - `JoltSpecModel`: To enforce the Bazaarvoice JOLT community edition array structure.
   - `JsonLogicModel`: To enforce valid JsonLogic AST syntax.
   - `CamelRouteModel`: To enforce the Apache Camel YAML DSL structure.


Prompt 3: Agent Nodes & OpenAPI Tool Calling

@workspace Build the LangGraph agent nodes in `src/agentic_generator/graph/nodes.py`. 

Requirements:
- We need 3 distinct node functions: `analyze_and_extract_apis`, `generate_transformations_and_rules`, and `assemble_camel_route`.
- In `analyze_and_extract_apis`: The LLM must read the OpenAPI specs provided in the state's `request_body` and figure out the exact HTTP methods, paths, and required parameters for the overlay data fetching and the final mutable API call. Use LangChain's structured output.
- In `generate_transformations_and_rules`: The LLM generates the JOLT mapping and the JsonLogic rules based on the English text in the state. Force the LLM to output using the Pydantic models created earlier.
- In `assemble_camel_route`: The LLM takes the APIs, the JOLT spec, and the JsonLogic, and generates the final declarative Camel YAML route.
- CRITICAL: Add a strict system prompt to every LLM call stating: "You are a configuration generator. You must never generate Java code. You only generate JSON and YAML."


Prompt 4: Graph Assembly & API Execution

@workspace Now assemble the graph in `src/agentic_generator/graph/orchestrator.py` and connect it to the API in `main.py`.

Requirements:
1. In `orchestrator.py`: Build the `StateGraph` using the `WorkflowState`. Add the nodes, set the entry point, define the linear edges (analyze -> generate -> assemble), and compile it.
2. In `main.py`: Update the `/generate` POST endpoint to accept a JSON payload containing the schemas and English rules. Pass this payload into the compiled LangGraph and return the final YAML and JSON strings in the HTTP response.
3. Register an MCP Tool named `generate_enterprise_integration` that wraps this exact same graph execution, allowing an external MCP Client to trigger this generation process dynamically.