import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.koin.compiler)
}

val ciRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val resolvedVersionCode = (findProperty("versionCode") as? String)?.toIntOrNull()
    ?: ciRunNumber?.let { 250 + it }
    ?: 310
val resolvedVersionName = (findProperty("versionName") as? String)
    ?: ciRunNumber?.let { "2.5.$it" }
    ?: "2.5.60"

val releaseStoreFilePath = (findProperty("RELEASE_STORE_FILE") as? String)
    ?: System.getenv("RELEASE_STORE_FILE")
val releaseStorePassword = (findProperty("RELEASE_STORE_PASSWORD") as? String)
    ?: System.getenv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = (findProperty("RELEASE_KEY_ALIAS") as? String)
    ?: System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = (findProperty("RELEASE_KEY_PASSWORD") as? String)
    ?: System.getenv("RELEASE_KEY_PASSWORD")
val hasDedicatedReleaseKeystore = !releaseStoreFilePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank() &&
    file(releaseStoreFilePath).exists()

android {
    namespace = "dev.xboyrex.darkplayer"

    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        applicationId = "dev.xboyrex.darkplayer"
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.android.jvm.get().toInt())
    }

    signingConfigs {
        getByName("debug") {
            val dbgFile = file("${project.rootDir}/app/debug.keystore")
            if (dbgFile.exists()) {
                storeFile = dbgFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (hasDedicatedReleaseKeystore) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasDedicatedReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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

    lint {
        checkReleaseBuilds = true
        abortOnError = false
        ignoreWarnings = true
    }

    splits {
        abi {
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
            excludes.add("/org/bouncycastle/pqc/legacy/picnic/**")
        }
    }

    dependenciesInfo {
        includeInApk = false
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

    implementation(libs.github.anilbeesetti.nextlib.mediainfo)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(project(":core:domain"))
    androidTestImplementation(libs.androidx.datastore.core)
    androidTestImplementation(libs.androidx.media3.session)
    androidTestImplementation(libs.androidx.media3.ui)
    androidTestImplementation(libs.github.anilbeesetti.nextlib.media3ext)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}
