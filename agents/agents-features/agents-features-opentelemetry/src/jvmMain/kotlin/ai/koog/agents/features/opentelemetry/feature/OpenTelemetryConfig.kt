package ai.koog.agents.features.opentelemetry.feature

import ai.koog.agents.annotations.JavaAPI
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.handler.AgentLifecycleEventContext
import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute
import ai.koog.agents.features.opentelemetry.integration.SpanAdapter
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporterImpl
import ai.koog.agents.features.opentelemetry.integration.weave.addWeaveExporterImpl
import ai.koog.agents.features.opentelemetry.platform.PlatformInfo
import ai.koog.agents.features.opentelemetry.platform.loadProductProperties
import ai.koog.agents.features.opentelemetry.platform.registerShutdownHook
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.kotlin.OpenTelemetry
import io.opentelemetry.kotlin.createOpenTelemetry
import io.opentelemetry.kotlin.factory.ContextFactory
import io.opentelemetry.kotlin.tracing.Tracer
import io.opentelemetry.kotlin.tracing.export.SpanExporter
import io.opentelemetry.kotlin.tracing.export.compositeSpanExporter
import io.opentelemetry.kotlin.tracing.export.simpleSpanProcessor
import io.opentelemetry.kotlin.tracing.export.stdoutSpanExporter
import io.opentelemetry.kotlin.tracing.export.toOtelKotlinSpanExporter
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

/**
 * Configuration class for OpenTelemetry integration.
 *
 * Provides seamless integration with the OpenTelemetry Kotlin SDK, allowing initialization
 * and customization of various components such as the tracer, exporters, etc.
 *
 * Uses the Kotlin Multiplatform OpenTelemetry SDK's `createOpenTelemetry { }` DSL for
 * native configuration. For OTLP exporters (Langfuse, Weave), Java SDK exporters are
 * wrapped via compat bridge.
 */
public class OpenTelemetryConfig : FeatureConfig() {

    private companion object {

        private val logger = KotlinLogging.logger { }
    }

    private val productProperties = loadProductProperties()

    private val customSpanExporters = mutableListOf<SpanExporter>()

    private val customResourceAttributes = mutableMapOf<String, Any>()

    private var _sdk: OpenTelemetry? = null

    private var _serviceName: String = productProperties["name"] ?: "ai.koog"

    private var _serviceVersion: String = productProperties["version"] ?: "0.0.0"

    private var _instrumentationScopeName: String = _serviceName

    private var _instrumentationScopeVersion: String = _serviceVersion

    // TODO: KG-785 — Restore setSampler() when Kotlin SDK adds sampler support to TracerProviderConfigDsl

    private var _verbose: Boolean = false

    private var _spanAdapter: SpanAdapter? = null

    override fun setEventFilter(filter: (AgentLifecycleEventContext) -> Boolean) {
        // Do not allow events filtering for the OpenTelemetry feature
        // Open Telemetry relay on the hierarchy. Filtering events can break the feature logic.
        throw UnsupportedOperationException("Events filtering is not allowed for the OpenTelemetry feature.")
    }

    /**
     * Indicates whether verbose telemetry data is enabled.
     *
     * When this value is `true`, the application collects more detailed telemetry data.
     * This setting is useful for debugging and detailed monitoring but may result in
     * increased resource usage or performance overhead.
     *
     * The value reflects the setting controlled through the `setVerbose(verbose: Boolean)` method,
     * with a default value of `false` if not explicitly configured.
     */
    public val isVerbose: Boolean
        get() = _verbose

    /**
     * Provides access to the `Tracer` instance for tracking and recording tracing data.
     * Returns a Kotlin Multiplatform OpenTelemetry SDK Tracer.
     */
    public val tracer: Tracer
        get() = sdk.tracerProvider.getTracer(_instrumentationScopeName, _instrumentationScopeVersion)

    /**
     * Provides access to the `ContextFactory` for managing span context.
     */
    internal val contextFactory: ContextFactory
        get() = sdk.context

    private val sdk: OpenTelemetry
        get() = _sdk ?: initializeOpenTelemetry().also { sdk ->
            _sdk = sdk

            // Set the instrumentation scope name only once when SDK is created
            _instrumentationScopeName = _serviceName
            _instrumentationScopeVersion = _serviceVersion
        }

    /**
     * The name of the service associated with this OpenTelemetry configuration.
     */
    public val serviceName: String
        get() = _serviceName

