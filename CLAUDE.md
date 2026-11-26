# Wake Wake Up

Despertador para Android que só desliga depois de uma missão (contas ou frase). Visual de rádio-relógio dos anos 70 com dígitos flip, em português e inglês. Tem também um site estático de download em `site/`. Detalhes de produto no `README.md`.

## Stack

- Kotlin 2.2 + Jetpack Compose (BOM 2026.06) + Material 3 + Navigation Compose 2.9; MVVM com ViewModels e um `AppContainer` simples.
- Room 2.8 (banco v6, schemas em `app/schemas/`, migrações escritas à mão) + Coroutines.
- Alarme: `AlarmManager.setAlarmClock`, serviço em primeiro plano `AlarmRingService`, `RingWatchdog` a cada 3 min, `BootReceiver`.
- AGP 9.3, Gradle 9.5 (wrapper), compileSdk 37, targetSdk 36, minSdk 26, JDK 17.
- Testes: JUnit + Robolectric + Compose UI na JVM (`app/src/test`). Sem emulador neste PC.
- Site: HTML/CSS/JS puro, sem build (`site/`, PT na raiz e EN em `site/en/`).

## Rodar, compilar e testar (Git Bash)

O caminho tem acento ("Área de Trabalho") e os workers de teste do Gradle quebram com isso. Use sempre o script, que monta a pasta numa letra de unidade com `subst` quando precisa:

```bash
bash ferramentas/gradle.sh :app:assembleDebug
bash ferramentas/gradle.sh :app:testDebugUnitTest                 # suíte inteira (~270 testes)
bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "*AlarmTimingTest*"
bash ferramentas/gradle.sh :app:lintDebug
bash ferramentas/gradle.sh :app:assembleRelease                   # sem keystore.properties sai sem assinatura
WWU_SCREENSHOTS=1 bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"
```

- Prints saem em `app/build/screenshots/` com prefixos `pt`, `en`, `pequeno` (celular pequeno) e `fonte-grande` (1.3x).
- Dois Gradle na mesma pasta ao mesmo tempo brigam por trava: trabalho paralelo vai em worktrees separados.
- Worktrees: crie a partir de `melhorias-claude` (ou da branch atual), de preferência fora do OneDrive e num caminho sem acento, ex.: `git worktree add C:/Users/vinic/wwu-wt/<nome> -b <branch> HEAD`.
- Site: `python -m http.server 8765 --directory site` e abrir `http://localhost:8765/`. O `lang.js` redireciona pelo idioma do navegador (força com `localStorage wwu-lang=en|pt`).

## Estrutura (`app/src/main/java/com/wakewakeup/`)

- `data/`: entidades Room (`AlarmEntity`, `WakeHistoryEntity`), `AppDatabase`, `AlarmRepository`, `RadioBrowser` (busca de rádios).
- `session/`: agendar (`AlarmScheduler`, `nextRingMillis`), tocar (`AlarmRingService`, `RingActivity`, `RingWatchdog`), missões (`MissionGenerator`), "Já acordou?" (`PreAlarmPlanner`, `PreAlarmCheck`), receivers.
- `ui/`: uma pasta por tela, `components/` (Hardware, FlipDigit, Keypads, DayKeys, Labels), `theme/`, `navigation/` (`Destinations`, `WwuNavGraph`).
- `res/raw/`: 7 sons de alarme. Os nomes NÃO podem mudar: alarmes salvos guardam o som pelo nome do recurso.
- `res/values/strings.xml` (EN) e `res/values-pt/strings.xml` (PT): toda string nova entra nos dois (o `StringsTest` confere).

## Telas

Permissões, Lista de alarmes, Editar alarme, Tarefa, Som, Rádio, Estatísticas, Confirmar pulo (rotas do `WwuNavGraph`) e, em `RingActivity`, Toque (com Espere), Missão e Bom dia. Mapa completo com estados e as 93 funções em `.auditoria/mapa.md`; status de cada função em `.auditoria/funcoes.md`.

## Convenções

- Tema escuro fixo, paleta e tipografia em `ui/theme/` (Barlow Condensed + Instrument Sans). Componentes de "hardware" em `ui/components/Hardware.kt`.
- Textos de interface curtos e diretos, sem cara de marketing; PT-BR é o idioma principal.
- Comentários e mensagens de commit do código original em inglês; commits desta revisão em português.
- Sem dependências novas de runtime sem razão forte. Orçamento em `.auditoria/orcamento.md`.
- Áudio: manifesto em `assets/audio/audio_manifest.md` (o app não usa música).
- Nunca versionar `local.properties`, `keystore.properties`, `*.jks`, `.env`.
