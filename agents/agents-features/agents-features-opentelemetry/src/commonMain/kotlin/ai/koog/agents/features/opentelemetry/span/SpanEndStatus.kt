package ai.koog.agents.features.opentelemetry.span

import io.opentelemetry.kotlin.tracing.data.StatusData

internal data class SpanEndStatus(val statusData: StatusData)