    /**
     * The version of the service used in the OpenTelemetry configuration.
     */
    public val serviceVersion: String
        get() = _serviceVersion

    internal val spanAdapter: SpanAdapter?
        get() = _spanAdapter

    /**
     * Sets the service information for the OpenTelemetry configuration.
     * This information is used to identify the service in telemetry data.
     *
     * @param serviceName The name of the service.
     * @param serviceVersion The version of the service.
     */
    public fun setServiceInfo(serviceName: String, serviceVersion: String) {
        _serviceName = serviceName
        _serviceVersion = serviceVersion
    }

    /**
     * Adds a Kotlin SDK SpanExporter to the OpenTelemetry configuration.
     *
     * @param exporter The SpanExporter instance to be added to the list of custom span exporters.
     */
    public fun addSpanExporter(exporter: SpanExporter) {
        customSpanExporters.add(exporter)
    }

    /**
     * Adds a Java SDK SpanExporter to the OpenTelemetry configuration.
     * The exporter is automatically converted to the Kotlin SDK type via the compat bridge.
     *
     * This overload accepts Java OpenTelemetry SDK exporters such as `OtlpGrpcSpanExporter`,
     * `OtlpHttpSpanExporter`, or `LoggingSpanExporter` directly, without requiring manual
     * `.toOtelKotlinSpanExporter()` conversion.
     *
     * @param exporter The Java SDK SpanExporter instance.
     */
    public fun addSpanExporter(exporter: io.opentelemetry.sdk.trace.export.SpanExporter) {
        customSpanExporters.add(exporter.toOtelKotlinSpanExporter())
    }

    // TODO: KG-785 — Restore addSpanProcessor() when Kotlin SDK exposes processor factories outside DSL scope.
    //  Currently simpleSpanProcessor/batchSpanProcessor are TraceExportConfigDsl extensions and cannot be
    //  called outside the export { } block. Internally we use simpleSpanProcessor as default.

    /**
     * Adds resource attributes to the OpenTelemetry configuration.
     * Resource attributes are key-value pairs that provide metadata
     * describing the entity producing telemetry data.
     *
     * @param attributes A map where the keys are attribute names and the values
     *                   are the attribute values. Supported types: String, Long, Double, Boolean.
     */
    public fun addResourceAttributes(attributes: Map<String, Any>) {
        customResourceAttributes.putAll(attributes)
    }

    // TODO: KG-785 — Restore setSampler() when Kotlin SDK adds sampler support to TracerProviderConfigDsl.
    //  The Kotlin SDK v0.2.0 defines a Sampler interface but does not wire it into the DSL.

    // TODO: KG-785 — Restore setSdk() when Kotlin SDK provides an equivalent injection mechanism.
    //  The current Kotlin SDK's createOpenTelemetry { } DSL does not support injecting
    //  a pre-configured SDK instance.

    /**
     * Controls whether verbose telemetry data should be captured during application execution.
     * When set to `true`, the application collects more detailed telemetry data.
     * This option can be useful for debugging and fine-grained monitoring but may impact performance.
     *
     * Default value is `false`, meaning verbose data capture is disabled.
     */
    public fun setVerbose(verbose: Boolean) {
        _verbose = verbose
    }

    /**
     * Adds a custom span adapter for post-processing GenAI agent spans.
     * The adapter can modify span data, add attributes/events, or perform other
     * post-processing logic before spans are completed.
     *
     * @param adapter The ProcessSpanAdapter implementation that will handle
     *                post-processing of GenAI agent spans
     */
    internal fun addSpanAdapter(adapter: SpanAdapter) {
        _spanAdapter = adapter
    }

    //region Private Methods

    private fun initializeOpenTelemetry(): OpenTelemetry {
        val resourceMap = buildResourceMap()
        val preConfiguredExporters = customSpanExporters.toList()

        val sdk = createOpenTelemetry {
            tracerProvider {
                resource(resourceMap)
                export {
                    val exporters = if (preConfiguredExporters.isEmpty()) {
                        logger.debug { "No custom span exporters configured. Using stdout span exporter by default." }
                        listOf(stdoutSpanExporter())
                    } else {
                        preConfiguredExporters.also { list ->
                            list.forEach { exporter ->
                                logger.debug { "Adding span exporter: ${exporter::class.simpleName}" }
                            }
                        }
                    }

                    val compositeExporter = if (exporters.size == 1) {
                        exporters.first()
                    } else {
                        compositeSpanExporter(*exporters.toTypedArray())
                    }

                    simpleSpanProcessor(compositeExporter)
                }
            }
        }

        registerShutdownHook {
            logger.debug { "Shutting down OpenTelemetry SDK" }
        }

        return sdk
    }

