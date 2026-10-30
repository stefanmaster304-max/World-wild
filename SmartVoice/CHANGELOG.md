# Smart Voice change log

## 0.0.1 — engine selection update — 2026-10-30

- Removed Google TTS synthesis from the app: deleted its native adapter, JNI library, dependency JAR, and Smart Voice sample ZIPs. Google package imports are rejected and old Google registry entries are hidden.
- Fixed Microsoft voice selection to use the SDK voice name stored in the first line of MultiTTS `param` (for example, `Microsoft Maria`) instead of the package/model ID (`pt-BR-Maria-Apollo`). The license on the next line is now separated and normalized to the `Key:` form used by the reference adapter.
- Persisted the SDK voice name separately so existing Microsoft package registrations can be upgraded from their stored parameter block.
- Updated the welcome text and README to list the currently supported synthesis engine accurately.
- The reported Maria error is consistent with sending the package ID as the SDK voice name. A fresh build is required to verify the corrected name and license on the device.

## 0.0.1 — Access Mind Vocalizer follow-up — 2026-10-30

- Extracted the newly added Code Factory Vocalizer Voices APK for static analysis. It contains ARM64 and ARMv7 `libNuanceVocalizer.so`, `libsecure.so`, `libsp.so`, and `libc++_shared.so`, plus a catalog with compact, pro, high, and premium operating points.
- The APK's `AndroidManifest.xml` and primary `classes.dex` fail ZIP CRC validation; its native libraries and catalog could be extracted, but it is not a reliable install/runtime test artifact. Static inspection confirms the operating-point metadata and native engine path, not end-to-end loading of Access Mind downloads.
- Restricted Vocalizer imports to the Access Mind `speech/ve` and `speech/components` package structure. Legacy MultiTTS Vocalizer descriptor ZIPs are rejected, and previously registered Vocalizer voices without Access Mind provenance are hidden.
- Stored the Access Mind origin and native operating-point identifier separately from the user-facing quality. Vocalizer voice labels now show localized Low/Medium/High/Premium instead of internal strings such as `embedded_compact`.
- Kept playback disabled because the reference APK integrates its own license service and does not provide Smart Voice redistribution/use authorization; no app identity or license checks are bypassed.

## 0.0.1 — speech-engine follow-up — 2026-10-30

- Google speech parameters now use the normalized rate and pitch scaling found in the MultiTTS adapter instead of sending out-of-range values. The composer now has a bounded wait, reports terminal states, and streams PCM in callback-sized chunks so a stalled synthesis returns a diagnostic instead of waiting forever.
- Microsoft synthesis cancellation now records the SDK's reason, error code, and error details instead of returning only Android error `-1`.
- Vocalizer preview no longer calls the MultiTTS native library that rejected Smart Voice with `JNI_ERR`. That binary checks its licensed host identity; playback requires a Vocalizer runtime authorized for Smart Voice. The importer remains able to recognize the package formats, but does not claim they can synthesize with this library.
- Catalog filter accessibility labels now follow the selected item, preventing TalkBack from continuing to announce “All languages” after a language is chosen.
- Corrected the welcome screen, README, and Vocalizer format notes to describe the actual runtime restriction.

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
