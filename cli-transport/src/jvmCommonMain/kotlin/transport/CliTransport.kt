package ai.koog.cli.transport

import ai.koog.agents.annotations.JavaAPI
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.time.Duration

/**
 * Interface for running cli tools.
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

    /**
     * Default implementation of ProcessTransport using a ProcessBuilder to spawn a new process in available shell.
     */
    public object Default : ProcessCliTransport() {
        private val isWindows = System.getProperty("os.name").lowercase().contains("win")

        override fun buildCommand(
            command: List<String>,
            workspace: String,
            env: Map<String, String>
        ): List<String> = if (isWindows) {
            listOf("cmd", "/c") + command
        } else {
            command
        }
    }

    public companion object {
        /**
         * Default implementation of ProcessTransport using a ProcessBuilder to spawn a new process in available shell.
         */
        @JavaAPI
        @JvmStatic
        @JvmName("getDefault")
        public fun default(): CliTransport = Default

        /**
         * Creates a [DockerCliTransport] with the specified image and optional volumes.
         */
        @JavaAPI
        @JvmStatic
        @JvmOverloads
        public fun withDocker(
            imageName: String,
            volumes: List<DockerVolume> = emptyList()
        ): CliTransport = DockerCliTransport(imageName, volumes)
    }
}
