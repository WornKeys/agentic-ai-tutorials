# Multi-Agent Spring Boot Builder Prompts

**Context:** This document contains a sequence of prompts designed to instruct an LLM to build an "Agentic Builder" application. This Builder uses a State Machine to orchestrate agents that generate, test, and commit code for a separate "Target Application" (a dynamic PAS-to-JSON parser).

---

## Prompt 1: The Factory (Agentic Builder App) & State Machine

**Role:** You are an expert Software Architect specializing in Multi-Agent Systems and State Machine design.

**Task:** Generate the core Spring Boot architecture for an "Agentic Builder" application. This application's sole purpose is to manage a workflow that generates and tests *another* distinct Spring Boot application.

**Requirements:**
1. **State Machine:** Integrate a State Machine (e.g., Spring State Machine or a robust custom FSM). Define the following states: `INIT`, `GENERATING_TESTS`, `GENERATING_CODE`, `EXECUTING_TESTS`, `EVALUATING_FAILURES`, `COMMITTING_CODE`, `COMPLETED`, `FAILED`.
2. **State Transitions & Events:** Define the events that trigger transitions between these states (e.g., `TESTS_FAILED` transitions from `EXECUTING_TESTS` to `EVALUATING_FAILURES`).
3. **Agent Interface:** Define a generic `StatefulAgent` interface. Agents will be triggered by specific state transitions. 
4. **Output:** Provide the State Machine configuration class, the Enums for States and Events, and the base `StatefulAgent` interface. Do not write the specific agents yet.

---

## Prompt 2: The Product Skeleton & TDD Agent

**Context:** Building upon the "Agentic Builder" State Machine.

**Task:** Implement the `TddGenerationAgent`, which triggers during the `GENERATING_TESTS` state.

**Requirements:**
1. This agent must target a separate, external directory representing the "Target Application" (the PAS Parser).
2. **Input Data Format:** The Target Application will process incoming PAS messages formatted as JSON arrays of index-value pairs (e.g., `[{"index": 1, "value": "John"}, {"index": 2, "value": 35}]`). The output must be a standard mapped JSON object.
3. **Agent Logic:** The agent must generate JUnit 5 test cases in the Target Application's `src/test/java` directory. These tests should assert that specific PAS JSON inputs produce specific mapped JSON outputs.
4. **Output:** Provide the Java code for `TddGenerationAgent`. Show how it reads a sample PAS JSON input and a sample Expected JSON output from the Builder's resources, constructs the JUnit class as a string, and writes it to the Target Application's file system. Transition the state machine to `GENERATING_CODE` upon success.

---

## Prompt 3: The Code Generation Agent (Strictly Dynamic Mapping)

**Context:** The State Machine is in the `GENERATING_CODE` state. Tests exist in the Target Application.

**Task:** Implement the `CodeGenerationAgent`.

**Requirements:**
1. This agent generates the `PasToJsonService` in the Target Application's `src/main/java`.
2. **CRITICAL ARCHITECTURAL CONSTRAINT:** The generated `PasToJsonService` **MUST NOT** contain any hardcoded indices or switch statements for mapping the PAS data. 
3. **Dynamic Engine:** The agent must generate a parser that relies on an external mapping configuration (e.g., a `pas-mapping.json` file in `src/main/resources`). The mapping file should define how `index: 1` maps to the JSON key `"name"`, and `index: 2` maps to `"age"`. 
4. **Output:** Provide the Java code for the `CodeGenerationAgent`. Show the payload it sends to the LLM (you can stub the actual LLM API call), explicitly instructing the LLM to write a generic, configuration-driven mapping engine. Write the resulting file to the Target Application. Transition to `EXECUTING_TESTS`.

---

## Prompt 4: Test Execution & The Enhancement Loop

**Context:** The State Machine is in the `EXECUTING_TESTS` state.

**Task:** Implement the `TestExecutionAgent` and the `EnhancementAgent`.

**Requirements:**
1. **Execution:** The `TestExecutionAgent` must programmatically run the Gradle test task in the Target Application's directory (using the Gradle Tooling API or a robust Process shell execution).
2. **Conditional Routing:** - If tests pass, fire the `TESTS_PASSED` event to transition to `COMMITTING_CODE`.
   - If tests fail, fire the `TESTS_FAILED` event to transition to `EVALUATING_FAILURES`.
3. **Enhancement:** Implement the `EnhancementAgent` that listens for the `EVALUATING_FAILURES` state. It must read the test failure logs, package them with the generated source code, and prepare a prompt for the LLM to fix the mapping logic or configuration. Once fixed, transition back to `EXECUTING_TESTS`.
4. **Output:** Provide the code for both agents and show how they interact with the State Machine's context to pass error logs around.

---

## Prompt 5: The Git Operations Agent

**Context:** The State Machine is in the `COMMITTING_CODE` state.

**Task:** Implement the `GitCommitAgent`.

**Requirements:**
1. Use the Eclipse JGit library.
2. Target the local Git repository of the Target Application.
3. Stage all new or modified files in `src/main` and `src/test`.
4. Generate an automated commit message indicating that the agent successfully implemented the dynamic PAS mapping and passed all TDD constraints.
5. Commit and push to the remote branch. Fire the `WORKFLOW_COMPLETE` event to transition to the `COMPLETED` state.
6. **Output:** Provide the Java code for `GitCommitAgent`, handling JGit exceptions cleanly.