package ai.koog.agents.features.opentelemetry.platform

/**
 * Read-write lock abstraction for thread safety in a span collection.
 * JVM: backed by ReentrantReadWriteLock; non-JVM: no-op (single-threaded).
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect class RWLock() {
    inline fun <T> read(action: () -> T): T
    inline fun <T> write(action: () -> T): T
}

/**
 * Platform information (OS name, version, architecture).
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object PlatformInfo {
    val osName: String?
    val osVersion: String?
    val osArch: String?
}

/**
 * Returns the JVM class type name of the given throwable, or its Kotlin qualified name on non-JVM.
 */
internal expect fun errorTypeName(error: Throwable): String?

/**
 * Returns the type name for an [ai.koog.agents.core.feature.model.AIAgentError].
 * Since AIAgentError is a data class (not Throwable), this simply returns the class name.
 */
internal fun errorTypeName(error: ai.koog.agents.core.feature.model.AIAgentError): String? =
    error::class.simpleName

/**
 * Loads product properties from classpath resources (JVM) or returns an empty map (non-JVM).
 */
internal expect fun loadProductProperties(): Map<String, String>

/**
 * Registers a shutdown hook for cleanup. JVM: Runtime.addShutdownHook; non-JVM: no-op.
 */
internal expect fun registerShutdownHook(action: () -> Unit)
