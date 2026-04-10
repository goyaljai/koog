package ai.koog.agents.features.opentelemetry.extension

import ai.koog.agents.core.feature.model.AIAgentError
import ai.koog.agents.features.opentelemetry.attribute.Attribute
import ai.koog.agents.features.opentelemetry.attribute.applyAttributes
import ai.koog.agents.features.opentelemetry.event.GenAIAgentEvent
import ai.koog.agents.features.opentelemetry.span.SpanEndStatus
import io.opentelemetry.kotlin.tracing.data.StatusData
import io.opentelemetry.kotlin.tracing.model.Span

internal fun Span.setSpanStatus(endStatus: SpanEndStatus? = null) {
    status = endStatus?.statusData ?: StatusData.Ok
}

internal fun Span.setAttributes(attributes: List<Attribute>, verbose: Boolean) {
    applyAttributes(attributes, verbose)
}

internal fun Span.setEvents(events: List<GenAIAgentEvent>, verbose: Boolean) {
    events.forEach { event ->
        // The Kotlin SDK supports event attributes.
        // Pass body fields as attributes.
        val eventAttributes = buildList {
            event.bodyFieldsToAttributes(verbose)
            addAll(event.attributes)
        }

        addEvent(event.name) {
            applyAttributes(eventAttributes, verbose)
        }
    }
}

internal fun Throwable?.toSpanEndStatus(): SpanEndStatus =
    if (this == null) {
        SpanEndStatus(StatusData.Ok)
    } else {
        SpanEndStatus(StatusData.Error(this.message))
    }

internal fun AIAgentError?.toSpanEndStatus(): SpanEndStatus =
    if (this == null) {
        SpanEndStatus(StatusData.Ok)
    } else {
        SpanEndStatus(StatusData.Error(this.message))
    }
