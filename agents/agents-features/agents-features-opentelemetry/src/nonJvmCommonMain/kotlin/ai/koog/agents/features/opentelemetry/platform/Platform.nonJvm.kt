package ai.koog.agents.features.opentelemetry.platform

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class RWLock {
    actual inline fun <T> read(action: () -> T): T = action()
    actual inline fun <T> write(action: () -> T): T = action()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual object PlatformInfo {
    actual val osName: String? = null
    actual val osVersion: String? = null
    actual val osArch: String? = null
}

internal actual fun errorTypeName(error: Throwable): String? = error::class.simpleName

internal actual fun loadProductProperties(): Map<String, String> = emptyMap()

internal actual fun registerShutdownHook(action: () -> Unit) {
    // No-op on non-JVM platforms
}
