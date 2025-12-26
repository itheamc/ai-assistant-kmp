import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    // Cocoapods
    alias(libs.plugins.kotlinCocoapods)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0"
        summary = "Shared module for MediaPipe LLM"
        homepage = "https://github.com/google/mediapipe"
        ios.deploymentTarget = "16.0"

        framework {
            baseName = "ComposeApp"
            isStatic = true

            // Add these flags to fix the Swift Compatibility errors
            linkerOpts("-L/usr/lib/swift", "-macosx_version_min", "11.0") // if building on macOS
            linkerOpts("-L/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphonesimulator")
        }

        pod("MediaPipeTasksGenAIC") {
            version = "~> 0.10.21"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        pod("MediaPipeTasksGenAI") {
            version = "0.10.21"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        // Add this at the bottom of the cocoapods block
        extraSpecAttributes["pod_target_xcconfig"] = "{ 'OTHER_LDFLAGS' => '-lObjC' }"
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Coroutines
            implementation(libs.kotlinx.coroutines.android)

            // LLM Inference (mediapipe task gen)
            implementation(libs.mediapipe.tasks.genai)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // For Extended Icon
            implementation(libs.icons.extended)

            // For Navigation
            implementation(libs.navigation.compose)

            // For Koin DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // For Datastore
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)

            // For Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // DateTime
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.itheamc.aiassistant"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.itheamc.aiassistant"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
