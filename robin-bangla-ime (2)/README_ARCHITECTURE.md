# Robin Bangla IME - Architecture & Implementation Plan

## 1. Architecture Overview
This application is built on the Android `InputMethodService` (IMS) framework. It follows a clean, modular architecture:

*   **Core Service (`BanglaIME`):** Extends `InputMethodService`. It manages the lifecycle, view inflation, and connection to the input field (`InputConnection`).
*   **Logic Engine (`PhoneticEngine`):** A standalone Kotlin object that handles the complex logic of converting Roman characters (e.g., "kkh") into Bangla characters (e.g., "ক্ষ") using a suffix-matching algorithm and buffer management.
*   **UI Layer:**
    *   **Keyboard View:** A custom XML layout utilizing Android's legacy `KeyboardView` (for reliability and performance) with heavily customized attributes for a modern look.
    *   **Activities:** `SettingsActivity` for configuration and `AboutActivity` for developer credits.
*   **Voice Module:** Integrates `Android SpeechRecognizer` directly into the keyboard service for seamless voice-to-text without leaving the app context.

## 2. Implementation Steps
1.  **Manifest Setup:** Define the `InputMethodService` and permissions (`RECORD_AUDIO`, `INTERNET`).
2.  **Phonetic Engine:** Build the dictionary mapping and the buffer processing logic.
3.  **Keyboard Layouts:** Design XML files for English (QWERTY), Bangla (Direct), and Symbols.
4.  **Service Implementation:** Handle `onKey`, `onText`, and mode switching logic in `BanglaIME.kt`.
5.  **Voice Integration:** Implement `RecognitionListener` to handle audio streams.
6.  **Developer Identity:** Create the `AboutActivity` honoring Md Ishtiak Ahmed Robin.

## 3. Play Store Readiness Checklist
- [x] **Privacy Policy:** Must state that no keystrokes are sent to a server.
- [x] **Permissions:** Request `RECORD_AUDIO` only at runtime when the user taps the mic.
- [x] **Target SDK:** Ensure `targetSdk` is 34+.
- [x] **Store Listing:** High-res icon and feature graphics.
- [x] **Content Rating:** Rated 3+ (General Audience).

## 4. Performance Optimization
- **Memory:** The `PhoneticEngine` uses static maps to avoid garbage collection churn during typing.
- **Rendering:** Keyboard layouts are pre-cached.
- **Latency:** Voice typing runs on a background thread via the System API.

