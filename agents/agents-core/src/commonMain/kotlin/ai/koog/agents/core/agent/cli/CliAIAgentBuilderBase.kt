package ai.koog.agents.core.agent.cli

import ai.koog.agents.core.agent.CliAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLModel
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Base class for CLI AI agent builders.
 */
public abstract class CliAIAgentBuilderBase<Self : CliAIAgentBuilderBase<Self>> internal constructor(
    protected var config: AIAgentConfig,
    protected var transport: CliTransport? = null,
    protected var workspace: String = ".",
    protected var timeout: Duration? = null,
    protected var id: String? = null,
    protected var clock: Clock = Clock.System,
    protected val featureInstallers: MutableList<CliAIAgent.FeatureContext.() -> Unit> = mutableListOf(),
) {
    protected abstract fun self(): Self

    /**
     * Sets the CLI transport.
     */
    public fun transport(transport: CliTransport): Self = self().apply {
        this.transport = transport
    }

    /**
     * Sets the workspace directory.
     */
    public fun workspace(workspace: String): Self = self().apply {
        this.workspace = workspace
    }

    /**
     * Sets the execution timeout.
     */
    public fun timeout(timeout: Duration): Self = self().apply {
        this.timeout = timeout
    }

    /**
     * Sets the execution timeout in minutes.
     */
    public fun timeoutMin(timeoutMin: Long): Self = timeout(timeoutMin.minutes)

    /**
     * Adds the system prompt.
     */
    public fun systemPrompt(systemPrompt: String): Self = self().apply {
        this.config = config.copy(prompt = prompt(config.prompt) { system(systemPrompt) })
    }

    /**
     * Sets the prompt.
     */
    public fun prompt(prompt: Prompt): Self = self().apply {
        this.config = config.copy(prompt = prompt)
    }

    /**
     * Sets the LLM model.
     */
    public fun llModel(llModel: LLModel): Self = self().apply {
        this.config = config.copy(model = llModel)
    }

    /**
     * Sets the agent ID.
     */
    public fun id(id: String?): Self = self().apply {
        this.id = id
    }

    /**
     * Sets the clock.
     */
    public fun clock(clock: Clock): Self = self().apply {
        this.clock = clock
    }

    /**
     * Installs a feature.
     */
    public fun install(installer: CliAIAgent.FeatureContext.() -> Unit): Self = self().apply {
        this.featureInstallers.add(installer)
    }
}
