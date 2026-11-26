# Mapa do projeto Wake Wake Up

Gerado em 2026-11-26 pelo agente MAPA (somente leitura). Caminhos relativos à raiz do checkout `app/...` e `site/...`.

## 1. Stack e versões

Fonte: `gradle/libs.versions.toml` e `app/build.gradle.kts`.

| Item | Versão |
|---|---|
| Android Gradle Plugin | 9.3.1 |
| Kotlin | 2.2.20 (plugin compose e android) |
| KSP | 2.2.20-2.0.4 |
| compileSdk | 37 |
| minSdk | 26 |
| targetSdk | 36 |
| versionCode / versionName | 2 / 1.1 |
| Compose BOM | 2026.06.00 (Material 3, UI, Graphics, Tooling) |
| Navigation Compose | 2.9.7 |
| Room | 2.8.0 (schemas exportados em `app/schemas`, banco na versão 6) |
| Lifecycle | 2.11.0 (viewmodel-compose, runtime-compose, service) |
| Activity Compose | 1.13.0 |
| Core KTX | 1.19.0 |
| Coroutines Android | 1.8.1 |
| Testes | JUnit 4.13.2, Robolectric 4.17, androidx test core 1.7.0, test ext junit 1.3.0, Espresso 3.5.1, compose-ui-test-junit4 (versão pela BOM) |

Config relevante: `namespace = com.wakewakeup`, `applicationId = com.wakewakeup`. Java/JVM 17. `isMinifyEnabled` e `isShrinkResources` true no release, com `proguard-rules.pro`. Assinatura release só se existir `keystore.properties` na raiz (git-ignored). `generateLocaleConfig = true` (PT e EN no seletor de idioma do Android 13+). Testes Robolectric com recursos reais (`unitTests.isIncludeAndroidResources`) e heap de 2g.

## 2. Estrutura de pastas

Pacote raiz: `app/src/main/java/com/wakewakeup/`

- `WakeWakeUpApplication.kt`: cria os canais de notificação (`alarm_ringing` para o toque, canal `pre_alarm` para a pergunta "Já acordou?").
- `AppContainer.kt`: composição manual de dependências (banco, `AlarmScheduler`, `PreAlarmCheck`, `AlarmRepository`, `WakeHistoryRepository`, `appScope`).
- `MainActivity.kt`: activity única de launcher. Decide a tela inicial (permissões ou lista), trata o intent de "Confirmar pular" e de abrir estatísticas, reagenda alarmes no início do processo, reabre `RingActivity` se houver toque em andamento.
- `data/`
  - `Alarm.kt`: modelo de domínio (`Alarm`, `TaskType` MATH/PHRASE, `Difficulty` EASY/MEDIUM/HARD).
  - `AlarmEntity.kt`: entidade Room, conversões `toDomain()` / `toEntity()`, interface `AlarmDao`.
  - `AlarmRepository.kt`: regras de alarme (salvar, excluir, ligar/desligar, pular próximo dia, pular um toque, desfazer, armar próximo toque, reagendar tudo).
  - `AppDatabase.kt`: Room versão 6, migrações `MIGRATION_4_5` (remove amostras antigas) e `MIGRATION_5_6` (pausa vira pulo). Destrutiva só nas versões iniciais de desenvolvimento.
  - `WakeHistoryEntity.kt`, `WakeHistoryEntry.kt`, `WakeHistoryRepository.kt`: histórico de despertares (data, hora do alarme, hora que acordou, número de esperas, duração), com `observeLastDays`.
  - `RadioBrowser.kt`: busca de rádios na API pública radio-browser.info (só HTTPS, limite 40, por nome ou as mais ouvidas do país).
- `session/`: tudo que acontece quando o alarme toca.
  - `AlarmReceiver.kt`: BroadcastReceiver disparado pelo AlarmManager. Ignora alarme desligado ou pulado no dia. Inicia `AlarmRingService`. Também trata a ação do watchdog.
  - `AlarmScheduler.kt`: agenda alarmes exatos (`setAlarmClock`) e funções puras de cálculo: `nextTriggerMillis`, `nextRingMillis`, `isSkippingNext`, `isoDayIndexOf`, `epochDayOf`, `nextTriggerEpochDay`.
  - `AlarmRingService.kt`: serviço em primeiro plano (`systemExempted`) que toca áudio com rampa, vibra, mantém o WakeLock, roda o temporizador (1 s), a missão, o Espere e a tela Bom dia. Grava a história ao concluir.
  - `AlarmSessionState.kt`: estado em memória da sessão (`RingSession`, `MissionState`, `FinishedInfo`, enum `SessionScreen` RINGING/MISSION/GOOD_MORNING) e constantes (`MISSION_TOTAL_SECONDS=120`, `HOLD_ON_SECONDS=300`, `RAMP_SECONDS=60`, `RAMP_START_VOLUME=0.15`).
  - `RingWatchdog.kt`: persiste a sessão em SharedPreferences (`ring_session`) e arma um alarme de checagem a cada 3 min, por até 3 h, para retomar o toque se o processo morrer.
  - `RingActivity.kt`: tela cheia sobre a tela de bloqueio (`showWhenLocked`, `turnScreenOn`). Escolhe entre `RingScreen`, `MissionScreen` e `GoodMorningScreen` conforme `SessionScreen`. Voltar não dispensa o alarme.
  - `MissionGenerator.kt`: contas de matemática por dificuldade, frase do dia (`phraseOfTheDay`), normalização e comparação de frase, realce do prefixo digitado.
  - `PreAlarmPlanner.kt`: função pura que decide quando e quais alarmes matinais (hora entre 3 e 11) oferecer para pular ("Já acordou?"). Constantes: `LEAD_MS` 1 h, `CUTOFF_MS` 5 min, `CLUSTER_MS` 1 h, `APP_SEEN_MS` 90 min, `RECHECK_MS` 1 min.
  - `PreAlarmCheck.kt`: notificação de oferta, recibo de pulo, reagendamento da checagem (SharedPreferences `pre_alarm`). IDs de notificação em `NotificationIds` (42 a 45). Ações `ACTION_CONFIRM_SKIP`, `ACTION_UNDO_SKIP`.
  - `PreAlarmReceiver.kt`: recebe a checagem agendada e a ação de pular ou desfazer.
  - `BootReceiver.kt`: reagenda tudo em BOOT_COMPLETED, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED e mudança de permissão de alarme exato. No boot também retoma o toque interrompido.
