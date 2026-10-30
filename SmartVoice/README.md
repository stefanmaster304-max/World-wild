# Smart Voice para Android

Smart Voice é a base de código de um aplicativo Android de fala acessível, versão 0.0.1, desenvolvido por Stefan. Neste estado, o projeto está intencionalmente sem mecanismo de síntese de voz: não inclui SDKs, bibliotecas nativas, serviço TTS registrado no Android nem modelos de voz. A aplicação não aparece como sintetizador do sistema e não reproduz fala.

A tela inicial informa que não há motores disponíveis. As telas de boas-vindas, dicionário e configurações permanecem como código do aplicativo para desenvolvimento posterior. A integração de um motor e a ativação dos fluxos de importação, seleção e teste de voz serão feitas quando houver um runtime definido.

Os arquivos de voz não são distribuídos neste repositório. Pacotes de voz e bibliotecas de terceiros devem ser obtidos e distribuídos conforme as respectivas permissões e licenças.

## Compilação

O projeto usa Gradle 8.9, Android Gradle Plugin 8.7.3, Java 17, `minSdk 26` e `targetSdk 35`. A partir desta pasta: `gradle --no-daemon assembleDebug`.

O APK de depuração é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Estrutura

- `app/src/main/java/`: interface e dicionário.
- `CHANGELOG.md`: histórico das alterações.
