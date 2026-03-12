# AGENTS.md

## Scope
- This repository is a Gradle composite build centered on `codeprompt`, with local dependency substitution for `agent-sdk`, `agents-common`, and `acp-langraph-langchain-bridge` via `settings.gradle.kts`; start with `settings.gradle.kts`, root `build.gradle.kts`, and `codeprompt/build.gradle.kts` before changing module wiring or dependency versions.

## Repository map
- `codeprompt/`: Spring Boot app and the main deliverable; implements the coding assistant graph.
- `acp-langraph-langchain-bridge/`: ACP stdio transport + LangGraph/LangChain bridge. Read its README for the ACP/Kotlin ↔ Java split.
- `agent-sdk/`: shared Markdown parsing, remote ACP client, MCP aliasing, and LangGraph helpers.
- `agents-common/`: reusable agents/skills intended to be consumed by Spring Boot agent apps.

## Big-picture runtime flow
- ACP traffic is stdio JSON-RPC; stdout must stay protocol-clean, so logs are configured to stderr (`codeprompt/src/main/resources/application.yaml`, bridge README).
- `codeprompt/src/main/java/net/osgiliath/codeprompt/langgraph/graph/CodingPromptGraph.java` defines the graph: `START -> filter -> unwrapper -> agent`, then `LLMToToolEdge` loops back to `agent` on tool calls or exits on `END`.
- `AttachmentFilterNode` asks an AI service to choose relevant attachments from ACP metadata before `AttachmentUnwrapperNode` loads binary content.
- `LLMProcessorNode` converts the latest `UserMessage` plus attachment contents into LangChain4j `Content`, then bridges `TokenStream` into LangGraph4j streaming output.
- Prompt behavior is defined mostly by `@AiService` interfaces (`JavaSpringBootAssistant`, `AttachmentFiltererFromMetadata`) rather than handwritten HTTP/model calls.

## Build and test workflow
- Use the root wrapper; all modules target Java 21 and inherit JUnit 5 + JaCoCo from root Gradle config.
- Common commands:
  - `./gradlew clean build --stacktrace`
  - `./gradlew :codeprompt:test --stacktrace`
  - `./gradlew :codeprompt:bootRun`
  - `./gradlew sonar -Dsonar.qualitygate.wait=true --stacktrace` (requires `SONAR_*` env vars)
- Dependency verification is used in module builds; when dependencies change, refresh verification metadata in lenient mode before retrying strict mode (see module READMEs).

## Test environment conventions
- Default local tests expect LM Studio on `http://127.0.0.1:1234/v1`; see `codeprompt/src/test/resources/application.yaml`.
- CI/local `github` profile uses GitHub Models and `MODEL_TOKEN`; see `codeprompt/src/test/resources/application-github.yaml` and `ProfileConfigurationIT`.
- `testcontainers` profile switches to Ollama config in `application-testcontainers.yaml`.
- Some end-to-end tests are intentionally external-system dependent: `RemoteAgentCallerCagentE2EIT` expects a `cagent`/Docker-backed ACP command, and Cucumber features write reports under `build/reports/cucumber/`.
- Attachment tests use fixtures under `codeprompt/src/test/resources/dataset/`; `Thread.java` contains the easter-egg assertion used by integration tests.

## Project-specific coding patterns
- Prefer extending the LangGraph pipeline by adding Spring `@Component` nodes/edges and wiring them in `CodingPromptGraph`, not by embedding orchestration logic in controllers or runners.
- Preserve bean names/qualifiers used by LangChain4j auto-wiring (`primaryChatModel`, `primaryStreamingChatModel`) when touching model configuration.
- Keep ACP session context intact: session id, cwd, and `mcpServers` are part of the cross-module contract documented in `agent-sdk/README.md`.
- When changing tool naming, check Spring MCP alias config semantics from `agent-sdk/README.md`; alias order matters because the first entry is the primary mapped tool, and remember `codeprompt` is published as a library (`bootJar` disabled, plain `jar` enabled) rather than as a fat jar by default.

## Files worth reading first for most tasks
- `settings.gradle.kts`
- `build.gradle.kts`
- `codeprompt/build.gradle.kts`
- `codeprompt/src/main/java/net/osgiliath/codeprompt/langgraph/graph/CodingPromptGraph.java`
- `codeprompt/src/main/java/net/osgiliath/codeprompt/langgraph/node/LLMProcessorNode.java`
- `agent-sdk/README.md`
- `acp-langraph-langchain-bridge/README.md`