- `ui/`
  - `navigation/`: `Destinations.kt` (rotas e `SkipRequest`), `WwuNavGraph.kt` (NavHost com as 8 rotas).
  - `list/`: `AlarmListScreen.kt`, `AlarmListViewModel.kt` (ligar, pular, desfazer pulo, volume do alarme).
  - `edit/`: `EditAlarmScreen.kt`, `EditAlarmViewModel.kt` (rascunho, salvar, excluir, dias, rampa, tarefa, som).
  - `task/TaskConfigScreen.kt`: tipo de tarefa, dificuldade, quantidade de problemas, prévia da frase do dia.
  - `sound/`: `SoundPickerScreen.kt` (sons do app, toques do celular, música do celular, padrão do sistema, rampa), `RadioScreen.kt` (busca de rádio), `BundledSounds.kt` (5 sons em `res/raw`: bipe_digital_1, bipe_digital_2, campainha_mecanica, passaros, sirene_1, sirene_2, telefone_de_disco), `SoundPreview.kt`.
  - `stats/`: `StatsScreen.kt`, `StatsViewModel.kt`.
  - `skip/`: `SkipConfirmScreen.kt`, `SkipConfirmViewModel.kt` (confirmação com conta para pular um toque, com desfazer).
  - `ring/`: `RingScreen.kt`, `RingSessionViewModel.kt` (tela do toque: Espere e Começar missão).
  - `mission/MissionScreen.kt`: calculadora e campo de frase, contador, "Não consigo resolver".
  - `goodmorning/GoodMorningScreen.kt`: tela Bom dia (tempo que levou, esperas, oferta de pular outros alarmes da hora, Concluir).
  - `permissions/`: `AlarmPermissions.kt` (checagem: NOTIFICATIONS, EXACT_ALARMS, FULL_SCREEN obrigatórias; BATTERY recomendada), `PermissionsScreen.kt`.
  - `components/`: `FlipDigit.kt` (dígitos flip), `Hardware.kt` (botões, LEDs, visor VFD, Espere, medidor LED), `Keypads.kt` (teclado da calculadora), `DayKeys.kt` (seleção de dias), `Labels.kt`.
  - `theme/`: `Color.kt`, `Type.kt` (Barlow Condensed e Instrument Sans), `Shape.kt`, `Gradients.kt`, `Theme.kt` (sempre escuro).
- Recursos: `app/src/main/res/values/strings.xml` (209 strings, base em inglês) e `values-pt/strings.xml` (PT). Fontes em `res/font`. Sons em `res/raw`. `res/xml/backup_rules.xml` e `data_extraction_rules.xml`.

Pasta `app/src/test/java/com/wakewakeup/`: `AppSetupTest.kt`, `StringsTest.kt`, `data/` (5 arquivos), `session/` (7 arquivos), `ui/` (9 arquivos, incluindo `ScreenshotTour.kt` e `UiTesting.kt`), `testing/` (`AppEnv.kt`, `FakeRadio.kt`).

## 3. Pontos de entrada

Activities:
- `MainActivity` (LAUNCHER, exported). Extras: `EXTRA_OPEN_STATS`, ação `ACTION_CONFIRM_SKIP` com `EXTRA_SKIP_IDS` e `EXTRA_SKIP_ATS`.
- `session.RingActivity` (não exportada, singleInstance, showWhenLocked, turnScreenOn, excludeFromRecents).

Services:
- `session.AlarmRingService` (não exportado, foregroundServiceType systemExempted, START_STICKY). Ações: START_MISSION, HOLD_ON, MISSION_KEY, MISSION_SET_TYPED, MISSION_CONFIRM, GIVE_UP, FINISH.

Receivers:
- `session.AlarmReceiver` (não exportado): alarme disparado e `ACTION_RING_WATCHDOG`.
- `session.PreAlarmReceiver` (não exportado): checagem "Já acordou?", confirmar e desfazer pulo.
- `session.BootReceiver` (exportado): BOOT_COMPLETED, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED, SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.

Permissões declaradas: INTERNET, SCHEDULE_EXACT_ALARM (até API 32), USE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SYSTEM_EXEMPTED, USE_FULL_SCREEN_INTENT, VIBRATE.

Rotas de navegação (`Destinations`, `WwuNavGraph`):
- `permissions?fromList={fromList}` (inicial se faltar permissão obrigatória)
- `list` (inicial normal)
- `edit/{alarmId}` (`-1` = novo alarme)
- `task`
- `sound`
- `radio`
- `stats`
- `skip/{ids}/{ats}` (vem da notificação de pulo)

Telas que não são rota: as três telas hospedadas em `RingActivity` (`RingScreen`, `MissionScreen`, `GoodMorningScreen`), e as notificações (oferta "Já acordou?", recibo de pulo, notificação do toque).

## 4. Lista de telas

Total: 11 telas (8 rotas de navegação + 3 telas do toque em `RingActivity`), mais as notificações do sistema.

| # | Tela | Arquivo | Como se chega | Estados |
|---|---|---|---|---|
| T01 | Permissões | `ui/permissions/PermissionsScreen.kt` | Primeiro abrir (se faltar permissão obrigatória) ou pelo aviso da lista (`fromList=true`) | Passos pendentes; permissão concedida (acende ao voltar das Configurações); "tudo pronto"; bateria só recomendada |
| T02 | Lista de alarmes | `ui/list/AlarmListScreen.kt` | Tela inicial, ou "Continuar" das permissões | Vazia (`EmptyState`, convite para criar); com alarmes; aviso de permissão faltando; aviso de volume baixo; próximo alarme no visor; gaveta de pulo (SKIPPING / SKIP_PROMPT); carregando (nulo até primeira leitura, sem piscar vazio) |
| T03 | Editar alarme | `ui/edit/EditAlarmScreen.kt` | "+" na lista (novo) ou toque em um cartão (editar) | Novo / editando; sem dias (aviso `days_none_hint`); sem rótulo (aviso); exclusão armada (dois toques); confirmação ao sair com alterações |
| T04 | Tarefa | `ui/task/TaskConfigScreen.kt` | Editor, "Tarefa" | Matemática ou frase; dificuldade; quantidade de problemas; prévia da frase do dia |
| T05 | Som | `ui/sound/SoundPickerScreen.kt` | Editor, "Som" | Sons do app (5); toques do celular; música do celular (escolher arquivo); padrão do sistema; prévia; rádio da internet selecionada |
| T06 | Rádio | `ui/sound/RadioScreen.kt` | Som, "Rádio da internet" | Carregando (`radio_loading`); sem internet (`radio_offline`, com "tentar de novo"); sem resultados (`radio_empty`); resultados ("Populares" ou busca); erro (`radio_error`) |
| T07 | Estatísticas | `ui/stats/StatsScreen.kt` | Lista, ícone de estatísticas (aparece só com alarmes), ou abrir pela notificação/Bom dia | Sem registros (`stats_empty`, `stats_no_record`); semana com dados (7 colunas, média, tabela alarme x acordou) |
| T08 | Confirmar pulo | `ui/skip/SkipConfirmScreen.kt` | Notificação "Já acordou?" (toque na ação), intent `skip/{ids}/{ats}` | Carregando; confirmar (conta de multiplicação/soma); concluído (com desfazer); obsoleto (`skip_confirm_stale`, o toque já mudou) |
| T09 | Toque | `ui/ring/RingScreen.kt` (em `RingActivity`) | Alarme dispara (`SessionScreen.RINGING`) | Tocando (hora, Espere, Começar missão); Espere ativo (barra com contagem, "sem limite" ou "volta em"); volume subindo |
| T10 | Missão | `ui/mission/MissionScreen.kt` (em `RingActivity`) | "Começar missão" (`SessionScreen.MISSION`) | Problema N de M; resposta digitada; errada (`wrong`, cor de erro); acertou um (`justCorrect`); tempo restante (120 s); "Não consigo resolver" (troca para frase); modo frase (campo de texto, prefixo realçado) |
| T11 | Bom dia | `ui/goodmorning/GoodMorningScreen.kt` (em `RingActivity`) | Missão concluída (`SessionScreen.GOOD_MORNING`) | Com oferta de pular outros alarmes da hora (`companions`); sem oferta; fecha sozinha em 10 min; Concluir ou Voltar |

