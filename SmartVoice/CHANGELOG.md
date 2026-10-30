# Smart Voice change log

## 0.0.1 — 2026-10-30

### Engine and import reliability fixes

- Wrapped the full Android TTS request path so unexpected Java and JNI linkage failures are recorded and returned through Android's synthesis error callback instead of escaping from the service.
- On Android 11 and newer, the next app launch now offers a copyable report from Android's historical process-exit trace when Smart Voice previously ended in a Java or native crash. Reports from before the currently installed app update are ignored.
- Added the required `DEFAULT` service category and engine settings metadata so Android can discover Smart Voice as a TTS service.
- Removed an invalid check against Android's global default TTS engine. The preview now selects the exact imported voice exposed by the explicitly requested Smart Voice TTS service and verifies that Android accepted the same voice ID before speaking.
- Recorded synthesis exceptions and added copy/share actions to error dialogs.
- Added the Access Mind Server chooser and selectable language, gender, and quality filters; voice rows use the server's published metadata.
- Added download progress/cancellation, followed by the normal secure extraction and package validation path, and a completion message after successful import.
- Added an ARM64 compatibility check for the Google and Vocalizer native runtimes. The APK bundles engine libraries only; user voice models remain external imports/downloads.
- Made the first imported/downloaded voice the default when no valid selection exists, and show the selected voice, language, and engine on the home screen.
- Removed the catalog's text search field. The server catalog now uses only language, gender, and quality selectors, with the filtered result count, and Back returns from the catalog to server selection.
- Removed the redundant version line from the home screen and updated the welcome text to name the three integrated engines and their supported package sources.

Runtime validation limit: the ARM64 system image cannot run on this x86_64 host, and ADB for the x86_64 emulator is blocked by the sandbox's read-only home directory. No claim of device playback validation is made.

Crash-reporting limit: the app can surface Java/native process traces on Android 11+, but it cannot catch a fatal native signal inside a voice library while that signal is occurring. Actual audio playback and the reported crash cause still need confirmation on an Android device.

### Added

- Initial Smart Voice Android app foundation, version `0.0.1`, with welcome flow and English, Portuguese, and Spanish interface text.
- Local Microsoft and Google speech-engine support, voice-package storage/selection, voice preview, and independent volume, pitch, and rate controls.
- Emoji name table, plus user dictionary management with add/edit/delete, TXT/CFG import, TXT export, and clear operations.
- Android ZIP association so file managers can offer **Import to Smart Voice** for ZIP packages. Opening a package starts the normal cancellable import flow.
- Online voice catalog backed by AccessMind's public voice catalog API, with selectable voice variants, package download, progress indication, and cancellation.
- AccessMind Vocalizer package metadata reader for voice `.hdr` XML plus `.dat` model archives.
- ARM64 Vocalizer synthesis integration using the `libNuanceVocalizer.so` and C++ runtime library found in the repository's MultiTTS APK, with a Java JNI bridge, PCM callback, and Android TTS service routing.
- Direct Telegram contact button linking to `https://t.me/damomgamer`.
- Vocalizer format research notes based on repository samples and three official AccessMind package downloads in Brazilian Portuguese, American English, and Spanish.

### Changed

- Dictionary add/edit/delete/import/export/clear actions are grouped on the dictionary screen.
- Removed the Accessibility settings shortcut. The settings screen keeps Android's text-to-speech settings shortcut.
- Removed the repeated current-voice/status text from the home screen.
- Emoji normalization now starts from English CLDR names and overlays the active voice locale, so an individual missing translation falls back to English.
- Package validation reports unsupported engines and missing engine-specific data instead of silently accepting an incomplete archive.

### Compatibility and validation note

- The APK compiles with the Vocalizer runtime and both MultiTTS and AccessMind import paths. The native Vocalizer library and local Google runtime are ARM64 only.
- No Android device is connected to this workspace. Vocalizer model loading and playback, along with Android's external ZIP chooser, still require a physical-device check. A successful build alone does not prove audio playback.
- Microsoft, Google, and Vocalizer output remain subject to on-device playback validation. Microsoft cloud/neural voices are not enabled.
