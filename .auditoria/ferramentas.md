# Ferramentas: build, lint, testes e site

Data: 2026-11-26. Branch melhorias-claude. Nenhum código do projeto foi alterado.
Logs: `.auditoria/logs/ferramentas-build.log` (run principal), `ferramentas-testes-retry.log` e `ferramentas-testes-stacktrace.log` (reruns dos testes), `ferramentas-kotlin-main.log` (recompilação limpa do código principal).

## Gravidade ALTA

1. **Suíte de testes unitários não executa (BLOQUEIO).** `:app:testDebugUnitTest` falha em todas as classes de teste com `ClassNotFoundException` (ex.: `com.wakewakeup.AppSetupTest`, `StringsTest`, `data.AlarmRepositoryTest`, `ui.AlarmListUiTest`). Nenhum XML de resultado foi gerado (`app/build/test-results/testDebugUnitTest/` vazio). Reproduzido 3 vezes, sempre igual. Total de testes: não apurado. Tempo da suíte e 10 mais lentos: não apurados.
   - Hipótese (não confirmada): o caminho do projeto contém "Área" (acento). Nos logs o worker do teste aparece com o caminho corrompido (`C:\Users\vinic\OneDrive\�rea de Trabalho\...`), o que sugere que o classpath passado ao processo de teste perde a codificação. As classes existem em `app/build/tmp/kotlin-classes/debugUnitTest/com/`, então o problema é de classpath, não de compilação.
   - Teste sugerido (o usuário decide): rodar a mesma tarefa a partir de uma cópia do projeto em caminho sem acento, por exemplo `C:\dev\wakewakeup`.

2. **Erro de lint (bloqueia `lintDebug`).** `NonObservableLocale`, `app/src/main/java/com/wakewakeup/ui/components/Labels.kt:74`. Dentro de composable, `Locale.getDefault()` não é observável. Correção indicada pelo lint: `LocalConfiguration`/`LocalLocale.current.platformLocale`.

## Gravidade MÉDIA

3. **Lint: 50 warnings e 1 hint** (total 52 issues, contados do `app/build/reports/lint-results-debug.xml`):
   - 13x `UseKtx`: `edit` de SharedPreferences sem KTX. Ex.: `ui/permissions/AlarmPermissions.kt:60, 71, 77`; `session/AlarmRingService.kt:251, 267`; `session/PreAlarmCheck.kt:120`.
   - 11x `GradleDependency`: core-ktx 1.19.0 desatualizado (1.19.1 disponível). Locais em `gradle/libs.versions.toml:5, 6, 8, 9, 10, 16`.
   - 4x `NewerVersionAvailable`: Kotlin Android 2.2.20 (2.4.21 disponível). `gradle/libs.versions.toml:3, 11, 13`.
   - 4x `ModifierParameter`: `ui/components/FlipDigit.kt:213`; `ui/components/Hardware.kt:310, 570, 582`.
   - 4x `UnusedResources`: `R.color.icon_background` em `res/values/colors.xml:4`, entre outros.
   - 2x `ApplySharedPref`: `session/RingWatchdog.kt:42, 47` (usa `commit`, pode ser `apply`).
   - 2x `UnusedAttribute`: `showWhenLocked` só existe na API 27+, mas o min é 26. `app/src/main/AndroidManifest.xml:45, 46`.
   - 2x `AndroidGradlePluginVersion`: Gradle 9.5.0 (9.9.2 disponível). `gradle/wrapper/gradle-wrapper.properties:3`.
   - 2x `ObsoleteSdkInt`: `WakeWakeUpApplication.kt:25` e `res/mipmap-anydpi-v26`.
   - 2x `UseOfNonLambdaOffsetOverload`: `ui/components/Hardware.kt:791`; `ui/sound/SoundPickerScreen.kt:299`.
   - 1x `InlinedApi`: `session/BootReceiver.kt:38` (`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` exige API 31; precisa de guarda de versão).
   - 1x `OldTargetApi`: `app/build.gradle.kts:23` (target SDK desatualizado).
   - 1x `PluralsCandidate`: `res/values/strings.xml:117`.
   - 1x `IconLauncherShape`: `res/drawable-nodpi/ic_launcher_foreground_art.png` preenche o quadrado inteiro.
   - 1x hint `AutoboxingStateCreation`: `ui/sound/RadioScreen.kt:91` (usar `mutableIntStateOf`).

4. **Warnings do compilador Kotlin (código principal, recompilação limpa): 3.** Todos em `app/src/main/java/com/wakewakeup/ui/sound/BundledSounds.kt:9` (colunas 25, 50 e 79). Anotação aplicada só ao parâmetro; comportamento futuro muda para o campo.

5. **Warnings do compilador Kotlin (testes): 6.** Todos de API deprecada no código de teste:
   - `session/AlarmRingServiceTest.kt:564` (`field type: Int`)
   - `session/PreAlarmCheckTest.kt:47` (`setIsInteractive`), `:78` (`isActivityIntent`), `:118` e `:156` (`field type: Int`), `:191` (`isBroadcastIntent`)

## Gravidade BAIXA

6. **Site (site/):** `node --check` sem erros em `site/main.js`, `site/lang.js` e `site/site-config.js`. Links e src locais: 5 HTML, 66 referências verificadas, 0 quebradas.

## Resumo de contagens
- Erros de compilação Kotlin: 0. Warnings Kotlin: 3 (código principal) e 6 (testes).
- Lint: 1 erro, 50 warnings, 1 hint.
- Testes: não executam (0 resultados). Falha do processo de teste, causa provável é o caminho com acento.
- Site: 0 erros de sintaxe JS, 0 links quebrados.
- Pendente para decisão do usuário: testar a suíte a partir de caminho sem acento.
