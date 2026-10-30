# Histórico de alterações

## 0.0.1 — Conversão BGT para NVGT

- Fontes do jogo convertidos de BGT para NVGT e organizados como `.nvgt`; os arquivos-fontes BGT foram removidos da pasta de trabalho.
- Conversão feita com o NVGT **0.89.1 beta oficial**, da série 0.89, conforme a versão escolhida para este projeto.
- Nome atualizado para **The Last Assault**, versão **0.0.1**, desenvolvimento/conversão por Stefan.
- Includes adaptados para NVGT, incluindo uso de handles de som e chamadas de `length()` compatíveis com a API.
- Áudio usa o sistema legado BASS incluído no NVGT 0.89.1 beta; posicionamento espacial usa `sound.set_position()` do NVGT e os OGG fornecidos em `sounds/`.
- HRTF global do Steam Audio/Phonon é habilitado a cada inicialização por `set_sound_global_hrtf(true)`. O pacote inclui a biblioteca Phonon usada pelo processador.
- Removida a configuração do pacote criptografado `sounds.dat`, ausente na origem.
- Removido o som `store.ogg` ausente no pacote recebido, para evitar tentativa contínua de abrir um arquivo inexistente.
- A ajuda no menu agora lê `README.md`.
- Projeto identificado como conteúdo fora do site GitHub Pages; permanece em `games/`, fora de `docs/`.

### Validação

- Compilação Linux: concluída com sucesso usando NVGT 0.89.1 beta.
- Compilação para Windows: concluída com sucesso usando o compilador Linux com alvo `--platform=windows`.
- Tentativa de iniciar o compilador Windows sob Wine: o Wine disponível abortou antes de executar o compilador (`free(): invalid pointer`). Portanto, o artefato Windows foi compilado, mas sua execução sob Wine não foi validada neste ambiente.