Notificações e superfícies do sistema (sem tela Compose):
- Oferta "Já acordou?" (`pre_alarm`, `pre_alarm_text_one` / `_many`), com ação de pular.
- Recibo de pulo (`SKIP_RECEIPT`), com ação de desfazer.
- Notificação do toque (`alarm_ringing`, texto do que está acontecendo, ação de abrir a tela do toque).
- Tela de bloqueio (RingActivity por cima).

## 5. Funções do ponto de vista do usuário

Legenda: "Testes" lista os métodos de `app/src/test` que cobrem a função (nomes exatos, classe entre parênteses quando ajuda). "sem teste direto" quer dizer que não há teste com esse nome.

### Alarmes (lista e editor)

- **F01 Criar alarme.** `ui/edit/EditAlarmViewModel.kt` (`startNew`, `save`), `ui/edit/EditAlarmScreen.kt`, `data/AlarmRepository.kt` (`save`), `session/AlarmScheduler.kt`. Testes: AlarmListUiTest.creatingAnAlarmFromScratch; AlarmRepositoryTest.savingANewAlarmSchedulesItsNextOccurrenceAsAnAlarmClock; AlarmListUiTest.leavingANewAlarmSavesNothing.
- **F02 Editar alarme.** `EditAlarmViewModel.startEdit/load`, `EditAlarmScreen.kt`. Testes: AlarmListUiTest.editingAnAlarm; AlarmRepositoryTest.editingAnAlarmReplacesItsSchedule.
- **F03 Sair do editor sem salvar (descartar).** `EditAlarmScreen.kt`, `EditAlarmViewModel.discardUnsaved`. Testes: AlarmListUiTest.leavingTheEditorWithBackDiscardsTheChanges; AlarmListUiTest.theSystemBackAsksBeforeLosingAChange; AlarmListUiTest.theSystemBackLeavesAnUntouchedAlarmAtOnce.
- **F04 Excluir alarme (dois toques).** `EditAlarmViewModel.deleteCurrent`, `AlarmRepository.delete`. Testes: AlarmListUiTest.deletingTakesTwoTaps; AlarmRepositoryTest.deletingCancels.
- **F05 Ligar e desligar alarme.** `AlarmListViewModel.toggleEnabled`, `AlarmRepository.setEnabled`, `AlarmListScreen.kt` (`SlideSwitch`). Testes: AlarmListUiTest.switchingAnAlarmBackOn; AlarmRepositoryTest.turningOffCancelsAndTurningOnReschedules.
- **F06 Escolher dias da semana.** `EditAlarmViewModel.toggleDay`, `ui/components/DayKeys.kt`, `data/AlarmEntity.kt` (dias guardados ordenados). Testes: AlarmEntityTest.daysAreStoredSortedAsText; AlarmListUiTest.aCardReadsItsDaysAsOnePhrase; AlarmTimingTest.weekdayAlarmOnFridayNightRingsMonday; AlarmTimingTest.weekendAlarmFromMonday; AlarmTimingTest.dayIndexesRunMondayToSunday.
- **F07 Definir hora e minuto (roda, passos de 10 min).** `EditAlarmViewModel.setHour/setMinute/stepHour/stepMinute`, `EditAlarmScreen.kt`. Testes: AlarmListUiTest.theTimeDrumsWorkWithTalkBack; AlarmTimingTest.midnightAlarm.
- **F08 Nome do alarme (rótulo).** `EditAlarmViewModel.setLabel`, `EditAlarmScreen.kt` (aviso sem nome). Testes: sem teste direto (AlarmEntityTest.alarmSurvivesTheRoundTripToTheDatabase cobre o campo na persistência).
- **F09 Ordem da lista por horário.** `AlarmListScreen.kt`. Testes: AlarmListUiTest.alarmsStayInTimeOrderWhetherOnOrOff.
- **F10 Próximo alarme no visor (contagem).** `AlarmListViewModel.nextAlarmMillis`, `AlarmListScreen.kt`, `session/AlarmScheduler.kt` (`nextRingMillis`). Testes: AlarmListUiTest.theNextDisplayCountsDownToTheNearestAlarm; AlarmListUiTest.aRingADayOrMoreAwayShowsItsDayAndTime; AlarmListUiTest.aRingAWeekOrMoreAwaySaysItIsTheNextOne.
- **F11 Estado vazio (convite para criar o primeiro).** `AlarmListScreen.kt` (`EmptyState`). Testes: AlarmListUiTest.anEmptyListInvitesToCreateTheFirstAlarm.
- **F12 Excluir e tocar em "+" (novo alarme).** `WwuNavGraph.kt`, `AlarmListScreen.kt`. Testes: cobertos por F01.

### Pular, desfazer e confirmar pulo

- **F13 Pular só o próximo dia (lista, gaveta).** `AlarmListViewModel.skipNext`, `AlarmRepository.skipNext`, `AlarmListScreen.kt` (`CardDrawerKind.SKIP_PROMPT`). Testes: AlarmRepositoryTest.skippingTheNextDayKeepsTheAlarmOnAndArmsTheRingAfterIt; AlarmTimingTest.skippingADayRingsAtTheFollowingOccurrence; AlarmTimingTest.aSkipOnlyCountsWhenItsDayIsTheNextOccurrence; AlarmTimingTest.aSkipAfterMidnightStillNamesTheMorningAhead; AlarmListUiTest.switchingARecurringAlarmOffOffersToSkipJustOneDay.
- **F14 Pular um alarme de uma vez só (sem dias) = desligar.** `AlarmRepository.skipNext` (alarme de uma vez: `setEnabled(false)`). Testes: AlarmRepositoryTest.skippingAOneTimeAlarmSwitchesItOff; AlarmListUiTest.switchingAOneOffAlarmOffDoesNotOfferTheSkip; AlarmTimingTest.oneTimeAndSwitchedOffAlarmsIgnoreASkip.
- **F15 Desfazer o pulo do dia (voltar a tocar).** `AlarmListViewModel.clearSkip`, `AlarmRepository.clearSkip`. Testes: AlarmRepositoryTest.undoingTheSkipRingsOnTheSkippedDayAgain.
- **F16 Ligar de volta é o mesmo que tirar o pulo; ligar/desligar à mão apaga o pulo.** `AlarmRepository.setEnabled`. Testes: AlarmRepositoryTest.switchingByHandDropsASkip.
- **F17 Pulo some ao salvar se o dia já passou ou o alarme está desligado.** `AlarmRepository.save` / `isValidSkip`. Testes: AlarmRepositoryTest.savingKeepsASkipOnlyWhileItStillFits.
- **F18 Pular um toque específico pela notificação "Já acordou?" (confirmar com conta).** `ui/skip/SkipConfirmViewModel.kt` (`load`, `pressKey`, `confirm`), `ui/skip/SkipConfirmScreen.kt`, `AlarmRepository.skipOccurrence`. Testes: SkipConfirmUiTest.turningOffTakesASumAndCanBeUndone; SkipConfirmUiTest.keepLeavesTheAlarmAlone; SkipConfirmUiTest.aRingThatAlreadyChangedCannotBeTurnedOff; AlarmRepositoryTest.skippingOneRingOnlyWorksWhileThatRingIsNextAndAhead; AlarmRepositoryTest.skippingOneRingOfAOneTimeAlarmSwitchesItOffAndUndoBringsItBack.
- **F19 Desfazer o pulo de um toque (pelo recibo ou pela tela).** `SkipConfirmViewModel.undo`, `AlarmRepository.undoSkipOccurrence`, `PreAlarmCheck` (ação de desfazer). Testes: PreAlarmCheckTest.theReceiptsUndoPutsTheRingBack; SkipConfirmUiTest.turningOffTakesASumAndCanBeUndone.
- **F20 Skip de um dia que já passou é ignorado (limpeza no boot ou no reagendamento).** `AlarmRepository.rescheduleAll` (`clearSkipsBefore`). Testes: AlarmRepositoryTest.rescheduleAllForgetsSkipsWhoseDayIsOver; AlarmReceiverTest.aRingAfterTheSkippedDayClearsTheSkip.

