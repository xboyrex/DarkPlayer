import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.koin.compiler)
}

android {
    namespace = "com.mayank.darkplayer"

    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        applicationId = "com.mayank.darkplayer"
        versionCode = 100 + (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 88)
        versionName = "3.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "88"}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${project.rootDir}/app/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val releaseKeystore = file("${project.rootDir}/app/release.keystore")
            if (releaseKeystore.exists() && !System.getenv("KEYSTORE_PASSWORD").isNullOrBlank()) {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "darkplayer"
                keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }

        create("release-with-debug-signing") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".release"
            matchingFallbacks.add("release")
        }
    }

    splits {
        abi {
            //noinspection WrongGradleMethod
            val isBuildingBundle = gradle.startParameter.taskNames.any {
                it.lowercase().contains("bundle")
            }

            isEnable = !isBuildingBundle
            reset()
            include(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64",
            )
            isUniversalApk = true
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            // Picnic signature data from Bouncy Castle; SSH never negotiates Picnic.
            excludes.add("/org/bouncycastle/pqc/legacy/picnic/**")
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.android.jvm.get()))
    }
}

dependencies {

    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":feature:network"))
    implementation(project(":feature:playlist"))
    implementation(project(":feature:videopicker"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    implementation(libs.google.android.material)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    implementation(libs.koin.compose.viewmodel)

    implementation(libs.accompanist.permissions)
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")

    implementation(libs.darkplayer.mediainfo)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(project(":core:domain"))
    androidTestImplementation(libs.androidx.datastore.core)
    androidTestImplementation(libs.androidx.media3.session)
    androidTestImplementation(libs.androidx.media3.ui)
    androidTestImplementation(libs.androidx.media3.datasource)
    androidTestImplementation(libs.androidx.media3.exoplayer)
    androidTestImplementation(libs.darkplayer.media3ext)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}
