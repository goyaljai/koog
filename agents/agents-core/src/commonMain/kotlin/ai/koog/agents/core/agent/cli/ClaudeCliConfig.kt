package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.cli.JsonUtils.boolVal
import ai.koog.agents.core.agent.cli.JsonUtils.doubleVal
import ai.koog.agents.core.agent.cli.JsonUtils.intVal
import ai.koog.agents.core.agent.cli.JsonUtils.stringVal
import ai.koog.agents.core.agent.cli.JsonUtils.toJsonStdoutEvents
import ai.koog.cli.transport.CliEvent
import ai.koog.cli.transport.CliException
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.Structure
import ai.koog.prompt.structure.json.generator.JsonSchemaConsts
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration

/**
 * Claude Code permission mode.
 */
public enum class ClaudePermissionMode(public val value: String) {
    /**
     * Automatically accept all edits.
     */
    AcceptEdits("acceptEdits"),

    /**
     * Bypass all permission checks.
     */
    BypassPermissions("bypassPermissions"),

    /**
     * Default permission mode.
     */
    Default("default"),

    /**
     * Delegate permissions to the parent agent.
     */
    Delegate("delegate"),

    /**
     * Do not ask for permissions.
     */
    DontAsk("dontAsk"),

    /**
     * Plan mode: only show planned actions without executing them.
     */
    Plan("plan")
}

/**
 * Helper functions for Claude CLI agent
 */
public object ClaudeCliHelper {

    /**
     * Generates flags for Claude cli in autonomous mode.
     */
    public fun flags(
        model: LLModel,
        systemMessages: List<Message.System>,
        permissionMode: ClaudePermissionMode?,
        additionalFlags: List<String>
    ): List<String> =
        buildList {
            add("-p")
            add("--output-format")
            add("stream-json")
            add("--verbose")

            if (model.provider == LLMProvider.Anthropic) {
                add("--model")
                add(model.id)
            }

            systemMessages.forEach { systemMessage ->
                add("--append-system-prompt")
                add(systemMessage.content)
            }

            permissionMode?.let {
                add("--permission-mode")
                add(it.value)
            }

            addAll(additionalFlags)
        }

    /**
     * Generates flags for Claude cli in structured mode.
     */
    public fun structuredFlags(
        model: LLModel,
        systemMessages: List<Message.System>,
        permissionMode: ClaudePermissionMode?,
        additionalFlags: List<String>,
        structure: Structure<*, LLMParams.Schema.JSON>
    ): List<String> =
        flags(model, systemMessages, permissionMode, additionalFlags) + listOf(
            "--json-schema",
            extractClaudeSchema(structure.schema)
        )

    /**
     * Generates environment with provided API key.
     */
    public fun env(apiKey: String?): Map<String, String> = buildMap {
        apiKey?.let { put("ANTHROPIC_API_KEY", it) }
    }

    /**
     * Extracts output from Claude CLI events.
     */
    public fun extractOutput(events: List<CliEvent>): CliAIAgentResponse {
        val failedEvent = events.filterIsInstance<CliEvent.Failed>().firstOrNull()
        if (failedEvent != null) {
            return CliAIAgentResponse(
                content = "Cli failed: ${failedEvent.message}",
                isError = true,
                metaInfo = CliAgentResponseMetaInfo()
            )
        }

        val jsonEvents = toJsonStdoutEvents(events)

        val resultEvent = jsonEvents
            .lastOrNull { it["type"]?.stringVal == "result" }
            ?: throw CliException("No result event found")

        val content = resultEvent["result"]
            ?.stringVal
            ?: throw CliException("No result found in result event")

        val isError = resultEvent["is_error"]?.boolVal ?: false

        val usageObject = resultEvent["usage"]?.jsonObject

        val metaInfo = CliAgentResponseMetaInfo(
            inputTokensCount = usageObject?.get("input_tokens")?.intVal,
            outputTokensCount = usageObject?.get("output_tokens")?.intVal,
            buildJsonObject {
                put("cacheCreationInputTokens", usageObject?.get("cache_creation_input_tokens")?.intVal)
                put("cacheReadInputTokens", usageObject?.get("cache_read_input_tokens")?.intVal)
                put("totalCostUsd", resultEvent["total_cost_usd"]?.doubleVal)
            }
        )

        return CliAIAgentResponse(
            content = content,
            isError = isError,
            metaInfo = metaInfo
        )
    }