### Notificação "Já acordou?" (PreAlarmCheck)

- **F21 Perguntar, uma hora antes, sobre alarmes matinais (03:00 a 11:59).** `session/PreAlarmPlanner.kt` (`plan`), `session/PreAlarmCheck.kt` (`reschedule`, `postOffer`). Testes: PreAlarmPlannerTest.nothingIsAskedUntilAnHourBefore; PreAlarmPlannerTest.withinTheHourTheMorningAlarmIsOffered; PreAlarmPlannerTest.onlyMorningAlarmsAreEverQuestioned; PreAlarmCheckTest.anEveningAlarmIsNeverQuestioned; PreAlarmCheckTest.theFirstCheckIsArmedAnHourBeforeAndNeverWakesThePhone.
- **F22 Agrupar alarmes de reserva numa só pergunta.** PreAlarmPlanner (`cluster`). Testes: PreAlarmPlannerTest.backupAlarmsComeTogetherInOneOffer; PreAlarmCheckTest.backupAlarmsShareOneOffer.
- **F23 Não perguntar nos últimos 5 min antes de um toque.** PreAlarmPlanner (`CUTOFF_MS`). Testes: PreAlarmPlannerTest.neverInTheLastFiveMinutesBeforeARing; PreAlarmPlannerTest.anyAlarmAboutToRingHoldsTheOfferBack.
- **F24 Não perguntar se o app foi aberto há menos de 90 min.** PreAlarmPlanner (`APP_SEEN_MS`), `PreAlarmCheck.appSeen`. Testes: PreAlarmPlannerTest.havingTheAppOpenSinceBeforeMeansNoNeedToAsk.
- **F25 Cada toque é perguntado uma só vez.** PreAlarmPlanner (`firstUnhandled`), `PreAlarmCheck.markHandled`. Testes: PreAlarmPlannerTest.eachRingIsAskedAboutOnce; PreAlarmCheckTest.theSameRingIsNeverAskedAboutTwice.
- **F26 Desligados e pulados ficam de fora da pergunta.** PreAlarmPlanner. Testes: PreAlarmPlannerTest.switchedOffAndSkippedRingsAreLeftAlone.
- **F27 Não perguntar com o telefone em uso ou com a tela desligada (espera).** PreAlarmCheck (`phoneInUse`, `UNLOCK_POLL_MS`). Testes: PreAlarmCheckTest.phoneInUse (helper); PreAlarmCheckTest.aPhoneInUseGetsOneSilentOffer; PreAlarmCheckTest.withTheScreenOffItWaitsWithoutWakingThePhone; PreAlarmCheckTest.aLockedScreenIsNotInUse.
- **F28 Nada é perguntado durante um toque.** PreAlarmCheck / `AlarmRingService.onAlarmFired` (`cancelOffer`). Testes: PreAlarmCheckTest.nothingIsAskedWhileAnAlarmIsRinging; PreAlarmCheckTest.aRingingAlarmWithdrawsTheOfferToTurnItOff (em AlarmRingServiceTest).
- **F29 Retirar a oferta se o alarme mudou.** `PreAlarmCheck.dropStaleOffer`. Testes: PreAlarmCheckTest.anOfferWhoseAlarmChangedIsWithdrawn.
- **F30 Sem permissão de notificação, não há checagem.** `PreAlarmCheck.notificationsAllowed`. Testes: PreAlarmCheckTest.withoutTheNotificationPermissionThereAreNoChecks.
- **F31 Mostrar recibo de pulo com opção de desfazer.** `PreAlarmCheck.postReceipt`. Testes: PreAlarmCheckTest.theReceiptsUndoPutsTheRingBack.

### Toque e sessão

