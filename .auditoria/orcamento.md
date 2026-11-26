# Orçamento de performance: Wake Wake Up

Plataforma principal: **app Android** (celular, Android 8+ / minSdk 26, Kotlin + Jetpack Compose).
Plataforma secundária: **site de download** em `site/` (HTML, CSS e JS puros).

Nenhuma melhoria pode estourar estes limites. Medir antes (Fase 1, `performance-antes.md`) e depois (Fase 6, `performance-depois.md`). Se algo estourar, vira P0.

## App Android

| Item | Limite | Como medir |
|---|---|---|
| Tamanho do APK release (R8 + shrinkResources) | no máximo +5% sobre a linha de base da Fase 1, e nunca acima de 12 MB | `gradlew :app:assembleRelease`, tamanho do APK em `app/build/outputs/apk/release/` |
| Sons em `res/raw` | total até 2,5 MB, cada arquivo até 400 KB | `du` na pasta |
| Fontes em `res/font` | até 5 arquivos e 450 KB no total (hoje: 5 arquivos, ~420 KB) | `du` na pasta |
| Imagens | ícones e ilustrações em VectorDrawable; bitmap só em WebP, até 1024 px no maior lado | lista de `res/drawable*` |
| Dependências de runtime | nenhuma nova sem razão forte registrada no relatório | `app/build.gradle.kts` e `libs.versions.toml` |
| Partida a frio até a lista de alarmes | até 1,5 s em aparelho intermediário; nada de trabalho de disco ou rede na thread principal | revisão de código (sem emulador neste PC) e `adb shell am start -W` quando houver aparelho |
| Fluidez | 60 FPS na rolagem da lista e nos dígitos flip; nenhuma tela parada recompondo sem parar (o relógio pode atualizar 1x por segundo) | revisão de código das animações e dos `LaunchedEffect`/`while(true)` |
| Tela de toque (`RingActivity`) | abre por cima da tela bloqueada em até 1 s depois do disparo; som começa antes da UI terminar de compor | revisão de código de `AlarmRingService` e `RingActivity` |
| Memória ao tocar | até 150 MB PSS | `adb shell dumpsys meminfo com.wakewakeup` quando houver aparelho |
| Bateria | nenhum wake lock fora do toque; nada de polling em segundo plano; alarmes só por `AlarmManager.setAlarmClock` | revisão de código |
| Testes JVM | a suíte inteira (`gradlew :app:testDebugUnitTest`) deve continuar passando; tempo registrado na linha de base | Gradle |

## Site (`site/`)

| Item | Limite |
|---|---|
| Lighthouse no celular | 90 ou mais em Performance, Acessibilidade, Boas práticas e SEO |
| Peso da página inicial sem o vídeo e sem o APK | até 500 KB |
| JavaScript próprio | até 30 KB no total, sem framework |
| Vídeo | `preload="none"` ou `metadata`, com pôster; nunca baixado antes de o usuário pedir |
| Imagens | WebP ou AVIF quando passarem de 50 KB, com `width`/`height` definidos |
| Fontes | `font-display: swap`, no máximo 4 arquivos woff2 |

## Prints

Não há emulador neste PC, então os prints do app saem do `ScreenshotTour` (Robolectric, na JVM):

- celular padrão: 411x891 dp (equivalente ao 390x844 pedido para web);
- celular pequeno: 360x640 dp, para pegar layout apertado;
- fonte grande (1.3x), para pegar texto cortado.

Prints do site: Playwright em 390x844 (celular) e 1440x900 (desktop).
