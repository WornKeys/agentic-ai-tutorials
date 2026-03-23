# Python MCP Server & LangGraph Builder Prompts

**Context:** This document contains a sequence of prompts designed to instruct an LLM to build a Python-based Model Context Protocol (MCP) server. This server wraps a LangGraph agentic workflow that autonomously generates, tests, and commits code for a target Spring Boot application.

---

## Prompt 1: The MCP Server Skeleton & Tool Exposure

**Role:** You are an elite Python Software Architect specializing in the Model Context Protocol (MCP) and agentic workflows.

**Task:** Scaffold a standard Python MCP server using the official `mcp` SDK.

**Requirements:**
1. Initialize a Python project using `uv` or `pip`, including dependencies for `mcp`, `langgraph`, `langchain-openai`, and `gitpython`.
2. Create the core `server.py` file. Set up an MCP FastMCP or standard MCP server instance.
3. Expose a single MCP tool named `generate_and_test_spring_boot_feature`. 
4. The tool should accept parameters: `target_directory` (path to the Spring Boot app) and `pas_message_format` (description of the incoming PAS JSON structure).
5. Inside this tool's execution block, leave a placeholder for invoking a LangGraph workflow. The tool must return the final outcome (Success/Failure and build logs) to the MCP client.
6. **Output:** Provide the `requirements.txt` (or `pyproject.toml`) and the `server.py` code. Do not implement the LangGraph logic yet.

---

## Prompt 2: LangGraph State and The "Dynamic Mapping" Enforcer

**Context:** Building the internal LangGraph engine for the `generate_and_test_spring_boot_feature` MCP tool.

**Task:** Define the LangGraph `State` and implement the LLM-driven code generation nodes.

**Requirements:**
1. Define a `TypedDict` for the state containing: `messages`, `target_dir`, `mapping_config_path`, `tests_passed` (bool), `iteration_count` (int), and `build_logs`.
2. Implement the `design_mapping_node`: This node uses an LLM to generate a `pas-mapping.json` file. This file dictates how input PAS indices map to output JSON keys. Write this file to the Spring Boot app's `src/main/resources`.
3. Implement the `generate_code_node`: This node uses an LLM to write the `PasToJsonService.java`. 
4. **CRITICAL ARCHITECTURAL RULE:** Inject a strict system prompt into the `generate_code_node` ensuring the generated Java code **reads from `pas-mapping.json` dynamically using Jackson/Gson**. It must not contain a single hardcoded index, `switch` statement, or `if/else` chain for the mapping logic. If the LLM generates hardcoded logic, it fails the requirement.
5. Implement the `generate_tests_node`: Uses an LLM to write JUnit 5 tests asserting the dynamic logic works based on sample JSON data.
6. **Output:** Provide the Python code for the state definition and these three specific node functions.

---

## Prompt 3: The TDD Execution Loop & Agentic Reasoning

**Context:** Continuing the LangGraph workflow implementation. Tests and code have been generated.

**Task:** Implement the execution, reasoning, and routing logic for the TDD loop.

**Requirements:**
1. Implement `execute_gradle_node`: A Python function that uses `subprocess.run` to execute `./gradlew test` in the `target_dir`. It must capture `stdout` and `stderr`. Update the state: if exit code is 0, `tests_passed = True`; otherwise `False`, appending the error logs. Increment `iteration_count`.
2. Implement the `reasoning_node`: If tests fail, this node takes the `build_logs` and the current Java code, passes them to an LLM, and asks it to analyze the failure and output a correction plan before routing back to `generate_code_node`.
3. **The Graph Compilation:** Wire the graph together. Define a conditional edge after `execute_gradle_node`. 
   - If `tests_passed == True`, route to `commit_git_node` (stub this for now).
   - If `tests_passed == False` AND `iteration_count < 4`, route to `reasoning_node` -> `generate_code_node`.
   - If `tests_passed == False` AND `iteration_count >= 4`, route to `END` (fail fast to prevent infinite loops).
4. **Output:** Provide the Python code for the `execute_gradle_node`, the conditional routing function, and the `StateGraph` compilation block.

---

## Prompt 4: Git Operations and MCP Tool Integration

**Context:** Finalizing the MCP server and LangGraph integration.

**Task:** Implement the Git node and wire the compiled graph into the MCP tool.

**Requirements:**
1. Implement `commit_git_node`: Use `GitPython` to stage all changes in the Spring Boot `target_dir`, commit with a message like "Agentic TDD: PAS Dynamic Mapping", and push to the current branch.
2. Go back to the `server.py` from Prompt 1. Inside the `generate_and_test_spring_boot_feature` MCP tool, instantiate the compiled LangGraph workflow.
3. Pass the tool's input parameters into the LangGraph initial state.
4. `await` or `invoke` the graph execution. 
5. Extract the final state (whether it succeeded or hit the iteration limit) and format it as a clean Markdown string to return to the MCP Client.
6. **Output:** Provide the completed, integrated `server.py` file showing exactly how the MCP tool executes the LangGraph workflow and handles the final response.

---

## Prompt 5: State Persistence & Human-in-the-Loop (HITL)

**Context:** The MCP Server and LangGraph workflow are fully structured. We now need to add state persistence, debugging capabilities, and a safety pause.

**Task:** Integrate LangGraph Checkpointing and a Human-in-the-Loop breakpoint.

**Requirements:**
1. **Checkpointer:** Import `MemorySaver` (for in-memory debugging) or `SqliteSaver` (for persistent local debugging) from LangGraph. 
2. **Graph Compilation:** Update the `workflow.compile()` step to include the checkpointer. 
3. **The Breakpoint:** Add an interrupt before the Git commit step: `compile(checkpointer=memory, interrupt_before=["commit_git"])`.
4. **Thread Management:** Modify the main MCP tool (`generate_and_test_spring_boot_feature`) to accept an optional `thread_id` parameter. When the graph is invoked, pass this `thread_id` in the `config` dictionary. 
5. **Handling the Interrupt:** When the graph hits the `commit_git` breakpoint, it will yield control back to the MCP server. The tool must return a message to the MCP client stating: *"Workflow paused for human review. Tests passed. Review the code in the target directory. To approve and commit, call the `approve_and_commit` tool with thread_id: [ID]."*
6. **The Approval Tool:** Create a second, smaller MCP tool named `approve_and_commit`. This tool accepts a `thread_id`, fetches the existing graph state, and calls `graph.invoke(None, config={"configurable": {"thread_id": thread_id}})` to resume execution and push the code.
7. **Output:** Provide the updated `server.py` showing the checkpointer integration, the modified primary tool, and the new approval tool.