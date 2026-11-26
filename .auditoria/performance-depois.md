# Performance depois: Wake Wake Up

Medido em 2026-11-26, branch melhorias-claude, HEAD e737427. Nenhum código alterado e Gradle não foi rodado: usei os artefatos já gerados (APK release, resultados de teste e lint). Limites vindos de `orcamento.md`. Linha de base: `performance-antes.md`.

## Tabela final

| item | antes | depois | limite | dentro? |
|---|---|---|---|---|
| APK release sem assinatura | 4.066.524 B | 4.134.361 B (+1,67%) | até +5% (4.269.850 B) e até 12 MB | **sim** |
| Composição: `classes.dex` | 2.479.656 B | 2.635.860 B (+6,3%) | n/a (conta no total) | n/a |
| Composição: `resources.arsc` | 205.888 B | 218.032 B | n/a | n/a |
| Composição: `lib/` (4 ABIs) | 37.392 B | 37.392 B | n/a | n/a |
| Composição: `res/` (53 entradas) | n/a | 2.634.757 B | n/a | n/a |
| `res/raw` total | 2.105.236 B | 2.105.236 B | até 2,5 MB | **sim** |
| `res/raw` maior arquivo | 300.717 B | 300.717 B | até 400 KB | **sim** |
| `res/font` | 419.212 B, 5 arquivos | 419.212 B, 5 arquivos | até 450 KB e 5 arquivos | **sim** (no limite de arquivos) |
| `drawable-nodpi` (ícone) | 117.515 B, PNG 432 px | 90.774 B, WebP 432 px (lossless) | bitmap só em WebP, até 1024 px | **sim** (-22,8%) |
| `drawable` e `mipmap-anydpi-v26` (XML) | 1.276 B e 696 B | igual | VectorDrawable | **sim** |
| Tempo do `assembleRelease` | 65 s | não medido (Gradle não rodado) | sem limite | n/a |
| Testes JVM: total / falhas | não concluído (0 resultados) | 368 / 0 falhas, 0 erros, 28 ignorados | suíte inteira passa | **sim** |
| Testes JVM: tempo somado | não medido | 68,0 s (49 suítes) | sem limite | n/a |
| Lint: erros | 1 (`NonObservableLocale`) | 0 | sem limite | n/a |
| Lint: warnings | 50 warnings + 1 hint (contagem do XML) | 60 warnings (contagem do texto) | sem limite | n/a (bases diferentes, ver nota) |
| Lighthouse mobile PT: Performance / Acess. / Boas práticas / SEO | 97 / 100 / 100 / 100 | 97 / 100 / 100 / 100 | 90 ou mais | **sim** |
| Lighthouse mobile EN: Performance / Acess. / Boas práticas / SEO | 97 / 100 / 100 / 100 | 97 / 100 / 100 / 100 | 90 ou mais | **sim** |
| Página inicial PT sem vídeo e APK | 225.960 B | 214.759 B | até 500 KB | **sim** |
| Página inicial EN sem vídeo e APK | não medido na linha de base | 214.742 B | até 500 KB | **sim** |
| JavaScript próprio (`main.js` + `lang.js` + `site-config.js`) | 19.361 B | 22.379 B | até 30 KB | **sim** |
| Fontes woff2 na página | 5 | 4 (84.352 B, antes 104.988 B) | no máximo 4 | **sim** |
| `font-display` | swap | swap nas 4 `@font-face` | swap | **sim** |
| Vídeo | `preload="none"` com pôster | igual (PT e EN) | none ou metadata, com pôster | **sim** |
| Imagens acima de 50 KB carregadas pela página | nenhuma | nenhuma | WebP/AVIF acima de 50 KB | **sim** (ver nota sobre og-image) |
| Partida a frio, memória, FPS, bateria | não medido (sem aparelho) | não medido (sem aparelho) | 1,5 s; 150 MB PSS; 60 FPS | não medido |

**Estouros: nenhum.** Os pontos que mais se mexeram estão em "Notas".

## Notas de medição

