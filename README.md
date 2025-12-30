# AiAssistant - Kotlin Multiplatform On-Device AI

AiAssistant is a Kotlin Multiplatform (KMP) project that brings Large Language Models (LLMs) directly to Android and iOS devices. Using Google's MediaPipe GenAI, it performs high-performance AI inference entirely on-device, ensuring privacy and offline capability.

## ✨ Features
- **On-Device LLM:** Private and secure AI chat without cloud dependency.
- **Cross-Platform:** Shared business logic and UI across Android and iOS using Compose Multiplatform.
- **Dynamic Model Loading:** Efficiently downloads and manages heavy LLM weights (Gemma, etc.) at runtime.
- **Streaming Responses:** Real-time token generation for a responsive chat experience.

## 🛠 Tech Stack
- **Framework:** [Kotlin Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html)
- **UI:** [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- **AI Engine:** [MediaPipe GenAI Tasks](https://developers.google.com/mediapipe/solutions/genai/llm_inference)
- **DI:** [Koin](https://insert-koin.io/)
- **Concurrency:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- **Storage:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)

## 📂 Project Structure
- `composeApp/src/commonMain`: Shared UI components, ViewModels, and core logic.
- `composeApp/src/androidMain`: Android implementation of MediaPipe GenAI.
- `composeApp/src/iosMain`: iOS implementation of MediaPipe GenAI via CocoaPods.
- `iosApp`: Native iOS project wrapper using the shared `composeApp` framework.

---

## 🚀 Setup & Installation

### Prerequisites
- **JDK 17**
- **Android Studio** (Latest stable version)
- **Xcode** 16.0+
- **CocoaPods** (`brew install cocoapods`)

### 🐘 Gradle Setup
The project uses Version Catalogs for dependency management. Ensure your `composeApp/build.gradle.kts` is configured as follows:

```kotlin
plugins {

    // ... other plugins
    
    alias(libs.plugins.kotlinCocoapods)
}

kotlin {

    // ... other configuration
    
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
        }

        pod("MediaPipeTasksGenAI") {
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {

        androidMain.dependencies {
            // ... other common dependencies
            
            // MediaPipe LLM Inference
            implementation(libs.mediapipe.tasks.genai)
            implementation(libs.mediapipe.tasks.vision)
        }
    }

    // ... other configuration
}
```

```toml
[versions]

# Others...

# LLM Inference (mediapipe task gen)
mediapipe = "0.10.29"

[libraries]

# Others libraries ...

# LLM Inference (mediapipe task gen)
mediapipe-tasks-genai = { module = "com.google.mediapipe:tasks-genai", version.ref = "mediapipe" }
mediapipe-tasks-vision = { module = "com.google.mediapipe:tasks-vision", version.ref = "mediapipe" }


[plugins]

# Other plugins ...

# Cocoapods
kotlinCocoapods = { id = "org.jetbrains.kotlin.native.cocoapods", version.ref = "kotlin" }
```

### 📱 Android Setup
1. Open the project in **Android Studio**.
2. The `AndroidManifest.xml` is pre-configured with `INTERNET` permissions for model downloading and a custom `LlmContextInitializer`.
3. Select the `composeApp` run configuration and target an Android device (API 24+).
4. Run the app.

### 🍎 iOS Setup (CocoaPods)
The project uses the `kotlin-cocoapods` plugin for seamless integration with the Apple ecosystem.

1. **Configure Gradle**: Ensure `gradle.properties` contains:
   ```properties
   kotlin.apple.deprecated.allowUsingEmbedAndSignWithCocoaPodsDependencies=true
   ```
2. **Install Pods**:
   ```bash
   ./gradlew podInstall
   ./gradlew :composeApp:generateDummyFramework
   ```
   And then,
   ```bash
   cd iosApp
   pod install
   ```
3. **Open Workspace**: Always open `iosApp.xcworkspace` in Xcode.
4. **Static Linkage**: The project is configured for static linkage in the `Podfile`:
   ```ruby
   use_frameworks! :linkage => :static
   ```
5. **Run**: Select an iOS Simulator (iOS 16.0+) or physical device and press **Cmd + R**.

---

## 🏗 Configuration Details

### MediaPipe Models
The app is designed to work with models like **Gemma 2b**.
- **Android**: Expects a `.task` model file.
- **iOS**: Expects a `.bin` model file.
The `AiAssistantViewModel` handles the platform-specific URL and filename mapping automatically.

---

## 🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License
This project is licensed under the MIT License.
