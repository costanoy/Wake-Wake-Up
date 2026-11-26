# Arte de divulgação

Peças que NÃO entram no APK nem no site. Confira os direitos de uso antes de publicar.

| Arquivo | Tamanho | Onde entra | Ferramenta e plano | Link do design |
|---|---|---|---|---|
| `play-store/play-icone-512.png` | 512x512, sem transparência | Ícone da loja (Play Console) | ffmpeg local, recorte de `app/src/main/res/drawable-nodpi/ic_launcher_foreground_art.png` (crop 296x296 em 68,68, dentro dos cantos arredondados da arte, escala 512). Sem Canva. | não se aplica |
| `play-store/play-destaque-pt.png` | 1024x500 | Gráfico de destaque (PT) | PENDENTE. Tentativa no Canva (plano da conta conectada ao MCP) não aprovada: ver abaixo. | rascunho descartável: https://www.canva.com/d/3hACPL6Kr0GQ7-s |
| `play-store/play-destaque-en.png` | 1024x500 | Gráfico de destaque (EN) | PENDENTE | não criado |

## Por que o gráfico de destaque ficou pendente

O conector do Canva só deixa escolher o texto, o tamanho e a cor, não a família da fonte, e não confirma qual fonte usou (a referência interna é `YAFdJvSyp_k`). O guia manda parar se não houver Instrument Sans garantida. Além disso, a tentativa saiu em 1264x1264 (o formato 1024x500 foi ignorado), com fundo em imagem do Canva e cores diferentes do guia (`#F5E9DB` e `#A6907C` no lugar de `#F6ECE0` e `#B49E88`). O rascunho não deve ser usado.

Recorte do relógio já preparado (de `site/og-image.png`, 612x226 a partir de 294,90) e enviado ao Canva como mídia `MAHcCPEJGho`. Caminho local de trabalho: pasta temporária, refazer com `ffmpeg -i site/og-image.png -vf "crop=612:226:294:90"`. Sugestão: montar as duas peças no Canva manualmente (escolhendo Instrument Sans) ou localmente com ffmpeg/HTML se a fonte estiver instalada.