- **F32 Tocar no horário (som, tela cheia sobre a tela de bloqueio).** `session/AlarmReceiver.kt`, `session/AlarmRingService.kt` (`onRingStart`, `onAlarmFired`), `session/RingActivity.kt`. Testes: AlarmReceiverTest.anEnabledAlarmStartsTheRingingServiceAndSchedulesItsNextOccurrence; AlarmReceiverTest.aOneOffAlarmRingsAndIsNotScheduledAgain; AlarmReceiverTest.aDisabledAlarmNeverRings; AlarmReceiverTest.aDeletedAlarmOrABrokenIntentIsIgnored; AlarmRingServiceTest.ringsTheChosenSoundOnLoopStartingLow.
- **F33 Alarme de uma vez se desliga ao tocar; repetido continua ligado.** `AlarmRingService.onAlarmFired`. Testes: AlarmRingServiceTest.aOneOffAlarmTurnsItselfOffAsItRings; AlarmRingServiceTest.aRepeatingAlarmStaysOnWhenItRings; AlarmRingServiceTest.aRepeatingAlarmKeepsItsScheduleThroughTheWholeRitual.
- **F34 Som do alarme: sons do app (5 sons incluídos).** `ui/sound/BundledSounds.kt`, `AlarmRingService.startAudio`. Testes: SoundAndTaskUiTest.choosingOneOfTheAppsSoundsAndTurningTheRampOff; AlarmRingServiceTest.withNoSystemAlarmSoundTheBundledBellRings.
- **F35 Som do celular: toques do celular e padrão do sistema.** `SoundPickerScreen.kt`, `AlarmRingService.defaultAlarmUri`, `buildFallbackPlayer`. Testes: SoundAndTaskUiTest.goingBackToTheSystemDefault; AlarmRingServiceTest.aSoundThatCantPlayFallsBackToTheSystemAlarm; AlarmRingServiceTest.aBrokenSystemAlarmSoundFallsBackToTheBundledBell.
- **F36 Música do celular (escolher arquivo).** `SoundPickerScreen.kt`, `EditAlarmViewModel.setSound`. Testes: SoundAndTaskUiTest.pickingASongFromThePhone.
- **F37 Rádio da internet: escolher uma estação popular ou buscar por nome.** `ui/sound/RadioScreen.kt`, `data/RadioBrowser.kt` (`search`), `testing/FakeRadio.kt`. Testes: SoundAndTaskUiTest.wakingUpToARadioStation; SoundAndTaskUiTest.radioWithoutInternetCanRetryAndSearch; RadioBrowserTest.keepsOnlyNamedHttpsStationsOncePerStream; RadioBrowserTest.aStationWithoutACountryCodeKeepsAnEmptyOne; RadioBrowserTest.aBlankQueryAsksForTheMostListenedInTheCountry; RadioBrowserTest.aQuerySearchesByName; RadioBrowserTest.noNetworkIsAnError; RadioBrowserTest.anUnexpectedAnswerIsAnErrorToo.
- **F38 Rádio que não começa é trocado pelo alarme padrão após 8 s.** `AlarmRingService.startStream` (`STREAM_TIMEOUT_MS`). Testes: AlarmRingServiceTest.aRadioThatDoesntStartIsReplacedAfterEightSeconds.
- **F39 Rádio que tocando sem reação dá lugar ao alarme em volume total (carência de 3 min).** `AlarmRingService` (`RADIO_GRACE_MS`). Testes: AlarmRingServiceTest.aRadioNobodyReactsToGivesWayToTheAlarmAtFullVolume.
- **F40 Aumentar aos poucos (rampa de 15% a 100% em 60 s).** `AlarmRingService.tickRinging`, `SoundPickerScreen.kt` (ramp switch, `setRampUp`). Testes: AlarmRingServiceTest.volumeClimbsToFullInAMinute; AlarmRingServiceTest.withoutTheRampItRingsAtFullVolumeAtOnce; RingFlowUiTest.theVolumeMeterRisesWithTheRamp; SoundAndTaskUiTest.choosingOneOfTheAppsSoundsAndTurningTheRampOff.
- **F41 Vibração enquanto toca, parando com o som.** `AlarmRingService.startVibration/stopVibration`. Testes: AlarmRingServiceTest.itVibratesWhileItRingsAndStopsWithTheSound.
- **F42 Notificação do toque diz o que está acontecendo.** `AlarmRingService.buildNotification`. Testes: AlarmRingServiceTest.theNotificationSaysWhatTheAlarmIsDoing.
- **F43 Voltar não dispensa o alarme (tela do toque).** `RingActivity.kt`. Testes: RingFlowUiTest.backDoesntDismissTheAlarm.
- **F44 Tela do toque ao abrir depois que o alarme acabou volta ao app.** `RingActivity.kt`. Testes: RingFlowUiTest.aRingScreenOpenedAfterTheAlarmEndedGoesBackToTheApp.
- **F45 Tela do toque (hora, botões Espere e Começar missão).** `ui/ring/RingScreen.kt`. Testes: RingFlowUiTest.theWholeRitualFromRingingToGoodMorning; PortugueseUiTest.theRingScreen.
- **F46 Espere: 5 min de silêncio, conta de quantas vezes, volta em volume baixo.** `AlarmRingService.onHoldOn` (`HOLD_ON_SECONDS`), `ui/ring/RingSessionViewModel.kt`, `Hardware.kt` (`EspereBar`). Testes: AlarmRingServiceTest.espereSilencesForFiveMinutesAndCountsOnce; AlarmRingServiceTest.afterEspereItRingsAgainFromLow; AlarmRingServiceTest.espereKeepsThePhoneAwakeEvenAfterAQuarterOfAnHour; AlarmRingServiceTest.aSecondAlarmDuringEspereCanBeSnoozedToo; RingFlowUiTest.afterAnEspereTheBarCountsInsteadOfSayingNoLimit.
- **F47 Começar missão: silencia o som e mostra o primeiro problema.** `AlarmRingService.onStartMission`. Testes: AlarmRingServiceTest.startingTheMissionStopsTheSoundAndShowsTheFirstProblem.
- **F48 Missão matemática: teclado da calculadora (dígitos, C, apagar, sinal).** `ui/mission/MissionScreen.kt`, `ui/components/Keypads.kt`, `AlarmRingService.onMissionKey`. Testes: AlarmRingServiceTest.calculatorKeys; AlarmRingServiceTest.confirmingNothingDoesNothing; AlarmRingServiceTest.aWrongAnswerIsFlaggedAndTheNextKeyStartsOver; RingFlowUiTest.aStrayTapOnCantSolveKeepsTheSums.
- **F49 Problemas de matemática (fácil: soma de 1 dígito; média: soma de 2 dígitos; difícil: multiplicação).** `session/MissionGenerator.kt` (`mathQuestion`). Testes: MissionGeneratorTest.easyIsASumOfTwoSingleDigits; MissionGeneratorTest.mediumIsASumOfTwoTwoDigitNumbers; MissionGeneratorTest.hardIsAMultiplication.
- **F50 Quantidade de problemas (1 a N) e avanço.** `TaskConfigScreen.kt`, `EditAlarmViewModel.changeTaskCount`, `AlarmRingService.onMissionConfirm`. Testes: SoundAndTaskUiTest.taskSetupDifficultyAndHowManyProblems; AlarmRingServiceTest.solvingEveryProblemEndsInGoodMorning; RingFlowUiTest.theWholeRitualFromRingingToGoodMorning; AlarmRingServiceTest.calculatorKeys.
- **F51 Missão de frase do dia (digitar a frase, sem acento e sem diferenças de caixa).** `MissionGenerator.phraseOfTheDay/phraseMatches/normalizePhrase`, `MissionScreen.kt`. Testes: MissionGeneratorTest.phraseIsTheSameAllDayAndChangesEveryDay; MissionGeneratorTest.phraseFollowsTheLanguage; MissionGeneratorTest.phrasesHaveNoAccentsAndAreTypeable; MissionGeneratorTest.normalizationIgnoresCaseAccentsPunctuationAndExtraSpaces; MissionGeneratorTest.phraseMatching; MissionGeneratorTest.matchedPrefixLightsTheDisplayAsYouType; AlarmRingServiceTest.aPhraseAlarmGoesStraightToThePhrase; RingFlowUiTest.aPhraseAlarm.
- **F52 Prévia da frase do dia na configuração da tarefa.** `TaskConfigScreen.kt`. Testes: SoundAndTaskUiTest.theTaskPreviewShowsTodaysPhrase.
- **F53 "Não consigo resolver" troca para a frase.** `AlarmRingService.onGiveUp`, `MissionScreen.kt`. Testes: AlarmRingServiceTest.givingUpSwitchesToThePhraseAndKeepsTheClockRunning; RingFlowUiTest.cantSolveItSwitchesToTypingThePhrase.
- **F54 Tempo da missão (2 min); acabou, toca de novo mais alto (pelo menos 80%).** `AlarmRingService.tickMission` (`MISSION_TOTAL_SECONDS`). Testes: AlarmRingServiceTest.runningOutOfTimeRingsAgainLouder; AlarmRingServiceTest.theTimerCountsDownOncePerSecond; AlarmRingServiceTest.theMissionTimerKeepsThePhoneAwakeToo.
- **F55 Concluir a missão: grava o despertar e encerra o toque.** `AlarmRingService.finishSession`, `data/WakeHistoryRepository.kt`. Testes: AlarmRingServiceTest.theWakeUpIsSavedAsSoonAsTheMissionIsDone; AlarmRingServiceTest.finishingRecordsTheWakeUpAndStopsEverything; AlarmRingServiceTest.finishingTheMissionEndsTheWatch.
- **F56 Tela Bom dia (tempo que levou, esperas, Concluir).** `ui/goodmorning/GoodMorningScreen.kt`, `AlarmRingService.onFinish`. Testes: RingFlowUiTest.theWholeRitualFromRingingToGoodMorning; RingFlowUiTest.theConcluirCaptionIsPartOfItsKey; PortugueseUiTest.listAndEditor (indireto).
- **F57 Bom dia fecha sozinha em 10 min.** `AlarmRingService` (`GOOD_MORNING_MS`). Testes: AlarmRingServiceTest.bomDiaClosesByItselfAfterTenMinutes.
- **F58 Bom dia oferece pular os outros alarmes matinais da próxima hora.** `session/PreAlarmPlanner.kt` (`companions`), `GoodMorningScreen.kt`, `SkipConfirmScreen`. Testes: PreAlarmPlannerTest.bomDiaOffersTheOtherMorningAlarmsDueWithinTheHour.
- **F59 Voltar fecha a tela Bom dia.** `RingActivity.kt`. Testes: sem teste direto.
- **F60 Ver estatísticas da semana (histórico de 7 dias).** `ui/stats/StatsScreen.kt`, `StatsViewModel.kt`, `WakeHistoryRepository.observeLastDays`. Testes: StatsUiTest.withNoWakeUpsYet; StatsUiTest.aWeekOfWakeUps; StatsUiTest.wakingUpAfterMidnightCountsTheRightDelay; AlarmListUiTest.theStatsButtonOpensTheStats; RingFlowUiTest.seeStatsOpensTheStatsScreen.
- **F61 Link de privacidade nas estatísticas.** `StatsScreen.kt` (`privacy_url`). Testes: StatsUiTest.thePrivacyPolicyOpensInTheBrowser; StatsUiTest.withoutABrowserThePrivacyLinkDoesNothing; PortugueseUiTest.thePrivacyLinkOpensThePortuguesePage.

