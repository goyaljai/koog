package ai.koog.agents.features.opentelemetry.mock

import io.opentelemetry.kotlin.attributes.AttributesMutator
import io.opentelemetry.kotlin.context.Context
import io.opentelemetry.kotlin.context.ContextKey
import io.opentelemetry.kotlin.context.Scope
import io.opentelemetry.kotlin.factory.ContextFactory
import io.opentelemetry.kotlin.tracing.Tracer
import io.opentelemetry.kotlin.tracing.model.Span
import io.opentelemetry.kotlin.tracing.model.SpanContext
import io.opentelemetry.kotlin.tracing.model.SpanCreationAction
import io.opentelemetry.kotlin.tracing.model.SpanKind

/**
 * A mock implementation of the Kotlin OTel SDK Tracer for testing.
 */
class MockTracer : Tracer {
    val createdSpans = mutableListOf<MockSpan>()

    override fun startSpan(
        name: String,
        parentContext: Context?,
        spanKind: SpanKind,
        startTimestamp: Long?,
        action: (SpanCreationAction.() -> Unit)?
    ): Span {
        val mockSpan = MockSpan(spanKind = spanKind, startTimestamp = startTimestamp ?: System.nanoTime())
        mockSpan.name = name

        // Execute the creation action to collect attributes
        if (action != null) {
            val creationAction = MockSpanCreationAction(mockSpan)
            action.invoke(creationAction)
        }

        createdSpans.add(mockSpan)
        return mockSpan
    }

    fun clear() {
        createdSpans.clear()
    }
}

/**
 * Mock SpanCreationAction that delegates to MockSpan for attribute collection.
 */
private class MockSpanCreationAction(
    private val span: MockSpan
) : SpanCreationAction {
    override fun setBooleanAttribute(key: String, value: Boolean) = span.setBooleanAttribute(key, value)
    override fun setStringAttribute(key: String, value: String) = span.setStringAttribute(key, value)
    override fun setLongAttribute(key: String, value: Long) = span.setLongAttribute(key, value)
    override fun setDoubleAttribute(key: String, value: Double) = span.setDoubleAttribute(key, value)
    override fun setBooleanListAttribute(key: String, value: List<Boolean>) = span.setBooleanListAttribute(key, value)
    override fun setStringListAttribute(key: String, value: List<String>) = span.setStringListAttribute(key, value)
    override fun setLongListAttribute(key: String, value: List<Long>) = span.setLongListAttribute(key, value)
    override fun setDoubleListAttribute(key: String, value: List<Double>) = span.setDoubleListAttribute(key, value)
    override fun addLink(spanContext: SpanContext, attributes: (AttributesMutator.() -> Unit)?) {}
    override fun addEvent(name: String, timestamp: Long?, attributes: (AttributesMutator.() -> Unit)?) {
        span.addEvent(name, timestamp, attributes)
    }
}

/**
 * A mock implementation of the Kotlin OTel SDK ContextFactory for testing.
 */
class MockContextFactory : ContextFactory {
    override fun root(): Context = MockContext()
    override fun implicit(): Context = MockContext()
    override fun storeSpan(context: Context, span: Span): Context = context
}

/**
 * A mock implementation of Context for testing.
 */
class MockContext : Context {
    private val values = mutableMapOf<String, Any?>()

    override fun <T> createKey(name: String): ContextKey<T> = MockContextKey(name)

    override fun <T> set(key: ContextKey<T>, value: T?): Context {
        val newCtx = MockContext()
        newCtx.values.putAll(values)
        newCtx.values[(key as MockContextKey).name] = value
        return newCtx
    }

    override fun <T> get(key: ContextKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return values[(key as MockContextKey).name] as T?
    }

    override fun attach(): Scope = MockScope
}

private class MockContextKey<T>(val name: String) : ContextKey<T>

private object MockScope : Scope {
    override fun detach() {}
}