    private fun buildResourceMap(): Map<String, Any> = buildMap {
        put("service.name", _serviceName)
        put("service.version", _serviceVersion)
        put("service.instance.time", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))

        PlatformInfo.osName?.let { put("os.type", it) }
        PlatformInfo.osVersion?.let { put("os.version", it) }
        PlatformInfo.osArch?.let { put("os.arch", it) }

        putAll(customResourceAttributes)
    }

    //endregion Private Methods

    // integrations:

    /**
     * Configure an OpenTelemetry span exporter that sends data to [Langfuse](https://langfuse.com/).
     *
     * @param langfuseUrl the base URL of the Langfuse instance.
     *        If not a set is retrieved from `LANGFUSE_HOST` environment variable.
     *        Defaults to [https://cloud.langfuse.com](https://cloud.langfuse.com).
     * @param langfusePublicKey if not set, is retrieved from `LANGFUSE_PUBLIC_KEY` environment variable.
     * @param langfuseSecretKey if not set, is retrieved from `LANGFUSE_SECRET_KEY` environment variable.
     * @param timeout OpenTelemetry SpanExporter timeout.
     *        See [io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder.setTimeout].
     * @param traceAttributes list of trace-level Langfuse attributes.
     *        See the full list: [Trace-Level Attributes](https://langfuse.com/integrations/native/opentelemetry#trace-level-attributes)
     *
     * @see <a href="https://langfuse.com/docs/get-started#create-new-project-in-langfuse">How to create a new project in Langfuse</a>
     * @see <a href="https://langfuse.com/faq/all/where-are-langfuse-api-keys">How to set up API keys in Langfuse</a>
     * @see <a href="https://langfuse.com/docs/opentelemetry/get-started#opentelemetry-endpoint">Langfuse OpenTelemetry Docs</a>
     */
    @JavaAPI
    @JvmOverloads
    public fun addLangfuseExporter(
        langfuseUrl: String? = null,
        langfusePublicKey: String? = null,
        langfuseSecretKey: String? = null,
        timeout: JavaDuration? = null,
        traceAttributes: List<CustomAttribute>? = null
    ): Unit = this.addLangfuseExporterImpl(
        langfuseUrl,
        langfusePublicKey,
        langfuseSecretKey,
        timeout?.toKotlinDuration(),
        traceAttributes
    )

    /**
     * Configure an OpenTelemetry span exporter that sends data to [W&B Weave](https://wandb.ai/site/weave/).
     *
     * @param weaveOtelBaseUrl the URL of the Weave OpenTelemetry endpoint.
     *        If not set, is retrieved from `WEAVE_URL` environment variable.
     *        Defaults to [https://trace.wandb.ai](https://trace.wandb.ai).
     * @param weaveEntity can be found by visiting your W&B dashboard at [https://wandb.ai/home](https://wandb.ai/home) and
     *        checking the *Teams* field in the left sidebar.
     *        If not set, is retrieved from `WEAVE_ENTITY` environment variable.
     * @param weaveProjectName name of your Weave project.
     *        If not set, is retrieved from `WEAVE_PROJECT_NAME` environment variable.
     * @param weaveApiKey can be created on the [https://wandb.ai/authorize](https://wandb.ai/authorize) page.
     *        If not set, is retrieved from `WEAVE_API_KEY` environment variable.
     * @param timeout OpenTelemetry SpanExporter timeout.
     *        See [io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder.setTimeout].
     *
     * @see <a href="https://weave-docs.wandb.ai/guides/tracking/otel/">Weave OpenTelemetry Docs</a>
     */
    @JavaAPI
    @JvmOverloads
    public fun addWeaveExporter(
        weaveOtelBaseUrl: String? = null,
        weaveEntity: String? = null,
        weaveProjectName: String? = null,
        weaveApiKey: String? = null,
        timeout: JavaDuration? = null,
    ): Unit = addWeaveExporterImpl(
        weaveOtelBaseUrl,
        weaveEntity,
        weaveProjectName,
        weaveApiKey,
        timeout?.toKotlinDuration()
    )
}
