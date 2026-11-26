# Performance antes (linha de base): Wake Wake Up

Medido em 2026-11-26, branch melhorias-claude, sem alterar código. Limites vindos de `orcamento.md`.

## Tabela

| item | medido | limite | dentro? |
|---|---|---|---|
| APK release sem assinatura (R8 + shrinkResources) | 4.066.524 B (~4,07 MB) | até +5% da linha de base e nunca acima de 12 MB | sim (esta é a linha de base) |
| APK debug | 15.324.797 B (~15,3 MB) | sem limite no orçamento | n/a |
| Tempo do `:app:assembleRelease` | 65 s (1m03s) | sem limite | n/a |
| Composição release: `classes.dex` | 2.479.656 B | n/a | n/a |
| Composição release: `resources.arsc` | 205.888 B | n/a | n/a |
| Composição release: `lib/` (4 ABIs de androidx.graphics.path) | 37.392 B | n/a | n/a |
| `res/raw` (sons) total | 2.105.236 B (~2,11 MB), 8 arquivos (7 mp3 + keep.xml) | até 2,5 MB | sim |
| `res/raw` cada mp3 | 300.717 B (todos os 7 com o mesmo tamanho) | até 400 KB por arquivo | sim |
| `res/font` total | 419.212 B (~409 KiB), 5 arquivos | até 450 KB e 5 arquivos | sim (no limite de arquivos) |
| `res/drawable-nodpi` (PNG de ícone) | 117.515 B, 432x432 px, PNG RGB | bitmap só em WebP, até 1024 px | NÃO (PNG, não WebP) |
| `res/drawable` (XML) | 1.276 B, 3 arquivos | VectorDrawable | sim |
| `res/mipmap-anydpi-v26` (XML) | 696 B | n/a | sim |
| Testes JVM (`:app:testDebugUnitTest`) | NÃO CONCLUÍDO: falhou em todas as classes com `ClassNotFoundException` (24 s, também com `--rerun`) | a suíte inteira deve passar | NÃO (falha de execução, ver nota) |
| Lighthouse mobile, página PT (`/`): Performance | 97 | 90 ou mais | sim |
| Lighthouse mobile, página PT: Acessibilidade | 100 | 90 ou mais | sim |
| Lighthouse mobile, página PT: Boas práticas | 100 | 90 ou mais | sim |
| Lighthouse mobile, página PT: SEO | 100 | 90 ou mais | sim |
| Lighthouse mobile, página EN (`/en/`): Performance | 97 | 90 ou mais | sim |
| Lighthouse mobile, página EN: Acessibilidade / Boas práticas / SEO | 100 / 100 / 100 | 90 ou mais | sim |
| Peso da página inicial PT sem vídeo e sem APK | 225.960 B (~226 KB; Lighthouse: 223 KiB) | até 500 KB | sim |
| JavaScript próprio (`main.js` + `site-config.js` + `lang.js`) | 19.361 B (~19 KB) | até 30 KB | sim |
| Vídeo `wake-wake-up-video.mp4` | `preload="none"` com pôster (index.html:115) | nunca baixado antes do pedido | sim |
| Fontes woff2 na página | 5 arquivos (instrument-sans 400/500/600, barlow-condensed 500/600) | no máximo 4 arquivos woff2 | NÃO (5 > 4) |
| `font-display` | `swap` nas 5 `@font-face` (styles.css:4-8) | swap | sim |
| Imagens acima de 50 KB | só o pôster `assets/video-poster.jpg` (41.010 B) está abaixo do limite; nenhuma acima | WebP/AVIF acima de 50 KB | sim |

Cálculo da página inicial PT (bytes): index.html 16.163 + styles.css 30.581 + lang.js 2.334 + main.js 16.329 + site-config.js 698 + 5 fontes 104.988 + app-icon.png 11.745 + video-poster.jpg 41.010 + favicon-32.png 2.112 = 225.960. O `apple-touch-icon.png` (35.843 B) só baixa em iOS e ficou de fora.

## Notas de medição

