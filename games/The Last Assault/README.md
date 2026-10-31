# The Last Assault

**Versão:** 0.0.1

**Desenvolvimento e conversão:** Stefan

**Tecnologia:** NVGT 0.89.1 beta oficial

**Áudio:** BASS do NVGT com Steam Audio/Phonon e HRTF global para sons posicionais

The Last Assault é um jogo de ação sonora em um mapa 3D baseado em coordenadas e exploração por teclado. Esta edição contém a conversão do código BGT original para NVGT. O jogo é um projeto fora do site; os arquivos estão em `games/`, fora da pasta publicada pelo GitHub Pages (`docs/`).

Os mapas são carregados exclusivamente de arquivos `.map` na pasta `maps/`. O mapa inicial é `maps/main.map`. O formato segue o usado pelo `shooter`: cabeçalho `map:` com limites em linhas separadas `minx:`, `maxx:`, `miny:`, `maxy:`, `minz:` e `maxz:`, seguido por plataformas, zonas, sons e pontos de viagem.

## Controles

- Setas: mover-se pelo mapa.
- Alt+Enter: usar um ponto de viagem quando o personagem estiver dentro dele.
- Alt+B: abrir o menu de construção durante a partida. O mundo continua avançando enquanto o menu está aberto; Tab e Shift+Tab percorrem as opções.
- Alt+Seta para cima/baixo: aumentar ou reduzir o volume da música do mapa.
- Home/End: aumentar ou reduzir o volume mestre do jogo.
- Page Up/Page Down nos menus: aumentar ou reduzir o volume da música do menu.
- Espaço: saltar.
- Teclas 1, 2 e 3: selecionar armas (a terceira é liberada a partir do nível 2).
- Ctrl esquerdo ou direito: disparar a arma selecionada.
- R: recarregar.
- A: consultar munição.
- H: consultar saúde.
- Tab e Shift+Tab: percorrer o inventário rápido.
- Enter: interagir; Shift+Enter: usar o item selecionado.
- I: abrir o inventário.
- Esc: voltar ao menu principal.

O menu principal oferece **Play**, **Documentation**, **Settings** e **Exit**. A Documentação permite escolher o idioma do documento (inglês ou português) e abrir o README ou o changelog em um campo multilinha somente leitura de um formulário de áudio NVGT. As Configurações também usam um formulário de áudio nativo, navegável com Tab e Shift+Tab, com lista de idioma e caixa de seleção para navegação circular dos menus. O jogo oferece inglês (padrão), português, espanhol, turco e vietnamita; os textos são carregados de `languages/*.lang`.

O menu de construção é aberto durante a partida com Alt+B. Ele permite criar mapas, editar ou excluir registros do mapa atual e adicionar plataformas, zonas, zonas seguras, pontos de viagem manuais, fontes sonoras, sons ambientes e escadas. O editor de linhas inclui adicionar, editar, excluir, copiar/colar, copiar todos os registros e renomear o mapa. O cabeçalho `map` e os seis limites são protegidos. O mundo continua sendo atualizado enquanto os formulários estão abertos. A troca ou criação de mapa limpa os dados e os sons do mapa anterior, posiciona o jogador no destino e inicia apenas os sons definidos no novo mapa; mapas sem áudio permanecem silenciosos.

Ao selecionar **Play**, o jogo carrega o mapa da pasta `maps/`, restaura uma posição salva somente se ela estiver dentro dos limites desse mapa e anuncia o nome da zona atual; a plataforma do piso não é usada como nome de localização. A pistola e sua munição são preparadas automaticamente no início da partida. A saúde persistida é recuperada para um valor válido após uma morte.

O idioma, as preferências e os dados do jogador são salvos pelo NVGT na pasta de dados do aplicativo. No Windows, a localização fica em `%AppData%/StefanGames/The Last Assault/`; o arquivo de configurações e progresso é `settings.ini`.

Cada linha de mapa é um registro com campos separados por `:`. Tipos aceitos:

```text
map: Nome do mapa
minx: 0
maxx: 250
miny: 0
maxy: 250
minz: 0
maxz: 40
minlevel: 1
maxlevel: 0
minresurrection: 0
maxresurrection: 0
platform:minX:maxX:minY:maxY:minZ:maxZ:tipoDePiso
zone:minX:maxX:minY:maxY:minZ:maxZ:nomeDaZona
safezone:minX:maxX:minY:maxY:minZ:maxZ
staircase:minX:maxX:minY:maxY:minZ:maxZ:tipoDePiso
source:minX:maxX:minY:maxY:minZ:maxZ:arquivo.ogg:volume:tonalidade
ambience:minX:maxX:minY:maxY:minZ:maxZ:arquivo.ogg:volume:tonalidade
travelpoint:minX:maxX:minY:maxY:minZ:maxZ:mapaDestino:destMinX:destMaxX:destMinY:destMaxY:destMinZ:destMaxZ
```

Os efeitos sonoros fornecidos ficam em `sounds/`. Os formulários e a fala nativos do NVGT estão incluídos localmente em `includes/form.nvgt` e `includes/speech.nvgt`.

Ao adicionar som ambiente, o construtor filtra o catálogo por **Music** ou **Sources**. Os registros usam o formato do `shooter`: `source:` para fonte posicional e `ambience:` para reprodução limitada à área, com arquivo, volume e tonalidade. A categoria de música é identificada pelo caminho do arquivo; os demais arquivos são efeitos ambientais. Volume é linear entre `0.0` e `1.0`; tonalidade entre `50` e `200`. Ambientes são pausados fora da área e retomados quando o jogador retorna; fontes posicionais continuam usando atenuação 3D. Os pontos de viagem são manuais e acionados com Alt+Enter. O carregador ainda aceita os nomes antigos `src` e `ambient` e os registros com categoria explícita. Ao entrar numa zona, o jogo anuncia seu nome; fora das zonas, anuncia que a área é desconhecida. Zonas seguras são áreas sem nome visível onde o jogador não pode disparar.
