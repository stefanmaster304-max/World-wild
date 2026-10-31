# Dangerous World!

Código-fonte do jogo **Dangerous World!**, atualizado para NVGT 0.89.1-beta. Este diretório reúne o código e os dados de configuração versionáveis.

## Áudio

A espacialização usa o `sound_pool` nativo do NVGT, com HRTF global ativo. O áudio de menu também mantém HRTF ligado.

O jogo carrega os sons diretamente das subpastas categorizadas de `sounds/` (por exemplo, `sounds/player/`, `sounds/weapons/`, `sounds/map platforms/`, `sounds/Musics/` e `sounds/anbients/`). O pacote de áudio não está incluído neste repositório; coloque a pasta `sounds/` fornecida com o jogo ao lado de `dw.nvgt` ou do executável. O código não exige que os sons sejam reunidos em um arquivo único.

## Dados locais

Preferências e progresso ficam no diretório de dados do usuário informado pelo NVGT, dentro de `StefanGames/Dangerous World`. As opções são gravadas em `settings.dat`; os dados do jogador — mapa e posição atuais, nível, experiência, eliminações, saúde, inventário, arma equipada, escudo e mapas descobertos — ficam em `game_save.dat`. O jogo cria o arquivo de preferências com os valores padrão na primeira execução e salva o progresso automaticamente durante a partida.

## Sistemas retirados

Foram removidos os sistemas de fome e sede, coleta de recursos do piso, mãos esquerda/direita, NPCs, calendário, mensagens diárias e o atualizador incompatível com NVGT 0.89.1-beta. Itens no chão são recolhidos automaticamente quando o jogador se aproxima. O inventário rápido percorre os itens com Tab e Shift+Tab; Espaço pula, Alt+Enter interage com portas e pontos de viagem, e Enter abre lojas próximas.

## Compilação

Com o compilador NVGT 0.89.1-beta instalado, execute na raiz desta pasta:

```text
nvgt -c dw.nvgt
```

A compilação de release para Linux foi validada. A compilação Windows permanece sem validação neste ambiente.