- Testes JVM: o Gradle falha com `Test process encountered an unexpected problem` e `ClassNotFoundException` para todas as classes de teste (AppSetupTest, StringsTest, ... ScreenshotTour). As classes existem em `app/build/tmp/kotlin-classes/debugUnitTest/`. Causa não identificada (pode ser o caminho com acento/OneDrive no classpath do worker, não testado). Logs: `.auditoria/logs-test-build.txt`, `logs-test-build2.txt` (com --stacktrace), `logs-test-build3.txt` (com --rerun). Como não há XML em `app/build/test-results/testDebugUnitTest/`, o tempo da suíte ficou sem medida.
- Lighthouse: Chrome headless, perfil mobile, servidor `python -m http.server 8766` (encerrado). A primeira rodada EN foi descartada: o navegador com locale pt-BR foi redirecionado para `/` pelo `lang.js`; a rodada válida usou `--lang=en-US`. JSON em `.auditoria/logs/lighthouse-antes.json` e `.auditoria/logs/lighthouse-antes-en.json`.
- As 7 mp3 de `res/raw` têm exatamente 300.717 B, mas md5 diferentes. Vale conferir se são arquivos de verdade ou placeholders gerados com o mesmo tamanho.

## Top 5 problemas do site (Lighthouse, PT)

1. Render-blocking: 1.010 ms de economia estimada. `lang.js` é carregado síncrono no `<head>` (index.html:6), e o `styles.css` também bloqueia.
2. Compressão de texto: 45 KiB de economia estimada. Provavelmente artefato do `http.server` do Python (não comprime); confirmar na hospedagem real.
3. Cache de longa duração: 205 KiB, 11 recursos. Também provavelmente artefato do servidor local; confirmar na hospedagem.
4. CSS não minificado: 6 KiB de economia (`styles.css`, 30.581 B).
5. Acessibilidade: `label-content-name-mismatch` com score 0 (rótulo visível diferente do nome acessível). Detalhes no JSON.

## Suspeitas no app (revisão de código, não medidas)

1. `MainActivity.kt:131` e `:133`: em `onStart`, `preAlarm.appSeen()` e `preAlarm.reschedule()` (este último no `lifecycleScope`, que roda na thread principal). `PreAlarmCheck.kt:68` (`prefs` é `getSharedPreferences` a cada uso), `:119-120`, `:235` e `:242` leem SharedPreferences na thread principal. Leitura síncrona do XML na primeira vez.
2. `MainActivity.kt:157` → `data/AlarmRepository.kt:140-149`: `rescheduleAll` roda no `lifecycleScope`. Depois dos `suspend` do DAO, o laço `scheduler.schedule(alarm)` (`session/AlarmScheduler.kt:36`) e `onChanged()` voltam à thread principal. Custo proporcional ao número de alarmes.
3. `AlarmRingService.kt:99` e `:118`: em `onAlarmFired`, o som só começa (`startAudio`, linha 130) depois de `alarmRepository.getById` e `setEnabled` (duas idas ao banco). Isso vai contra o orçamento de "som começa antes da UI". `cancelOffer` (linha 113) também vem antes.
4. `AlarmRingService.kt:342`: `buildPlayer` chama `prepare()` síncrono na thread principal do serviço. Para arquivo local é rápido; para som do usuário em armazenamento lento, pode demorar.
5. `session/PreAlarmCheck.kt:203-209`: `waitUntilInUse` faz polling a cada 250 ms (`UNLOCK_POLL_MS`, linha 45) até `unlockWaitMs`. Limitado no tempo, mas vai contra "nada de polling em segundo plano" do orçamento.
6. `ui/components/Hardware.kt:191-200`: a grade de furos desenha centenas de `drawCircle` em `onDrawBehind`, dentro de `drawWithCache`. O cache guarda só o modificador, não o desenho. Recalcula a cada draw. Custo não medido.
7. `ui/components/Hardware.kt:893-900`: as riscas de `ribLight` fazem o mesmo laço a cada draw. Custo não medido.
8. Sem problema encontrado: `ui/components/FlipDigit.kt:83-99` tem animações finitas (`animateTo` com tween de 420 ms). `RingScreen.kt:93-108` e `:214-219` têm loops de 250 ms (só nascer do sol), 60 s (relógio) e por segundo (Espera), com estado lido só na leitura do desenho. `session/RingActivity.kt:35` e `:93-94` configuram tela sobre a tela de bloqueio antes de `setContent`.

## Pendências

- Corrigir a falha de `:app:testDebugUnitTest` antes de medir o tempo da suíte (linha de base incompleta).
- Medir no aparelho quando houver um (partida a frio, memória ao tocar, FPS): não há emulador neste PC.
- Prints do `ScreenshotTour` não foram gerados (depende da suíte de testes, que falhou).
