# Dangerous World!

Código-fonte do jogo **Dangerous World!**, atualizado para NVGT 0.89.1-beta. Este diretório reúne o código e os dados de configuração versionáveis.

## Áudio

A espacialização usa o `sound_pool` nativo do NVGT, com HRTF global ativo. O áudio de menu também mantém HRTF ligado.

## Sistemas retirados

Foram removidos os sistemas de fome e sede, coleta de recursos do piso, mãos esquerda/direita, NPCs, calendário, mensagens diárias e o atualizador incompatível com NVGT 0.89.1-beta. Itens no chão são recolhidos automaticamente quando o jogador se aproxima. O inventário rápido percorre os itens com Tab e Shift+Tab; Espaço pula, Alt+Enter interage com portas e pontos de viagem, e Enter abre lojas próximas.

## Compilação

Com o compilador NVGT 0.89.1-beta instalado, execute na raiz desta pasta:

```text
nvgt -c dw.nvgt
```

A compilação de release para Linux foi validada. A compilação Windows permanece sem validação neste ambiente.
