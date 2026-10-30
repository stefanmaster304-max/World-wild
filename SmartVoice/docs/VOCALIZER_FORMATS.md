# Vocalizer package research

Research performed on 2026-10-30. AccessMind's live catalog is at [accessmind.io/runa-tts.php](https://accessmind.io/runa-tts.php); its page loads the downloadable entries from the public [`voices.json`](https://accessmind.io/voices.json) catalog. The catalog lists voice name, language, gender, quality, size, and a ZIP download URL under `files.accessmind.io/ve-voices/`.

## Sample AccessMind archives inspected

Three small `Lowest`/`compact` archives were downloaded from those official links for format inspection only. They were kept in temporary workspace storage, not copied into this repository.

| Sample | Download size | Archive contents |
| --- | ---: | --- |
| Luciana, Brazilian Portuguese, compact 1.2.3 | 595,368 bytes | `ptb/speech/components/ptb_luciana_embedded-compact_1-2-3.dat` and `ptb/speech/ve/ve_pipeline_ptb_luciana_22_embedded-compact_1-2-3.hdr` |
| Ava, American English, compact 3.4.0 | 4,053,073 bytes (the downloaded archive contains a 5,593,190-byte model) | `enu/speech/components/enu_ava_embedded-compact_3-4-0.dat` and `enu/speech/ve/ve_pipeline_enu_ava_22_embedded-compact_3-4-0.hdr` |
| Monica, Spanish, compact 1.2.4 | 730,263 bytes | `spe/speech/components/spe_monica_embedded-compact_1-2-4.dat` and `spe/speech/ve/ve_pipeline_spe_monica_22_embedded-compact_1-2-4.hdr` |

The exact size for Ava's ZIP returned by the catalog is 4,053,073 bytes. The model inside the ZIP is larger after decompression.

All three AccessMind `.hdr` files are readable XML. They include fields such as `langietf`, `voice`, `voiceversion`, `vopversion`, `frequencyhz`, and `voiceoperatingpoint`. The compact model data is stored separately under `speech/components`; the corresponding pipeline metadata is under `speech/ve`.

## MultiTTS archives in this repository

The repository's MultiTTS Vocalizer ZIPs use a different package wrapper. They contain `config.yaml` and `engines.yaml`; each voice has a directory containing a `.dat` model, a `.hdr` pipeline description, an encrypted `voice_embedded_*.xml` resource, and shared files such as `clm.dat`, `lid.dat`, `msp.dat`, `sysdct.dat`, and the synthesis dictionary. The examples inspected include `apps/voices/vocalizer+3_pt-BR.zip` and `apps/voices/12 voces en español de Vocalizer @TutorialesParaCiegos.zip`. Smart Voice now rejects this package family and hides prior registry entries that do not explicitly identify Access Mind as their source.

Both sources use Vocalizer Embedded model/header conventions and 22,050 Hz output metadata, but their ZIP layouts and model releases are not identical. AccessMind compact `.dat`/`.hdr` pairs do not contain the MultiTTS YAML descriptors or its common voice support files. They need a package interpreter and validation, and the matching Android runtime must confirm that it can load each operating point and version. Similar names or headers alone do not prove binary/runtime compatibility.

## Runtime and licensing boundary

AccessMind's download site offers Runa TTS voice data; AccessMind and its distributor describe the current product as Windows/NVDA software and note that local voice use follows installation and activation. Those downloads are not an Android SDK or a general redistribution grant. Smart Voice does not bypass AccessMind/Cerence activation or treat the Windows runtime as an Android runtime.

The repository now also contains `apps/Vocalizer Voices.apk` (package `es.codefactory.vocalizertts`, version 4.2.0). Its ARM64 and ARMv7 libraries include `libNuanceVocalizer.so`, `libsecure.so`, `libsp.so`, and `libc++_shared.so`; its `voice_list.xml` lists `embedded_compact`, `embedded_pro`, `embedded_high`, and `embedded_premium` operating points. Static code inspection shows that the app parses `.hdr` pipeline metadata, maps those operating-point IDs, and asks the native engine to load a selected model from its installed voice directory. This is evidence that the data format belongs to the Vocalizer Embedded family; it does not prove that Access Mind archives load end-to-end in this Android host. The app also uses a license service, and the repository does not provide authorization to redistribute or use these libraries in Smart Voice.

The new APK's ZIP entries for `AndroidManifest.xml` and `classes.dex` fail CRC validation, although `aapt` can read its package metadata and the native libraries/catalog can be extracted. JADX completed with errors. Consequently, the APK is useful for static reference, but it is not a reliable runtime test artifact. The older MultiTTS library separately returned `JNI_ERR` during Smart Voice's `JNI_OnLoad`; no license or signature check is bypassed here. Actual playback requires a Vocalizer runtime licensed for Smart Voice.

Smart Voice now accepts Vocalizer archives only when their headers and model data follow the Access Mind `speech/ve` plus `speech/components` layout. It stores the normalized user-facing quality (`low`, `medium`, `high`, or `premium`) separately from the internal operating-point identifier (`embedded-compact`, `embedded-pro`, `embedded-high`, or `embedded-premium`) so the voice chooser does not expose internal filenames/IDs as quality labels.

## Sources

- [AccessMind Runa TTS voice download page](https://accessmind.io/runa-tts.php)
- [AccessMind public voice catalog JSON](https://accessmind.io/voices.json)
- [AccessMind's Runa TTS download page](https://accessmind.co.il/downloads/runa-tts/)
- [Runa TTS Voices for NVDA product and compatibility details](https://www.orbitresearch.com/products/runa-tts/runa-tts-voices-for-nvda/)
- [MultiTTS reverse-engineering repository provided for analysis](https://github.com/xiyuqiangshou/multitts-reverse)
