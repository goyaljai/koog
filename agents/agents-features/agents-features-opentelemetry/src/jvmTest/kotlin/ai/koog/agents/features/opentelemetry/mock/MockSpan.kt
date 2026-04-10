package ai.koog.agents.features.opentelemetry.mock

import io.opentelemetry.kotlin.tracing.data.SpanEventData
import io.opentelemetry.kotlin.tracing.data.SpanLinkData
import io.opentelemetry.kotlin.tracing.data.StatusData
import io.opentelemetry.kotlin.tracing.model.Span
import io.opentelemetry.kotlin.tracing.model.SpanContext
import io.opentelemetry.kotlin.tracing.model.SpanKind
import io.opentelemetry.kotlin.tracing.model.TraceFlags
import io.opentelemetry.kotlin.tracing.model.TraceState

/**
 * A mock implementation of Kotlin OTel SDK Span for testing.
 */
class MockSpan(
    override val spanKind: SpanKind = SpanKind.INTERNAL,
    override val startTimestamp: Long = System.nanoTime(),
) : Span {

    var isStarted = true
    var isEnded = false

    override var name: String = ""
    override var status: StatusData = StatusData.Unset
    override val parent: SpanContext = MockSpanContext()

    private val _spanContext = MockSpanContext()
    override val spanContext: SpanContext get() = _spanContext

    private val _attributes = mutableMapOf<String, Any>()
    override val attributes: Map<String, Any> get() = _attributes.toMap()

    private val _events = mutableListOf<MockSpanEventData>()
    override val events: List<SpanEventData> get() = _events.toList()

    override val links: List<SpanLinkData> = emptyList()

    // Collected data for test assertions
    val collectedAttributes: Map<String, Any> get() = _attributes.toMap()
    val collectedEvents: List<MockSpanEventData> get() = _events.toList()

    // AttributesMutator
    override fun setBooleanAttribute(key: String, value: Boolean) {
        _attributes[key] = value
    }

    override fun setStringAttribute(key: String, value: String) {
        _attributes[key] = value
    }

    override fun setLongAttribute(key: String, value: Long) {
        _attributes[key] = value
    }

    override fun setDoubleAttribute(key: String, value: Double) {
        _attributes[key] = value
    }

    override fun setBooleanListAttribute(key: String, value: List<Boolean>) {
        _attributes[key] = value
    }

    override fun setStringListAttribute(key: String, value: List<String>) {
        _attributes[key] = value
    }

    override fun setLongListAttribute(key: String, value: List<Long>) {
        _attributes[key] = value
    }

    override fun setDoubleListAttribute(key: String, value: List<Double>) {
        _attributes[key] = value
    }

    // SpanEventCreator
    override fun addEvent(name: String, timestamp: Long?, attributes: (io.opentelemetry.kotlin.attributes.AttributesMutator.() -> Unit)?) {
        val eventAttrs = mutableMapOf<String, Any>()
        val mutator = MockAttributesMutator(eventAttrs)
        attributes?.invoke(mutator)
        _events.add(MockSpanEventData(name, timestamp ?: System.nanoTime(), eventAttrs.toMap()))
    }

    // SpanLinkCreator
    override fun addLink(spanContext: SpanContext, attributes: (io.opentelemetry.kotlin.attributes.AttributesMutator.() -> Unit)?) {
        // no-op for tests
    }

    // Span lifecycle
    override fun end() {
        isEnded = true
    }

    override fun end(timestamp: Long) {
        isEnded = true
    }

    override fun isRecording(): Boolean = isStarted && !isEnded
}

/**
 * Mock SpanContext for testing.
 */
class MockSpanContext : SpanContext {
    override val traceId: String = "00000000000000000000000000000000"
    override val traceIdBytes: ByteArray = ByteArray(16)
    override val spanId: String = "0000000000000000"
    override val spanIdBytes: ByteArray = ByteArray(8)
    override val traceFlags: TraceFlags = MockTraceFlags
    override val isValid: Boolean = false
    override val isRemote: Boolean = false
    override val traceState: TraceState = MockTraceState
}

object MockTraceFlags : TraceFlags {
    override val isSampled: Boolean = false
    override val isRandom: Boolean = false
}

object MockTraceState : TraceState {
    override fun get(key: String): String? = null
    override fun asMap(): Map<String, String> = emptyMap()
    override fun put(key: String, value: String): TraceState = this
    override fun remove(key: String): TraceState = this
}

/**
 * Mock SpanEventData for assertions.
 */
data class MockSpanEventData(
    override val name: String,
    override val timestamp: Long,
    override val attributes: Map<String, Any>
) : SpanEventData

/**
 * Mock AttributesMutator that collects attributes into a map.
 */
class MockAttributesMutator(
    private val attrs: MutableMap<String, Any>
) : io.opentelemetry.kotlin.attributes.AttributesMutator {
    override fun setBooleanAttribute(key: String, value: Boolean) {
        attrs[key] = value
    }

    override fun setStringAttribute(key: String, value: String) {
        attrs[key] = value
    }

    override fun setLongAttribute(key: String, value: Long) {
        attrs[key] = value
    }

    override fun setDoubleAttribute(key: String, value: Double) {
        attrs[key] = value
    }

    override fun setBooleanListAttribute(key: String, value: List<Boolean>) {
        attrs[key] = value
    }

    override fun setStringListAttribute(key: String, value: List<String>) {
        attrs[key] = value
    }

    override fun setLongListAttribute(key: String, value: List<Long>) {
        attrs[key] = value
    }

    override fun setDoubleListAttribute(key: String, value: List<Double>) {
        attrs[key] = value
    }
}
