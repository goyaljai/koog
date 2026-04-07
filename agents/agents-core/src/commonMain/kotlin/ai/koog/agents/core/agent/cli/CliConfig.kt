package ai.koog.agents.core.agent.cli

import ai.koog.cli.transport.CliEvent
import ai.koog.cli.transport.CliTransport
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import kotlin.time.Duration

/**
 * Configuration for [AIAgentCliStrategy].
 */
public interface CliConfig<Input, Output> {
    /** CLI transport for executing commands. */
    public val transport: CliTransport

    /** Binary of the CLI tool. */
    public val binaryPath: String

    /** Working directory for command execution. */
    public val workspace: String

    /** Environment variables for command execution. */
    public val env: Map<String, String>

    /** Execution timeout. */
    public val timeout: Duration?

    /** Generates command-line flags based on LLM model and system messages. */
    public fun flags(model: LLModel, systemMessages: List<Message.System>): List<String>

    /** Generates the request string from context and input. */
    public fun generateRequest(input: Input): String

    /** Extracts the output from CLI event lines. */
    public fun extractOutput(events: List<CliEvent>): Output

    /**
     * Represents a function that generates a request string from context and input.
     */
    public fun interface GenerateRequest<Input> {
        /**
         * Generates a request string from context and input.
         */
        public fun generateRequest(input: Input): String
    }
}
