# Smart Voice Android 0.0.1

This repository now contains the native Android application module (`app/`). The interface includes a first-run welcome screen, voice ZIP import with cancellation, voice selection, speech volume/pitch/rate sliders, emoji toggle, editable user dictionary with TXT/CFG import and TXT export, and interface-language selection in English, Portuguese, and Spanish. When emoji reading is enabled, offline CLDR emoji names are selected using the current voice's language. The APK includes the CLDR annotation locales published in the bundled data; locale-specific tables overlay their base-language table, and missing locales fall back to English. Regional indicator flags are spoken using a localized Android country name. The Unicode CLDR data and license are in `app/src/main/assets/emoji/`. It exposes an Android `TextToSpeechService`, stores imported packages privately, and has synthesis adapters for locally supplied Google models and Microsoft's Embedded Speech SDK.

## Build

The Android project uses Gradle, Java 17, Android Gradle Plugin 8.7.3, `minSdk 26` (Android 8.0), and `targetSdk 35`. Build a debug APK with:

```sh
gradle --no-daemon assembleDebug
```

The resulting APK is `app/build/outputs/apk/debug/app-debug.apk`. A copy of the current installable debug build is in `releases/SmartVoice-0.0.1-debug.apk`.

## Current engine support

- **Google local models:** the importer recognizes the Google `config.yaml` layout in this repository's Google ZIPs and resolves model folders by voice ID/speaker. Synthesis is wired to the local Android engine classes and the ARM64 JNI runtime recovered from the repository's MultiTTS APK. It performs local synthesis and does not intentionally call an online service. The current bundled Google native runtime is ARM64 only. Runtime compatibility still needs confirmation on a physical ARM64 Android device; a successful APK build alone does not prove synthesis works.
- **Microsoft offline voices:** the imported Microsoft descriptor provides each voice's model folder and engine parameter. The adapter follows MultiTTS's offline path using `EmbeddedSpeechConfig.fromPath` plus `setSpeechSynthesisVoice`; cloud synthesis is excluded. The SDK accepts the imported per-voice parameter as provided by that package. Actual playback still needs a physical-device check with these exact voice archives; successful compilation cannot prove the local Microsoft runtime accepts every archive.
- **Microsoft neural voices:** not enabled. There is no offline Android neural model package/runtime in the repository that can be used to synthesize them. No online fallback is used.

An optional Microsoft model license value is encrypted with Android Keystore AES-GCM before storage. Package-provided engine parameters remain part of the imported voice package and are passed to the offline SDK adapter as MultiTTS does.

## Import package format

This does **not** mean every arbitrary voice ZIP can be imported. The importer accepts Microsoft and Google archives that use the MultiTTS-style `config.yaml` descriptor. Microsoft voice entries must identify their model folder and include the engine parameter expected by the offline adapter. Google packages must include the engine descriptor plus each voice's local model files (`pipeline.pb` and `backend_pipeline.textproto`); the bundled native Google runtime currently supports ARM64 only. The sample supported-engine packages are in `data/voice-packages/`. ZIPs are extracted to app-private storage; path traversal, more than 100,000 entries, and expanded data over 3 GiB are rejected.

Packages from other sources may need a matching descriptor/layout adaptation. Package import/listing is distinct from synthesis: real voice playback still needs verification on a device for each engine and package family. The Windows OneCore format is not a universal Android format, so do not assume a random OneCore folder or ZIP is compatible solely because it contains OneCore voice files.

## Validation status

A real Gradle debug APK build has been produced in this workspace. There is no connected Android device or emulator here, so package import, service registration in system settings, and actual audio output have not yet been exercised on-device. In particular, Google's extracted native runtime has only ARM64 support in the provided APK and needs a device check.
