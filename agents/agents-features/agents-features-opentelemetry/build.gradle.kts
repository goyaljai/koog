import ai.koog.gradle.publish.maven.Publishing.publishToMaven

group = rootProject.group
version = rootProject.version

plugins {
    id("ai.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

val rootProjectVersion = rootProject.version.toString()
val rootProjectGroup = rootProject.group.toString()

kotlin {
    // Disable wasmJs target — OpenTelemetry Kotlin SDK 0.2.0 does not publish wasmJs artifacts
    targets.removeAll { it.name == "wasmJs" }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":agents:agents-core"))
                api(project(":agents:agents-utils"))
                api(libs.kotlinx.serialization.json)
                implementation(project(":agents:agents-mcp-metadata"))

                api(libs.opentelemetry.kotlin.core)
                api(libs.opentelemetry.kotlin.sdk.api)
                api(libs.opentelemetry.kotlin.implementation)
                api(libs.opentelemetry.kotlin.exporters.core)
                api(libs.opentelemetry.kotlin.semconv)
            }
        }

        jvmMain {
            dependencies {
                // Java OTel SDK - ONLY for OtlpHttpSpanExporter used by Langfuse/Weave integrations
                // TODO: KG-785 - Remove Java OTel SDK + compat deps when Kotlin SDK provides OTLP exporter
                implementation(project.dependencies.platform(libs.opentelemetry.bom))
                implementation(libs.opentelemetry.exporter.otlp)

                // Compat bridge - ONLY for .toOtelKotlinSpanExporter() conversion
                implementation(libs.opentelemetry.kotlin.compat)
            }

            resources.srcDir(layout.buildDirectory.dir("generated/resources"))
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.opentelemetry.kotlin.exporters.inMemory)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(project(":agents:agents-test"))
                implementation(libs.junit.jupiter.params)
            }
        }
    }

    explicitApi()

    sourceSets.all {
        languageSettings.optIn("io.opentelemetry.kotlin.ExperimentalApi")
        languageSettings.optIn("io.opentelemetry.kotlin.semconv.IncubatingApi")
    }
}

val generateProductProperties = tasks.register("generateProductProperties") {
    val outputDir = layout.buildDirectory.dir("generated/resources")
    val propertiesFile = outputDir.get().file("product.properties")

    inputs.property("version", rootProjectVersion)
    inputs.property("group", rootProjectGroup)
    outputs.file(propertiesFile)

    doLast {
        propertiesFile.asFile.parentFile.mkdirs()
        propertiesFile.asFile.writeText(
            """
            version=$rootProjectVersion
            name=$rootProjectGroup
            """.trimIndent()
        )
    }
}

tasks.named("jvmProcessResources") {
    dependsOn(generateProductProperties)
}

// The convention plugin creates wasmJs target/tasks, but OTel Kotlin SDK 0.2.0
// doesn't publish wasmJs artifacts. Exclude OTel deps from wasmJs configurations
// so they resolve without errors and disable wasmJs tasks to skip compilation.
configurations.matching { it.name.startsWith("wasmJs") }.configureEach {
    exclude(group = "io.opentelemetry.kotlin")
}
afterEvaluate {
    tasks.matching { it.name.startsWith("wasmJs") || it.name == "compileKotlinWasmJs" || it.name == "compileTestKotlinWasmJs" }.configureEach {
        enabled = false
    }
}

publishToMaven()
