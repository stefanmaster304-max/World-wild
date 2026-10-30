# Smart Voice for Android

Smart Voice is an Android text-to-speech engine for screen-reader users. Version `0.0.1` is written in Java and targets Android 8.0 (API 26) or newer. Stefan is the developer.

## Included application features

- Welcome screen and interface translations in English, Portuguese, and Spanish.
- Import compatible ZIP files from the app, or open a `.zip` in a file manager and choose Smart Voice. The app receives the URI through Android's `ACTION_VIEW` intent and opens the cancellable import flow.
- Voice selection, preview speech, and independent volume, pitch, and rate controls.
- Emoji names from bundled Unicode CLDR annotation tables. The selected voice's locale is used; English annotations seed the table so individual missing translations fall back to English.
- User dictionary with add, edit, delete, clear, TXT/CFG import, and TXT export, all grouped on the dictionary screen.
- System text-to-speech settings shortcut, interface-language selection, and a Telegram contact button for [@damomgamer](https://t.me/damomgamer).
- An online voice catalog backed by AccessMind's public `voices.json` endpoint. Selecting an entry downloads its ZIP from the AccessMind file host and sends it through the same validator used by local imports. Downloads require internet access; speech generation remains offline after an engine and compatible voice data are installed.

The importer checks the ZIP structure before registering data. Microsoft packages require their descriptor, model directory, `.APM` model, and `.INI` metadata. Google packages require a descriptor and the selected model's `pipeline.pb` and `backend_pipeline.textproto`. AccessMind Vocalizer packages are parsed from their XML `.hdr` metadata and `.dat` model files. ZIP path traversal, oversized archives, invalid descriptors, missing models, and unknown engines are rejected with an explanation.

## Engine and package status

- **Microsoft Embedded Speech:** MultiTTS-format offline model packages are recognized by the existing adapter. Microsoft neural cloud voices are not enabled.
- **Google local:** MultiTTS-format local model packages are recognized by the local Google adapter. The bundled native runtime is ARM64 only.
- **Vocalizer / Cerence:** The app includes the ARM64 Vocalizer JNI library and a Java runtime bridge recovered from the MultiTTS APK. Its importer reads MultiTTS `config.yaml` packages and AccessMind `.dat`/`.hdr` packages, then arranges AccessMind's split model files for the runtime. Synthesis is wired into the Android TTS service. Compilation confirms that the integration builds; a physical ARM64 device test is still needed to confirm model loading and audible output for each package family. The MultiTTS runtime library is ARM64 only. See [Vocalizer format research](docs/VOCALIZER_FORMATS.md).

AccessMind's catalog contains voice download links, but the linked voice models are not a free-standing Android engine. The vendor currently describes its Runa TTS product as Windows/NVDA software, with local voice operation after installation and activation. Smart Voice does not bypass or replace that licensing/activation system.

## Build

The project uses Gradle 8.9, Android Gradle Plugin 8.7.3, Java 17, `minSdk 26`, and `targetSdk 35`.

```sh
gradle --no-daemon assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The checked-in release copy is `releases/SmartVoice-0.0.1-debug.apk`.

## Validation

The APK was compiled from this source. There is no connected Android device in this workspace, so Vocalizer model loading/audio output and Android's external ZIP chooser still need an on-device check. A successful build does not confirm runtime playback.

## Project files

- `app/src/main/java/` — application UI, importer, Android TTS service, and synthesis adapters.
- `app/src/main/assets/emoji/` — Unicode CLDR annotation data and its license.
- `data/voice-packages/` — example Microsoft and Google ZIPs used for local development.
- `releases/` — installable debug APK.
- `CHANGELOG.md` — changes and fixes by version.

See [CHANGELOG.md](CHANGELOG.md) for this release's work.