- **APK:** cresceu 67.837 B (+1,67%), dentro dos 5%. O `classes.dex` cresceu 156 KB (+6,3%), o que pesa mais que o total do APK e vale acompanhar. `res/raw` e `res/font` não mudaram; o único arquivo de imagem trocado foi o ícone (PNG para WebP).
- **Testes:** a suíte agora executa (368 testes, 0 falhas). A linha de base estava incompleta porque a execução falhava com `ClassNotFoundException`. São 28 testes ignorados que não estavam registrados antes; vale conferir se são intencionais. Resultados em `.auditoria/logs/test-results-depois/`.
- **Lint:** o erro `NonObservableLocale` foi corrigido. A linha de base vem da contagem do XML (50 warnings + 1 hint). A medição de agora vem de `lint-depois.txt` (texto), então o +10 não é comparável diretamente. Categorias: 11 `UnusedResources`, 16 `UseKtx`, 11 `GradleDependency`, 4 `NewerVersionAvailable`, 2 `UnusedAttribute`, entre outras.
- **Ícone:** `drawable-nodpi/ic_launcher_foreground_art.webp`, 432x432, conferido pelo cabeçalho WebP.
- **Lighthouse:** a primeira rodada EN foi redirecionada para `/` pelo `lang.js` (mesmo problema da linha de base). Refiz com `--chrome-flags="--headless=new --lang=en-US"`; só a refeita vale. JSON em `.auditoria/logs/lighthouse-depois-pt.json` e `lighthouse-depois-en.json`.
- **og-image:** `site/og-image.png` (185 KB) e `og-image-en.png` (141 KB) passam de 50 KB, mas só entram em `<meta property="og:image">` e não são carregados pela página. Não contam no peso da página.
- **Não medido:** partida a frio, memória ao tocar, FPS e bateria dependem de aparelho. Não há emulador neste PC.

## Lighthouse mobile: top 5 problemas (PT, depois)

Ordem pela economia estimada. LCP está em 2,3 s (score 0,94), igual ao antes.

1. **Render-blocking:** 885 ms (antes 1.010 ms). `lang.js` continua síncrono no `<head>` (`site/index.html:6`), e o `styles.css` também bloqueia.
2. **Compressão de texto:** 300 ms / 52 KiB (antes 150 ms / 45 KiB). Provável artefato do `http.server`. Confirmar na hospedagem real, que usa o `.htaccess` do site.
3. **Cache de longa duração:** 10 recursos, 196 KB (antes 11 recursos, 205 KB). Também provável artefato do servidor local.
4. **CSS:** 10,5 KiB de CSS não usado e 6,7 KiB para minificar em `styles.css`, que cresceu de 30.581 B para 34.713 B.
5. **Imagens em formato moderno:** 29,8 KB possíveis (poster JPG de 41 KB e ícone PNG de 11,7 KB). Mesmo problema de antes.

Acessibilidade: `label-content-name-mismatch` era 0 e agora passa (1).

## Suspeitas de performance do app (`performance-antes.md`)

A lista tem 7 suspeitas e uma nota sem problema. Confira só os trechos citados.

| # | Situação antes | Situação agora | Status |
|---|---|---|---|
| 1 | `MainActivity.onStart` chamava `appSeen` e `reschedule` na thread principal | `onStart` usa `app.container.appScope.launch` (`MainActivity.kt:57-66`). `PreAlarmCheck.prefs` ainda chama `getSharedPreferences` a cada uso (`PreAlarmCheck.kt:88`), mas as leituras de `reschedule` e `check` rodam em `withContext(io)` | **Parcial** |
| 2 | `rescheduleAll` com laço de `schedule` na thread principal | `rescheduleAll` roda em `withContext(io)` (`AlarmRepository.kt:150`), e o laço e `onChanged` rodam no IO | **Resolvido** |
| 3 | `onAlarmFired` lia o banco (`getById`) antes de tocar o som | Continua assim: `getById` em `AlarmRingService.kt:120`, `startAudio` em `:166`, `setEnabled` em `:193` | **Ainda existe** |
| 4 | `buildPlayer` chamava `prepare()` síncrono | Continua em `AlarmRingService.kt:459` | **Ainda existe** |
| 5 | `waitUntilInUse` com polling a cada 250 ms | Não há mais `UNLOCK_POLL_MS`. A espera usa `BroadcastReceiver` com `withTimeoutOrNull` (`PreAlarmCheck.kt:230-250`) | **Resolvido** |
| 6 | `speakerGrille` desenhava centenas de `drawCircle` em `onDrawBehind` dentro de `drawWithCache` | Mesmo padrão em `Hardware.kt:185-199`. Custo não medido | **Ainda existe** |
| 7 | `ribLight` refazia o laço de riscas a cada draw | Mesmo padrão em `Hardware.kt:886-898`. Custo não medido | **Ainda existe** |
| nota | `FlipDigit`, `RingScreen` e `RingActivity` estavam sem problema | Não revisitados nesta medição | n/a |

## Tabela de estouros

Nenhum item estourou o limite. Itens que pedem atenção (dentro do limite, mas não resolvidos):

- Suspeitas 3, 4, 6 e 7 do app continuam no código (ver tabela acima).
- `classes.dex` +6,3% no APK.
- 28 testes ignorados na suíte.
- Lint: 60 warnings (sem limite no orçamento).

## Medição final (depois das correções da revisão)

- APK release (`app-release-unsigned.apk`): 4.132.133 B. Antes: 4.066.524 B. Variação: +65.609 B (+1,61%), dentro do limite de +5% (até 4.269.850 B).
- Testes unitários (`testDebugUnitTest`): 385 no total, 0 falhas, 0 erros, 28 ignorados, 127,4 s somados nos XML.
- Lint (`lintDebug`): 0 erros, 57 warnings (antes: 60).
