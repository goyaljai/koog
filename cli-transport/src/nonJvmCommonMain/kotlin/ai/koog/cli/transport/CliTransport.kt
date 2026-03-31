package ai.koog.cli.transport

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Interface for cli transport implementations.
 */
public actual interface CliTransport {
    /**
     * Checks if the required cli binary is available.
     */
    public actual fun checkAvailability(binary: String): CliAvailability

    /**
     * Executes the cli command and returns a Flow of AgentEvents.
     */
    public actual fun execute(
        command: List<String>,
        workspace: String,
        env: Map<String, String>,
        timeout: Duration?
    ): Flow<CliEvent>
}
