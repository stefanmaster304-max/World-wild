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

The repository's MultiTTS Vocalizer ZIPs use a different package wrapper. They contain `config.yaml` and `engines.yaml`; each voice has a directory containing a `.dat` model, a `.hdr` pipeline description, an encrypted `voice_embedded_*.xml` resource, and shared files such as `clm.dat`, `lid.dat`, `msp.dat`, `sysdct.dat`, and the synthesis dictionary. The examples inspected include `apps/voices/vocalizer+3_pt-BR.zip` and `apps/voices/12 voces en español de Vocalizer @TutorialesParaCiegos.zip`.

Both sources use Vocalizer Embedded model/header conventions and 22,050 Hz output metadata, but their ZIP layouts and model releases are not identical. AccessMind compact `.dat`/`.hdr` pairs do not contain the MultiTTS YAML descriptors or its common voice support files. They need a package interpreter and validation, and the matching Android runtime must confirm that it can load each operating point and version. Similar names or headers alone do not prove binary/runtime compatibility.

## Runtime and licensing boundary

AccessMind's download site offers Runa TTS voice data; AccessMind and its distributor describe the current product as Windows/NVDA software and note that local voice use follows installation and activation. Those downloads are not an Android SDK or a general redistribution grant. Smart Voice does not bypass AccessMind/Cerence activation or treat the Windows runtime as an Android runtime.

The repository's MultiTTS APK contains `lib/arm64-v8a/libNuanceVocalizer.so` and `libc++_shared.so`. Smart Voice now bundles those ARM64 libraries and includes the recovered Java JNI wrapper, runtime initialization, voice selection, and PCM callback path in its Android TTS service. AccessMind archives are normalized by placing each matching `.dat` model beside its `.hdr` descriptor in the runtime voice folder. The source compiles, but the JNI runtime has not yet been exercised on an Android device in this workspace. Package parsing and successful compilation cannot establish that every model release or operating point will load and speak correctly; verify supported package variants on ARM64 hardware.

## Sources

- [AccessMind Runa TTS voice download page](https://accessmind.io/runa-tts.php)
- [AccessMind public voice catalog JSON](https://accessmind.io/voices.json)
- [AccessMind's Runa TTS download page](https://accessmind.co.il/downloads/runa-tts/)
- [Runa TTS Voices for NVDA product and compatibility details](https://www.orbitresearch.com/products/runa-tts/runa-tts-voices-for-nvda/)
- [MultiTTS reverse-engineering repository provided for analysis](https://github.com/xiyuqiangshou/multitts-reverse)
