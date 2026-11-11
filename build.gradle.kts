import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
}

allprojects {
    if (!project.name.startsWith("glasmio")) {
        apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(rootProject.libs.versions.ktlintCli.get())
            android.set(true)
            outputColorName.set("RED")
            ignoreFailures.set(true)
        }

        dependencies.add(
            "ktlintRuleset",
            project(":ktlint-rules"),
        )
    }
}

subprojects {
    if (project.name.startsWith("glasmio")) {
        plugins.withId("com.android.library") {
            configure<LibraryExtension> {
                compileSdk = 37
                defaultConfig {
                    minSdk = 24
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                buildFeatures {
                    compose = true
                }
                lint {
                    abortOnError = false
                    checkReleaseBuilds = false
                }
            }
        }
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }
    } else {
        apply(plugin = rootProject.libs.plugins.aboutLibraries.get().pluginId)

        configure<com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension> {
            offlineMode.set(true)
            export {
                excludeFields.addAll("generated")
            }
        }
    }
}

allprojects {
    tasks.withType<Test>().configureEach {
        testLogging {
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_OUT,
                TestLogEvent.STANDARD_ERROR,
            )

            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
        ignoreFailures = true
    }
}