### Permissões e avisos

- **F62 Onboarding de permissões no primeiro uso.** `ui/permissions/PermissionsScreen.kt`, `WwuNavGraph.kt`, `MainActivity.kt` (`shouldShowOnboarding`). Testes: PermissionsUiTest.firstLaunchWalksThroughThePermissionsOnce; PermissionsUiTest.withEverythingAllowedTheAppOpensStraightOnTheList; AppSetupTest.everyPermissionAnAlarmNeedsIsDeclared.
- **F63 Notificações pedidas no próprio app (só na primeira vez).** `AlarmPermissions.kt`, `PermissionsScreen.kt`. Testes: PermissionsUiTest.notificationsAreAskedInTheAppOnlyTheFirstTime.
- **F64 Outras permissões abrem a página de configurações do sistema.** `PermissionsScreen.kt`. Testes: PermissionsUiTest.theOtherPermissionsOpenTheirSettingsPages.
- **F65 Permissão concedida nas configurações acende ao voltar.** `PermissionsScreen.kt`. Testes: PermissionsUiTest.aPermissionGrantedInSettingsLightsUpOnReturn.
- **F66 Lê as permissões do sistema (exato, tela cheia, notificações).** `AlarmPermissions.isGranted/allGranted`. Testes: PermissionsUiTest.alarmPermissionsAreReadFromTheSystem.
- **F67 Bateria: só recomendada.** `AlarmPermissions` (BATTERY, required=false). Testes: PermissionsUiTest.batteryIsOnlyRecommended.
- **F68 Aviso na lista de permissões faltando; toque leva às permissões e volta com um toque.** `AlarmListScreen.kt`, `WwuNavGraph.kt` (`fromList`). Testes: ListWarningsUiTest.missingPermissionsShowUnderTheNextDisplay; ListWarningsUiTest.theWarningOpensThePermissionsAndOneTapComesBack.
- **F69 Aviso de volume do alarme baixo, com ajuste.** `AlarmListViewModel.alarmVolume/turnUpAlarmVolume`, `AlarmListScreen.kt`. Testes: ListWarningsUiTest.aLowAlarmVolumeIsFlaggedAndTurnedUp; ListWarningsUiTest.theVolumeOnlyMattersWhileAnAlarmIsOn; ListWarningsUiTest.aLoudEnoughPhoneGetsNoVolumeWarning.

### Sistema: reagendamento, persistência, idioma

- **F70 Alarmes reagendados após reinício do celular.** `session/BootReceiver.kt`, `AlarmRepository.rescheduleAll`. Testes: AlarmReceiverTest.afterARebootEveryAlarmThatIsOnIsArmedAtItsRealNextRing; AlarmRepositoryTest.rescheduleAllArmsEveryAlarmThatIsOnAtItsRealNextRing.
- **F71 Alarmes reagendados ao trocar de fuso horário.** `BootReceiver.kt`. Testes: AppSetupTest.aNewTimeZoneMovesTheAlarmsToTheNewLocalTime.
- **F72 Alarmes reagendados após atualização do app ou force stop (ao abrir).** `MainActivity.rearmAlarmsOnce`, `BootReceiver`. Testes: AppSetupTest.anAppUpdateReschedulesEverything; AppSetupTest.otherBroadcastsDontTouchTheAlarms; AlarmRepositoryTest.theStartupRescheduleLeavesARingThatMayStillBeOnItsWayAlone.
- **F73 Toque interrompido (processo morto, reinício do celular) continua tocando.** `session/RingWatchdog.kt`, `AlarmRingService.onRingStart`, `BootReceiver`. Testes: AlarmRingServiceTest.aRingingSessionIsWrittenDownAndWatched; AlarmRingServiceTest.aSessionTheProcessLostRingsAgainWhereItWas; AlarmRingServiceTest.aStickyRestartWithNoIntentRingsAgain; AlarmRingServiceTest.restartingThePhoneDoesNotEndARingingAlarm.
- **F74 Migração do banco (dados mantidos; amostras antigas removidas; pausa vira pulo).** `data/AppDatabase.kt`. Testes: DatabaseMigrationTest.upgradingFromVersion4KeepsTheUsersDataAndDropsTheSamples; DatabaseMigrationTest.upgradingFromVersion5TurnsAPausedDayIntoASkip.
- **F75 Backup do Android (dados extraídos e backup completo).** `res/xml/backup_rules.xml`, `data_extraction_rules.xml`. Testes: sem teste direto (a restauração é tratada pelo reagendamento de F72).
- **F76 Textos em português e inglês.** `res/values/strings.xml`, `values-pt/strings.xml`. Testes: StringsTest.everyStringExistsInBothLanguages; StringsTest.placeholdersMatch; StringsTest.dayArraysHaveSevenEntries; StringsTest.shortDayNamesFitADayKey; PortugueseUiTest.theStringsArePortuguese.
- **F77 Tema escuro fixo, independente do sistema.** `MainActivity` (`enableEdgeToEdge`), `ui/theme/Theme.kt`. Testes: sem teste direto.
- **F78 Canais de notificação (toque silencioso no canal do alarme, pergunta sem som).** `WakeWakeUpApplication.createNotificationChannels`. Testes: AppSetupTest.theAlarmNotificationChannelRingsSilently; AppSetupTest.thePreAlarmChannelPopsUpWithoutASound; AppSetupTest.componentsAreWiredUp.
- **F79 Componentes do manifesto registrados.** `AndroidManifest.xml`. Testes: AppSetupTest.componentsAreWiredUp.
- **F80 Fuso e horário do Brasil nos testes de horário.** Testes: AlarmTimingTest.useBrazilTime; AlarmTimingTest.oneOffLaterTodayRingsToday; AlarmTimingTest.oneOffAlreadyPastRingsTomorrow; AlarmTimingTest.theMinuteAnAlarmFiresIsNotScheduledAgain; AlarmTimingTest.weeklyAlarmThatAlreadyRangWaitsAWeek; AlarmTimingTest.nextTriggerDayMatchesTheTrigger.

