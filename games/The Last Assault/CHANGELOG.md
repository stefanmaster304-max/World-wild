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
- Corrigido o travamento ao selecionar **Jogar**: o fade de áudio herdado do BGT tentava alcançar `-50`, valor inválido para o ganho linear `0.0–1.0` do NVGT, e ficava em um loop sem fim antes de chamar `draw_map`.
- O fade agora termina em ganho `0.0` com passos limitados; o volume antigo salvo em decibéis é convertido para a escala NVGT e os controles de volume do menu operam nessa escala.
- Ao iniciar o mapa novamente, as áreas, fontes ambientais e escadas da partida anterior são limpas antes de recriar o mapa original.
- Substituído o mapa embutido no código por arquivos `maps/*.map`, usando o `coordinate_map` nativo do NVGT para consultar plataformas.
- Criados includes separados para zonas e zonas seguras; a zona segura informa sua presença e impede disparos do jogador, ataques dos inimigos e dano de balas contra alvos dentro dela.
- Adicionado o construtor NVGT com criação, edição e exclusão de mapas, plataformas, zonas, zonas seguras, escadas, fontes posicionais, sons ambientes por área e pontos de viagem por Alt+Enter.
- Sons ambientes por área são pausados ao sair e retomados da posição de reprodução ao entrar novamente. Fontes posicionais usam o centro da área como origem espacial.
- O menu permite escolher mapas existentes, e o mapa inicial foi migrado para `maps/main.map`.
- Projeto identificado como conteúdo fora do site GitHub Pages; permanece em `games/`, fora de `docs/`.
- Acesso ao construtor durante o jogo alterado para Alt+B. O formulário usa botões navegáveis por Tab e Shift+Tab.
- A simulação continua atualizando inimigos, projéteis, escadas e áudio espacial enquanto o menu e seus formulários estão abertos.
- Consultas de mapas nomeadas explicitamente como `get_platform_at`, `get_zone_at`, `get_safe_zone_at` e `get_travel_point_at`.
- Alt+Enter verifica o ponto de viagem que contém a posição atual antes de carregar o mapa e as coordenadas de destino.
- O construtor de som filtra o catálogo de ambiente entre música e fontes ambientais, com volume linear e tonalidade por fonte.
- Adicionados controles de volume mestre com Home/End, música do mapa com Alt+Setas e música dos menus com Page Up/Page Down.
- Corrigidos fades e ajustes de volume para usar ganho linear NVGT `0.0–1.0` nas propriedades de som; o controle mestre usa a escala em dB exigida pela API global.
- Pontos de viagem seguem exclusivamente manuais; não foi incorporado o sistema automático de mundo aberto do jogo de comparação.
- Pontos manuais agora aceitam faixas de destino X/Y/Z e calculam a chegada proporcional à posição do jogador dentro da área de origem; formatos antigos de coordenada única continuam sendo lidos.
- O construtor foi conferido contra o fluxo do `shooter`: cada formulário usa ciclos curtos de `wait(5)` e avança inimigos, projéteis, escadas, sons e listener 3D antes de processar entrada, evitando congelar o mundo enquanto se constrói.
- As linhas novas de áudio seguem o formato usado pelo `shooter`: `source` para fontes posicionais e `ambience` para som por área, com arquivo, volume, tonalidade e categoria. O carregador também preserva compatibilidade com as linhas anteriores `src` e `ambient`, além da versão que guardava a categoria antes do nome do arquivo.
- O menu principal foi reorganizado para **Play**, **Documentation**, **Settings** e **Exit**. A documentação abre README e changelog em visualizador multilinha somente leitura.
- A música do menu principal para ao entrar em Configurações ou Documentação e só volta ao retornar ao menu principal.
- Adicionados idioma configurável (inglês padrão, português, espanhol, turco e vietnamita) e preferência de navegação circular nos menus.
- As opções e os campos principais dos menus e do construtor receberam traduções. O idioma escolhido é persistido junto às configurações.
- Criado um catálogo de traduções para os textos fixos da interface, formulários, mensagens e anúncios de teclado nos cinco idiomas. Informações variáveis, como volume, contagem de inimigos, munição, nível e posição no menu, são montadas com formato localizado.
- O editor do mapa foi alinhado ao fluxo do editor do `shooter`: agora permite adicionar, editar, excluir, copiar e colar linhas, copiar todas as linhas e renomear o mapa. A edição continua protegendo `name` e `bounds`, valida os registros de mapa reconhecidos e atualiza o mundo durante cada formulário.
- Nomes e contagens apresentados pelo inventário, armas disponíveis, prompts de entrada e avisos de HRTF também passam pelo catálogo do idioma selecionado.
- As configurações e os dados de jogador são gravados pela API `settings` do NVGT em `%AppData%/StefanGames/The Last Assault/settings.ini` no Windows. São persistidos nível, experiência, saúde, mapa e coordenadas, além das configurações.
- Ao entrar em uma zona normal, o jogo anuncia “Current location” e o nome; ao sair para uma área sem zona, anuncia “Area unknown”. A tecla B anuncia a mesma informação sob demanda.
- O formulário de zona segura solicita apenas as coordenadas; ao tentar disparar nela, o jogo informa que armas não podem ser usadas naquela área.

### Validação

- Compilação Linux: concluída com sucesso usando NVGT 0.89.1 beta.
- Compilação para Windows: concluída com sucesso usando o compilador Linux com alvo `--platform=windows`.
- Tentativa de iniciar o compilador Windows sob Wine: o Wine disponível abortou antes de executar o compilador (`free(): invalid pointer`). Portanto, o artefato Windows foi compilado, mas sua execução sob Wine não foi validada neste ambiente.
