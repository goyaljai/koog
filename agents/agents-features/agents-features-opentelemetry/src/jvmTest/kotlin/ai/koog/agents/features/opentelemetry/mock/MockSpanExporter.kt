package ai.koog.agents.features.opentelemetry.mock

import ai.koog.agents.features.opentelemetry.attribute.SpanAttributes
import ai.koog.utils.io.Closeable
import io.opentelemetry.kotlin.export.OperationResultCode
import io.opentelemetry.kotlin.tracing.data.SpanData
import io.opentelemetry.kotlin.tracing.export.SpanExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A mock span exporter that captures spans created by the OpenTelemetry feature.
 * This allows us to inject a MockTracer into the OpenTelemetry feature.
 *
 * @param filter a function that determines whether a given span should be exported. Defaults to exporting all spans.
 */
internal class MockSpanExporter : SpanExporter, Closeable {

    companion object {
        private val createAgentSpanOperationAttribute =
            SpanAttributes.Operation.Name(SpanAttributes.Operation.OperationNameType.CREATE_AGENT)
    }

    private val _collectedSpans = CopyOnWriteArrayList<SpanData>()

    val collectedSpans: List<SpanData>
        get() = _collectedSpans

    val runIds: List<String>
        get() {
            return collectedSpans.mapNotNull { span ->
                span.attributes["gen_ai.conversation.id"] as? String
            }.distinct()
        }

    val lastRunId: String
        get() = runIds.last()

    private val _isCollected: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val isCollected: StateFlow<Boolean>
        get() = _isCollected.asStateFlow()

    override suspend fun export(telemetry: List<SpanData>): OperationResultCode {
        telemetry.forEach { span ->
            _collectedSpans.add(span)

            val isCreateAgentSpan = span.attributes.any { (key, value) ->
                // Note! This code will wait until the first CreateAgentSpan is collected.
                //  If the test verifies multiple CreateAgentSpans, this check will give an unexpected result.
                key == createAgentSpanOperationAttribute.key && value == createAgentSpanOperationAttribute.value
            }

            if (isCreateAgentSpan) {
                _isCollected.value = true
            }
        }

        return OperationResultCode.Success
    }

    override suspend fun forceFlush(): OperationResultCode {
        return OperationResultCode.Success
    }

    override suspend fun shutdown(): OperationResultCode {
        return OperationResultCode.Success
    }

    override suspend fun close() {
        shutdown()
    }
}
