# Smart Voice for Android

Smart Voice is an Android text-to-speech engine for screen-reader users. Version `0.0.1` is written in Java and targets Android 8.0 (API 26) or newer. Stefan is the developer.

## Included application features

- Welcome screen and interface translations in English, Portuguese, and Spanish.
- Import compatible ZIP files from the app, or open a `.zip` in a file manager and choose Smart Voice. The app receives the URI through Android's `ACTION_VIEW` intent and opens the cancellable import flow.
- Voice selection, preview speech, and independent volume, pitch, and rate controls.
- Emoji names from bundled Unicode CLDR annotation tables. The selected voice's locale is used; English annotations seed the table so individual missing translations fall back to English.
- User dictionary with add, edit, delete, clear, TXT/CFG import, and TXT export, all grouped on the dictionary screen.
- System text-to-speech settings shortcut, interface-language selection, and a Telegram contact button for [@damomgamer](https://t.me/damomgamer).
- An online voice catalog with an **Access Mind Server** source, voice/language search, and language, gender, and quality filters. The list shows the server's voice, language code, gender, package variant, quality, and size. Selecting a row downloads and imports that ZIP through the same validator used by local imports, with progress and cancellation.
- Copy/share actions on package, download, and synthesis error dialogs so diagnostic details can be sent to the developer.
- The full TTS request path reports unexpected Java/JNI linkage errors through Android's synthesis callback. On Android 11+, the next launch can show and copy the OS trace from a previous Java or native process crash.

The importer checks the ZIP structure before registering data. Microsoft packages require their descriptor, model directory, `.APM` model, and `.INI` metadata. Google packages require a descriptor and the selected model's `pipeline.pb` and `backend_pipeline.textproto`. AccessMind Vocalizer packages are parsed from their XML `.hdr` metadata and `.dat` model files. ZIP path traversal, oversized archives, invalid descriptors, missing models, and unknown engines are rejected with an explanation.

## Engine and package status

- **Microsoft Embedded Speech:** MultiTTS-format offline model packages are recognized by the Embedded Speech adapter. Microsoft neural cloud voices are not enabled.
- **Google Local TTS:** MultiTTS-format local packages are recognized by the local Google adapter. The bundled Google native runtime is ARM64 only.
- **Vocalizer:** The importer recognizes MultiTTS and AccessMind `.dat`/`.hdr` packages, but playback is disabled. The Vocalizer library recovered from MultiTTS checks its host app license/signature and returns `JNI_ERR` in Smart Voice. A Vocalizer SDK/runtime authorized for Smart Voice is required; the voice data cannot fix this. See [Vocalizer format research](docs/VOCALIZER_FORMATS.md).

After the first successful import, Smart Voice automatically selects a voice if there is no valid current selection. The home screen then shows that voice, its locale and engine, and the control to choose another voice. The preview requests the exact selected voice from Smart Voice's TTS service; it does not intentionally fall back to the phone's default engine.

Google Local TTS requires an ARM64 Android device. On other ABIs, imports for that native engine are rejected with an architecture error. Microsoft uses the ABIs supplied by its packaged SDK.

The APK contains engine code and native libraries only; no voice models or voice ZIPs are embedded. Imported/downloaded model files are stored in the app's private data directory. The AccessMind catalog contains voice download links, but the linked models are not a free-standing Android engine. The vendor currently describes its Runa TTS product as Windows/NVDA software, with local voice operation after installation and activation. Smart Voice does not bypass or replace that licensing/activation system.

## Build

The project uses Gradle 8.9, Android Gradle Plugin 8.7.3, Java 17, `minSdk 26`, and `targetSdk 35`.

```sh
gradle --no-daemon assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The checked-in release copy is `releases/SmartVoice-0.0.1-debug.apk`.

## Validation

The APK was compiled from this source. TTS discovery is declared for Android's engine list, and the in-app preview explicitly requests the Smart Voice package and selected imported voice. Unexpected Java/JNI linkage errors are recorded; Android 11+ process-exit traces are surfaced on the next launch after a crash. I installed Android emulator images, but this x86_64 host cannot run the ARM64 image needed by Google, and the software x86_64 emulator could not be controlled because ADB requires a home-directory path that is read-only in this environment. Engine listing and actual playback still need verification on a device. A successful build does not confirm runtime playback, and a fatal native signal cannot be intercepted by Java while it occurs.

## Project files

- `app/src/main/java/` — application UI, importer, Android TTS service, and synthesis adapters.
- `app/src/main/assets/emoji/` — Unicode CLDR annotation data and its license.
- `data/voice-packages/` — example Microsoft and Google ZIPs used for local development.
- `releases/` — installable debug APK.
- `CHANGELOG.md` — changes and fixes by version.

See [CHANGELOG.md](CHANGELOG.md) for this release's work.