    /**
     * Extracts structured output from Claude CLI events.
     */
    public fun <T> extractStructuredOutput(
        events: List<CliEvent>,
        structure: Structure<T, *>,
    ): CliAgentStructuredResponse<T> {
        val failedEvent = events.filterIsInstance<CliEvent.Failed>().firstOrNull()
        if (failedEvent != null) {
            return CliAgentStructuredResponse(
                result = null,
                response = CliAIAgentResponse(
                    content = "Cli failed: ${failedEvent.message}",
                    isError = true,
                    metaInfo = CliAgentResponseMetaInfo()
                )
            )
        }

        val response = extractOutput(events)
        val jsonEvents = toJsonStdoutEvents(events)
        val resultString = jsonEvents
            .lastOrNull { it["type"]?.stringVal == "result" }
            ?.get("structured_output")
            ?.toString()
            ?: throw CliException("No structured output found")
        val result = structure.parse(resultString)

        return CliAgentStructuredResponse(
            result = result,
            response = response.copy(content = resultString)
        )
    }

    /**
     * Extracts a JSON schema for Claude Code CLI from the provided [schema].
     */
    private fun extractClaudeSchema(schema: LLMParams.Schema.JSON): String {
        val jsonSchema = schema.schema

        val defs = requireNotNull(jsonSchema[JsonSchemaConsts.Keys.DEFS]) { "DEFS is required in the JSON schema." }

        val rootType = jsonSchema[JsonSchemaConsts.Keys.REF]
            ?.stringVal
            ?.removePrefix(JsonSchemaConsts.Keys.REF_PREFIX)
            ?.let { defs.jsonObject[it] }

        require(rootType is JsonObject) { "Claude Code CLI requires a JSON object as the root type." }

        val updatedSchema = rootType.toMutableMap()
        updatedSchema[JsonSchemaConsts.Keys.DEFS] = defs

        return JsonObject(updatedSchema).toString()
    }
}

/**
 * Configuration for Claude CLI agent with structured output.
 *
 * @param Input The type of input the agent accepts.
 * @param Output The type of structured output the agent produces.
 * @property transport The CLI transport used to execute commands.
 * @property apiKey The Anthropic API key, or null to use the ANTHROPIC_API_KEY environment variable.
 * @property structure The structure definition for parsing the output.
 * @property permissionMode The permission mode for Claude CLI execution.
 * @property additionalFlags Additional command-line flags to pass to Claude CLI.
 * @property workspace The working directory for command execution.
 * @property timeout The execution timeout duration.
 */
public class ClaudeCliStructuredConfig<Input, Output>(
    override val transport: CliTransport,
    public val apiKey: String? = null,
    public val structure: Structure<Output, LLMParams.Schema.JSON>,
    public val permissionMode: ClaudePermissionMode? = null,
    public val additionalFlags: List<String> = emptyList(),
    override val workspace: String = ".",
    override val timeout: Duration? = null,
    private val generateRequest: CliConfig.GenerateRequest<Input>
) : CliConfig<Input, CliAgentStructuredResponse<Output>> {
    override val binaryPath: String = "claude"
    override val env: Map<String, String> = ClaudeCliHelper.env(apiKey)

    override fun flags(model: LLModel, systemMessages: List<Message.System>): List<String> =
        ClaudeCliHelper.structuredFlags(model, systemMessages, permissionMode, additionalFlags, structure)

    override fun generateRequest(input: Input): String =
        generateRequest.generateRequest(input)

    override fun extractOutput(events: List<CliEvent>): CliAgentStructuredResponse<Output> =
        ClaudeCliHelper.extractStructuredOutput(events, structure)
}

/**
 * Configuration for Claude CLI agent.
 *
 * @param Input The type of input the agent accepts.
 * @property transport The CLI transport used to execute commands.
 * @property apiKey The Anthropic API key, or null to use the ANTHROPIC_API_KEY environment variable.
 * @property permissionMode The permission mode for Claude CLI execution.
 * @property additionalFlags Additional command-line flags to pass to Claude CLI.
 * @property workspace The working directory for command execution.
 * @property timeout The execution timeout duration.
 */
public class ClaudeCliConfig<Input>(
    override val transport: CliTransport,
    public val apiKey: String? = null,
    public val permissionMode: ClaudePermissionMode? = null,
    public val additionalFlags: List<String> = emptyList(),
    override val workspace: String = ".",
    override val timeout: Duration? = null,
    private val generateRequest: CliConfig.GenerateRequest<Input>,
) : CliConfig<Input, CliAIAgentResponse> {
    override val binaryPath: String = "claude"
    override val env: Map<String, String> = ClaudeCliHelper.env(apiKey)

    override fun flags(model: LLModel, systemMessages: List<Message.System>): List<String> =
        ClaudeCliHelper.flags(model, systemMessages, permissionMode, additionalFlags)

    override fun generateRequest(input: Input): String =
        generateRequest.generateRequest(input)

    override fun extractOutput(events: List<CliEvent>): CliAIAgentResponse =
        ClaudeCliHelper.extractOutput(events)
}
