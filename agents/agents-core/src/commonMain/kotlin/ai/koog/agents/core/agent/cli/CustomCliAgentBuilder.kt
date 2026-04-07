package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.cli.transport.CliEvent
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Builder for custom CLI agent.
 */
public class CustomCliAgentBuilder<Input, Output> internal constructor(
    config: AIAgentConfig,
    transport: CliTransport?,
    workspace: String,
    timeout: Duration?,
    id: String?,
    clock: Clock,
    featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit>
) : CliAIAgentBuilderBase<CustomCliAgentBuilder<Input, Output>>(
    config,
    transport,
    workspace,
    timeout,
    id,
    clock,
    featureInstallers
) {
    private var binaryPath: String = ""
    private var flags: (LLModel, List<Message.System>) -> List<String> = { _, _ -> emptyList() }
    private var generateRequest: CliConfig.GenerateRequest<Input>? = null
    private var extractOutput: ((List<CliEvent>) -> Output)? = null
    private var env: Map<String, String> = emptyMap()

    override fun self(): CustomCliAgentBuilder<Input, Output> = this

    /**
     * Sets the binary path of the CLI tool.
     */
    public fun binaryPath(binaryPath: String): CustomCliAgentBuilder<Input, Output> = self().apply {
        this.binaryPath = binaryPath
    }

    /**
     * Sets the function that generates command-line flags.
     */
    public fun flags(flags: (LLModel, List<Message.System>) -> List<String>): CustomCliAgentBuilder<Input, Output> = self().apply {
        this.flags = flags
    }

    /**
     * Sets the function that generates the request string.
     */
    public fun generateRequest(generateRequest: CliConfig.GenerateRequest<Input>): CustomCliAgentBuilder<Input, Output> = self().apply {
        this.generateRequest = generateRequest
    }

    /**
     * Sets the function that extracts the output from CLI event lines.
     */
    public fun extractOutput(extractOutput: (List<CliEvent>) -> Output): CustomCliAgentBuilder<Input, Output> = self().apply {
        this.extractOutput = extractOutput
    }

    /**
     * Sets environment variables.
     */
    public fun env(env: Map<String, String>): CustomCliAgentBuilder<Input, Output> = self().apply {
        this.env = env
    }

    /**
     * Builds the custom CLI agent.
     */
    public fun build(): CliAIAgent<Input, Output> {
        val finalTransport = requireNotNull(this.transport) { "Transport is required" }
        require(binaryPath.isNotEmpty()) { "Binary path is required" }
        val generateRequest = requireNotNull(this.generateRequest) { "Generate request is required" }
        val extractOutputNotNull = requireNotNull(this.extractOutput) { "Extract output is required" }

        val customConfig = object : CliConfig<Input, Output> {
            override val transport: CliTransport = finalTransport
            override val binaryPath: String = this@CustomCliAgentBuilder.binaryPath
            override val workspace: String = this@CustomCliAgentBuilder.workspace
            override val env: Map<String, String> = this@CustomCliAgentBuilder.env
            override val timeout: Duration? = this@CustomCliAgentBuilder.timeout

            override fun flags(model: LLModel, systemMessages: List<Message.System>): List<String> =
                this@CustomCliAgentBuilder.flags(model, systemMessages)

            override fun generateRequest(input: Input): String =
                generateRequest.generateRequest(input)

            override fun extractOutput(events: List<CliEvent>): Output =
                extractOutputNotNull(events) ?: throw IllegalStateException("Failed to extract output")
        }

        return CliAIAgent(
            agentConfig = config,
            strategy = AIAgentCliStrategy(customConfig),
            id = id,
            clock = clock,
            installFeatures = {
                featureInstallers.forEach { it(this) }
            }
        )
    }
}
