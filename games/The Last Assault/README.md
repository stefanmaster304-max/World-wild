# The Last Assault

**Versão:** 0.0.1

**Desenvolvimento e conversão:** Stefan

**Tecnologia:** NVGT 0.89.1 beta oficial

**Áudio:** BASS do NVGT com Steam Audio/Phonon e HRTF global para sons posicionais

The Last Assault é um jogo de ação sonora em um mapa 3D baseado em coordenadas e exploração por teclado. Esta edição contém a conversão do código BGT original para NVGT. O jogo é um projeto fora do site; os arquivos estão em `games/`, fora da pasta publicada pelo GitHub Pages (`docs/`).

Os mapas são carregados de arquivos `.map` na pasta `maps/`. O mapa inicial é `maps/main.map`. O carregador suporta limites, plataformas, zonas, zonas seguras, escadas, fontes sonoras posicionais, sons ambientes por área e pontos de viagem.

## Controles

- Setas: mover-se pelo mapa.
- Alt+Enter: usar um ponto de viagem quando o personagem estiver dentro dele.
- Alt+B: abrir o menu de construção durante a partida. O mundo continua avançando enquanto o menu está aberto; Tab e Shift+Tab percorrem as opções.
- Alt+Seta para cima/baixo: aumentar ou reduzir o volume da música do mapa.
- Home/End: aumentar ou reduzir o volume mestre do jogo.
- Page Up/Page Down nos menus: aumentar ou reduzir o volume da música do menu.
- Espaço: saltar.
- Teclas 1, 2 e 3: selecionar armas (a terceira é liberada a partir do nível 2).
- Teclas de disparo: atacar.
- R: recarregar.
- A: consultar munição.
- H: consultar saúde.
- Tab e Shift+Tab: percorrer o inventário rápido.
- Enter: interagir; Shift+Enter: usar o item selecionado.
- I: abrir o inventário.
- Esc: voltar ao menu principal.

O menu principal oferece **Play**, **Documentation**, **Settings** e **Exit**. A Documentação abre o README ou o changelog em um campo multilinha somente leitura. As Configurações permitem escolher inglês (padrão), português, espanhol, turco ou vietnamita e ativar ou desativar a navegação circular dos menus. Menus, opções, formulários, mensagens, avisos e anúncios de teclado usam o idioma selecionado.

O menu de construção é aberto durante a partida com Alt+B. Ele permite criar mapas, editar ou excluir registros do mapa atual e adicionar plataformas, zonas, zonas seguras, pontos de viagem manuais, fontes sonoras, sons ambientes e escadas. O editor de linhas inclui adicionar, editar, excluir, copiar/colar, copiar todos os registros e renomear o mapa. Os campos `name` e `bounds` são protegidos. O mundo continua sendo atualizado enquanto os formulários estão abertos.

O idioma, as preferências e os dados do jogador são salvos pelo NVGT na pasta de dados do aplicativo. No Windows, a localização fica em `%AppData%/StefanGames/The Last Assault/`; o arquivo de configurações e progresso é `settings.ini`.

Cada linha de mapa é um registro com campos separados por `:`. Tipos aceitos:

```text
name:Nome do mapa
bounds:minX:maxX:minY:maxY:minZ:maxZ
platform:minX:maxX:minY:maxY:minZ:maxZ:tipoDePiso
zone:minX:maxX:minY:maxY:minZ:maxZ:nomeDaZona
safezone:minX:maxX:minY:maxY:minZ:maxZ
staircase:minX:maxX:minY:maxY:minZ:maxZ:tipoDePiso
source:minX:maxX:minY:maxY:minZ:maxZ:arquivo.ogg:volume:tonalidade:music|sources
ambience:minX:maxX:minY:maxY:minZ:maxZ:arquivo.ogg:volume:tonalidade:music|sources
travelpoint:minX:maxX:minY:maxY:minZ:maxZ:mapaDestino:destMinX:destMaxX:destMinY:destMaxY:destMinZ:destMaxZ
```

Os efeitos sonoros fornecidos ficam em `sounds/`.

Ao adicionar som ambiente, o construtor filtra o catálogo por **Music** ou **Sources**. Os registros usam o padrão de linhas do mapa do `shooter`: `source:` para a fonte sonora posicional e `ambience:` para o áudio por área. A linha mantém arquivo e parâmetros básicos em sequência e acrescenta a categoria no final para distinguir música de efeitos. Volume é linear entre `0.0` e `1.0`; tonalidade entre `50` e `200`. Sons ambientes ficam pausados fora da área e retomam de onde pararam quando o jogador retorna. Os pontos de viagem são manuais e acionados com Alt+Enter. O carregador aceita registros antigos `src`, `ambient` e os registros de áudio com categoria antes do arquivo. Ao entrar numa zona, o jogo anuncia a localização e o nome; fora de todas as zonas, anuncia que a área é desconhecida. Zonas seguras são áreas sem nome visível onde o jogador não pode disparar.