### Site (fora do app; sem testes automatizados)

- **F81 Baixar o app.** `site/main.js` (`data-download`, modos store/apk/ios/desktop), `site/site-config.js` (`storeUrl`, `apkUrl`), `site/wake-wake-up.apk`. Testes: nenhum.
- **F82 Trocar idioma PT/EN e redirecionar pelo idioma do navegador.** `site/lang.js` (chave `wwu-lang` no localStorage). Testes: nenhum.
- **F83 Relógio em flip na capa.** `site/main.js` (`tickClock`, `Flip`). Testes: nenhum.
- **F84 Vídeo de demonstração com botão de play.** `site/main.js` (`playButton`, `video`). Testes: nenhum.
- **F85 Demonstração do volume com rampa (interruptor).** `site/main.js` (`rampSwitch`, `paintVolume`). Testes: nenhum.
- **F86 Demonstração do Espere (segurar o botão).** `site/main.js` (`espere`, `holdTimer`). Testes: nenhum.
- **F87 Demonstração da calculadora (checar a resposta, teclado).** `site/main.js` (`press`, `paintCalc`, `shake`). Testes: nenhum.
- **F88 Demonstração de escolha de som (dial de estações).** `site/main.js` (`STATIONS`, `pickStation`, teclado). Testes: nenhum.
- **F89 Gráfico de exemplo "minutos até acordar".** `site/main.js` (`paintChart`, `MINUTES`). Testes: nenhum.
- **F90 Animação Bom dia (12 minutos).** `site/main.js` (`bomdia`). Testes: nenhum.
- **F91 Entrada das seções ao rolar.** `site/main.js` (IntersectionObserver). Testes: nenhum.
- **F92 Teste no Android (pedir acesso de teste por e-mail).** `index.html` (`#teste`, `.tester`). Testes: nenhum.
- **F93 Link de contato no rodapé.** `site/main.js` (`config.contact`). Testes: nenhum.

Total de funções numeradas: 93 (F01 a F93), sendo 80 do app e 13 do site (F81 a F93). Quatro do app não têm teste direto (F08, F59, F75, F77, e F12 é coberta por F01).

## 6. Fluxos principais, passo a passo

**Criar alarme.** Lista > "+" (`onNewAlarm`) > `editViewModel.startNew()` > editor (hora, dias, rótulo, tarefa, som, rampa) > Salvar > `EditAlarmViewModel.save` > `AlarmRepository.save` (grava no Room, `scheduler.schedule` com `setAlarmClock`, `onChanged` > `PreAlarmCheck.reschedule`) > volta à lista.

**Alarme tocar até Bom dia.**
1. `AlarmReceiver` recebe o disparo, busca o alarme, ignora se desligado ou pulado hoje, arma o próximo toque (`armNextRing`), inicia `AlarmRingService` (foreground).
2. `AlarmRingService.onAlarmFired`: desliga o alarme se for de uma vez, zera o Espere, cancela a oferta "Já acordou?", inicia a sessão, WakeLock, abre `RingActivity`, toca o som com rampa (15% em 1 minuto), salva em `RingWatchdog`, inicia o temporizador de 1 s.
3. Tela do toque: "Começar missão" (`onStartMission`: para o som, mostra a missão) ou "Espere" (`onHoldOn`: silêncio por 5 min, conta a espera).
4. Missão: matemática (N problemas, calculadora) ou frase do dia (digitar). "Não consigo resolver" troca para frase. Se o tempo de 2 min acabar, volta a tocar a 80%.
5. Último acerto > `finishSession`: grava o despertar no histórico, encerra o WakeLock e o foreground, mostra Bom dia com a duração e as esperas.
6. Bom dia: oferta de pular outros alarmes da hora (se houver), Concluir ou 10 min > `onFinish` (limpa sessão, reagenda a checagem "Já acordou?", `stopSelf`).

**Pular um dia.** Lista > cartão do alarme recorrente > desligar > gaveta "pular só quarta" (`SKIP_PROMPT`) > `AlarmListViewModel.skipNext` > `AlarmRepository.skipNext` grava `skipDate` = dia do próximo toque, o alarme fica ligado. No dia pulado, `AlarmReceiver` não toca e arma o toque seguinte. Desfazer: `clearSkip`. Alarme de uma vez: vira desligado (`setEnabled(false)`).

**"Já acordou?" (pular um toque pela notificação).**
1. `PreAlarmCheck.reschedule` agenda a checagem 1 h antes do primeiro alarme matinal (hora 3 a 11). Só oferece se o telefone estiver em uso e o app não foi aberto há 90 min.
2. A notificação "Já acordou?" tem a ação de pular. A ação abre `SkipConfirmScreen` (`ACTION_CONFIRM_SKIP`, extras de ids e instantes) via `MainActivity`.
3. `SkipConfirmViewModel.load` valida que cada toque ainda é o próximo. Se não, estado STALE. Confirmar pede uma conta. `confirm` chama `AlarmRepository.skipOccurrence` (pula só aquele toque; de uma vez: desliga).
4. Recibo (`postReceipt`) com ação de desfazer > `undoSkipOccurrence` devolve o toque. Só funciona se o toque tiver mais de 1 min.

**Reinício do celular durante um toque.** `BootReceiver` (BOOT_COMPLETED) > `rescheduleAll()` > `RingWatchdog.resumeIfInterrupted` (lê `ring_session`, idade máxima 3 h) > `AlarmRingService` com `EXTRA_RESUMED_SESSION`. A sessão volta com `startedAt`, esperas e pausa do Espere preservados.

**Processo morto durante um toque.** O watchdog (`AlarmManager`, a cada 3 min) chama `AlarmReceiver` com `ACTION_RING_WATCHDOG` > `RingWatchdog.onWake`: se a sessão ainda existe no processo, rearma; senão tenta retomar.

