package ai.koog.agents.features.opentelemetry.platform

import java.util.Properties
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class RWLock {
    private val lock = ReentrantReadWriteLock()

    actual inline fun <T> read(action: () -> T): T = lock.read(action)
    actual inline fun <T> write(action: () -> T): T = lock.write(action)
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual object PlatformInfo {
    actual val osName: String? = System.getProperty("os.name")
    actual val osVersion: String? = System.getProperty("os.version")
    actual val osArch: String? = System.getProperty("os.arch")
}

internal actual fun errorTypeName(error: Throwable): String? = error.javaClass.typeName

internal actual fun loadProductProperties(): Map<String, String> {
    val props = Properties()
    val classLoader = PlatformInfo::class.java.classLoader
    classLoader.getResourceAsStream("product.properties")?.use { stream ->
        props.load(stream)
    }
    return props.entries.associate { (k, v) -> k.toString() to v.toString() }
}

internal actual fun registerShutdownHook(action: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(Thread(action))
}
