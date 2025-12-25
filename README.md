# AiAssistant - Kotlin Multiplatform Project

This is a Kotlin Multiplatform project targeting Android and iOS, featuring LLM capabilities via Google MediaPipe.

## Project Structure

* [/composeApp](./composeApp/src) - Shared code for Android and iOS.
  - [commonMain](./composeApp/src/commonMain/kotlin) - UI and logic shared across all platforms.
  - [androidMain](./composeApp/src/androidMain/kotlin) - Android-specific implementation using MediaPipe Tasks GenAI.
  - [iosMain](./composeApp/src/iosMain/kotlin) - iOS-specific implementation using CocoaPods for MediaPipe.
* [/iosApp](./iosApp) - The native iOS wrapper application.

## CocoaPods & MediaPipe Integration

The project uses the `kotlin-cocoapods` plugin to manage iOS dependencies and integrate with Xcode.

### 1. Dependencies and Configuration

The following files were updated to support the CocoaPods setup:

*   **`gradle/libs.versions.toml`**: Defines the `kotlinCocoapods` plugin.
*   **`build.gradle.kts` (root)**: Applies the `kotlinCocoapods` plugin.
*   **`composeApp/build.gradle.kts`**: 
    - Applies the `kotlinCocoapods` plugin.
    - Configures the `cocoapods` block with:
        - `ios.deploymentTarget = "15.0"`
        - `framework { baseName = "ComposeApp" }`
        - Pod dependencies: `MediaPipeTasksGenAIC` and `MediaPipeTasksGenAI`.
    - Explicitly declares iOS targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`).
*   **`gradle.properties`**: 
    - Added `kotlin.apple.deprecated.allowUsingEmbedAndSignWithCocoaPodsDependencies=true` to resolve conflicts between manual embedding and CocoaPods management.

### 2. Manual Setup Steps

To build the iOS target, follow these steps:

1.  **Install CocoaPods**: Ensure you have CocoaPods installed on your system (`brew install cocoapods`).
2.  **Generate Podfile and Install**:
    Navigate to the `iosApp` directory and run:
    ```bash
    cd iosApp
    pod install
    ```
    This will generate the `.xcworkspace` and link the Kotlin framework as a pod.
3.  **Open Workspace**:
    Always open `iosApp.xcworkspace` in Xcode (instead of `.xcodeproj`).
4.  **Xcode Build Phase**:
    Ensure the "Run Script" phase that calls `embedAndSignAppleFrameworkForXcode` is removed or disabled if you rely entirely on CocoaPods for framework integration.

## Build and Run

### Android
- Run from Android Studio or:
  ```bash
  ./gradlew :composeApp:assembleDebug
  ```

### iOS
- Run from Xcode using the `iosApp` scheme.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and [MediaPipe LLM Inference](https://developers.google.com/mediapipe/solutions/genai/llm_inference).