**Rádio que falha.** `AlarmRingService.startStream` toca o stream HTTPS. Se não começar em 8 s (`STREAM_TIMEOUT_MS`), troca para o som padrão do sistema, ou para o sino incluído (`campainha_mecanica`) se não houver. Se o rádio tocar mas ninguém reagir por 3 min (`RADIO_GRACE_MS`), o alarme assume em volume total. Busca na tela de rádio: sem rede mostra "sem internet" com botão "tentar de novo"; resposta vazia mostra "sem resultados"; resposta inválida é erro.

**Primeiro uso.** `MainActivity` > `AlarmPermissions.shouldShowOnboarding` > `permissions` (inicial) > "Continuar" > `list`. Se já tudo permitido, abre direto na lista. Aviso na lista se faltar permissão (`fromList=true`).

**Estatísticas.** Lista > ícone (só com alarmes) > `StatsViewModel` > `observeLastDays(7)`. Ao concluir cada toque, `WakeHistoryRepository.record`.

## 7. O site (`site/`)

Arquivos:
- `index.html` (PT, raiz) e `en/index.html` (EN). Mesma estrutura de seções. Âncoras: `#baixar` (download), `#como-funciona`, `#teste`.
- `privacidade.html` (PT) e `en/privacy.html` (EN).
- `404.html`, `robots.txt`, `sitemap.xml`, `.htaccess`, `README.md`.
- `styles.css` (346 linhas), `assets/` (ícones, fontes woff2 Barlow Condensed e Instrument Sans, poster do vídeo), `og-image.png`, `favicon.ico`.
- Arquivos de download: `wake-wake-up.apk` (binário no repositório) e `wake-wake-up-video.mp4`.

Seções da página inicial (PT):
1. `header.hero` com nav (logo, "BAIXAR", PT/EN), relógio flip e título "Acorde de verdade.".
2. `section` "Veja uma manhã com o Wake Wake Up." (vídeo `#demo-video` com botão de play).
3. `section#como-funciona` "Três passos até levantar." (demos: Espere `#espere`, rampa `#ramp-switch` e medidor `#vol-segs`, calculadora `#calc`, Bom dia `#bomdia-digits`).
4. `section` "Acorde com o som que funciona para você." (dial de estações `#stations`, `#station-name`).
5. `section` "Quanto tempo você leva para acordar." (gráfico `#chart`, exemplo).
6. `section.final#baixar` "Bom dia, de verdade." (botões de download `[data-download]`, notas `data-apk-note`, `data-desktop-note`, `data-ios-note`).
7. `div#teste` "Ajude a testar" (revela o e-mail de acesso de teste).
8. `footer` com `#contact-link`.

Scripts:
- `site-config.js`: `window.WWU_CONFIG` com `storeUrl` (Play Store, vazio), `apkUrl` ("wake-wake-up.apk"), `contact` (e-mail do rodapé, vazio = oculto). Carregado antes de `main.js`.
- `lang.js`: carregado no `<head>`, antes do desenho. Lembra a escolha PT/EN no `localStorage` (`wwu-lang`), redireciona pelo idioma do navegador (`pt*` > PT, outros > EN) só na primeira visita de fora do site, nunca para robôs. Mantém o `#hash`.
- `main.js`: textos em PT e EN (objeto `T`); decide o modo de download (store / apk / ios / desktop) e mostra as notas; relógio flip (`tickClock`); vídeo com play; demonstrações interativas (rampa, Espere, calculadora com teclado, dial de estações, gráfico, Bom dia); animação de entrada por `IntersectionObserver`; respeita `prefers-reduced-motion`.

Surpresa: o e-mail do rodapé (`cyberhat.tech@gmail.com`, em `site-config.js`) é diferente do e-mail de acesso de teste (`vinicostamaga@outlook.com`, em `index.html`). Vale confirmar que são os dois desejados.

## 8. Como rodar, compilar e testar

Ambiente (Git Bash, na raiz do checkout):

```
export ANDROID_HOME="C:\\Users\\vinic\\AppData\\Local\\Android\\Sdk"
export JAVA_HOME="C:\\Program Files\\Android\\Android Studio\\jbr"
./gradlew.bat <tarefas> --console=plain
```

O aviso "sdk.dir ... Directory does not exist" é esperado e inofensivo. Builds levam minutos: usar timeout alto ou rodar em segundo plano.

- Compilar debug: `./gradlew.bat :app:assembleDebug --console=plain`
- Compilar release (precisa de `keystore.properties` na raiz; sem ele, sai sem assinatura): `./gradlew.bat :app:assembleRelease --console=plain`
- Suíte completa de testes (JVM, Robolectric): `./gradlew.bat :app:testDebugUnitTest --console=plain`
- Testes de uma classe: `./gradlew.bat :app:testDebugUnitTest --tests "com.wakewakeup.session.AlarmRingServiceTest" --console=plain`
- Prints das telas (`ScreenshotTour`, só roda com a variável `WWU_SCREENSHOTS` definida; sem ela o teste é ignorado): `WWU_SCREENSHOTS=1 ./gradlew.bat :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour" --console=plain`. Saída em `app/build/screenshots/`, com prefixos `pt`, `en`, `pequeno` (tela pequena) e `fonte-grande`.
- Não há emulador nem celular neste PC. Os prints saem do ScreenshotTour.

Site: sem build. Abrir `site/index.html` no navegador (lang.js ignora `file:`), ou servir a pasta `site/` com qualquer servidor estático.

## 9. Resumo e surpresas

- Telas: 11 (8 rotas + 3 telas do toque em `RingActivity`).
- Funções: 93 (80 do app, 13 do site). 4 funções do app sem teste direto (F08, F59, F75, F77).
- Pontos de entrada: 2 activities (`MainActivity`, `RingActivity`), 2 serviços (`AlarmRingService`, e o watchdog como alarme), 3 receivers (`AlarmReceiver`, `PreAlarmReceiver`, `BootReceiver`), 8 rotas de navegação.
- Surpresas:
  1. O alarme rearma-se em três momentos: no boot (`BootReceiver`), ao abrir o app (`MainActivity.rearmAlarmsOnce`) e a cada 3 min enquanto toca (`RingWatchdog`).
  2. A pergunta "Já acordou?" só existe para alarmes com hora entre 3 e 11, e nunca durante um toque.
  3. Alarme de uma vez se desliga ao começar a tocar, não só ao concluir (F33).
  4. Bom dia fecha sozinha em 10 min, mas o Espere não tem limite de uso (conta de vezes, e o site diz "SEM LIMITE").
  5. Testes de horário usam fuso do Brasil (`AlarmTimingTest.useBrazilTime`).
  6. `ScreenshotTour` é ignorado sem `WWU_SCREENSHOTS`, então a suíte padrão não gera prints.
  7. Dois e-mails de contato diferentes no site (ver seção 7).
  8. O APK e o vídeo estão versionados em `site/`.
  9. A migração 4 para 5 apaga dados de amostra, e só as versões iniciais de desenvolvimento usam recriação destrutiva.
