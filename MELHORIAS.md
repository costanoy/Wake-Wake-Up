# Melhorias: Wake Wake Up

Plano da Fase 4 (planejador), feito em 2026-11-26 sobre a branch `melhorias-claude`. TIPO: PRONTO. MODO: AUTOMÁTICO. LIBERDADE: ALTA.
Entradas: os 64 itens confirmados em `.auditoria/auditoria-*.md`, `orcamento.md`, `performance-antes.md`, `funcoes.md`, `skills.md` e `assets/audio/audio_manifest.md`. Regra visual única: `GUIA-VISUAL.md`.

## Resumo

- **64 itens confirmados, todos aproveitados** (alguns em parte): 21 P0, 29 P1, 14 P2. Mais 5 itens do que falta para a Google Play (PLAY-01 a PLAY-05).
- **P0** (bug, alarme que não toca ou para sem missão, orçamento estourado): LOG-01 a LOG-09, LOG-11, TEC-01 a TEC-09, TEC-11, SITE-01 (a parte que não depende da chave).
- **P1** (impacto alto em uso e produto): LOG-10, LOG-12, TEC-10, UX1-01 a UX1-09, UX2-01 a UX2-09, SITE-02 (parte do README), SITE-03, 04, 05, 07, 08, 09, 10, PLAY-01, PLAY-02, PLAY-03.
- **P2** (polimento): LOG-13 (parte), TEC-12, UX1-10 a UX1-13, UX2-10 a UX2-13, SITE-06 (preparo), SITE-11, 12, 13, PLAY-05.
- **9 lotes independentes** (7 do app com Gradle, 1 do site, 1 de arte no Canva). Dois lotes nunca mexem no mesmo arquivo.
- **Direct Boot (LOG-01/TEC-01) entra agora**, do jeito mais seguro: nada muda com o celular desbloqueado; o banco não muda de lugar. Só se valida de verdade no aparelho (lista em "Sugestões que precisam do Vinicius").
- **Versão depois desta revisão: `versionCode = 3`, `versionName = "1.2"`** (Lote 3). O site usa o mesmo número (Lote 8).
- **Orçamento**: nenhum lote adiciona dependência, fonte ou imagem ao app. APK release no máximo 4.269.850 B (linha de base 4.066.524 + 5%).

### Regras que valem para todos os lotes

1. **Lista fechada de arquivos.** Cada lote só cria ou altera os arquivos listados na sua seção. Se uma correção pedir um arquivo de outro lote, não mexa: registre no relatório do lote o que faltou.
2. **Teste de outro lote quebrou por causa da sua mudança?** Não edite o teste. Tente manter o comportamento que ele confere; se for impossível, registre no relatório o nome do teste, o motivo e a correção sugerida.
3. **Strings.** `res/values/strings.xml` e `res/values-pt/strings.xml` são só do Lote 7. Os outros lotes:
   - criam strings NOVAS no arquivo próprio indicado (`strings_<lote>.xml`, em `values/` e em `values-pt/`, com as mesmas chaves e os mesmos `%1$s`);
   - antes de criar uma chave, conferem com `grep -rn 'name="<chave>"' app/src/main/res` que ela não existe (chave repetida quebra a compilação depois da junção);
   - nunca apagam nem renomeiam uma string existente: só deixam de usar (as órfãs saem na junção);
   - quando uma string existente precisa mudar de sentido ou de número de argumentos, criam uma chave nova e deixam a antiga sem uso.
4. **Tema e componentes.** `ui/theme/*` e `ui/components/*` são só do Lote 3. Precisa de algo novo? Crie `private` no arquivo da própria tela, com os tokens do `GUIA-VISUAL.md`.
5. **`ScreenshotTour.kt`** é do Lote 4. Os outros lotes rodam o tour para ver as próprias telas, mas não o editam. Por isso cada lote de tela mantém os textos e descrições que o tour procura (listados na seção do lote).
6. **Gradle**: sempre `bash ferramentas/gradle.sh ...`, cada lote no seu worktree (dois Gradle na mesma pasta brigam). Lint: o erro `NonObservableLocale` em `Labels.kt:74` é da linha de base e só o Lote 3 corrige; nenhum lote pode criar erro de lint novo.
7. **Commits em português**, um por item ou grupo pequeno de itens, citando o ID. Nada de `git push`.
8. **Sem dependência nova.** Sem fonte nova. Sem bitmap novo no app.
9. **Design**: todo lote que mexe em tela usa a skill `impeccable` do projeto (`.claude/skills/impeccable`) para criar e, antes de dar por pronto, para criticar (craft-floor e critique-rubric) os prints das quatro variantes. Não rode `scripts/impeccable` (baixa um binário): a skill funciona só com a leitura dos arquivos.
10. **Onde está o detalhe de código.** Cada item abaixo resume o que fazer e aponta o item da auditoria com o passo a passo completo. Quando esta página disser diferente da auditoria, vale esta página.

## Tabela de lotes

| id | nome | prioridade máxima | itens | usa Gradle? |
|---|---|---|---|---|
| 1 | Motor do toque | P0 | LOG-03, TEC-02, LOG-04, TEC-03, LOG-02 (serviço), LOG-06, LOG-01/TEC-01 (hora do toque), LOG-10, UX2-02, UX2-11 (cor, serviço) | sim |
| 2 | Agendamento, boot e partida | P0 | LOG-01/TEC-01 (boot), LOG-08, LOG-05, LOG-07, LOG-09, LOG-11, TEC-06, TEC-04 (repositório, aviso, partida), TEC-11, TEC-10, LOG-12 (app), LOG-13 (parte), UX2-11 (cor, aviso) | sim |
| 3 | Sistema visual, componentes e versão | P0 | TEC-08 (ícone), TEC-09, UX2-08, UX2-05 (tipos), PLAY-02, TEC-12 (interruptor) | sim |
| 4 | Lista, permissões e editor | P0 | TEC-07, TEC-04 (lista), UX1-01, UX1-02, UX1-05, UX1-06, UX1-07, UX1-08, UX1-09, UX1-10, UX1-13 | sim |
| 5 | Telas do toque, da missão e do Bom dia | P0 | TEC-05, LOG-02 (teclas), LOG-01/TEC-01 (telas), UX2-01, UX2-04, UX2-05 (telas), UX2-07, UX2-10, UX2-12, UX2-13 | sim |
| 6 | Som, rádio, estatísticas e confirmar pulo | P1 | UX2-03, LOG-12 (teste), UX1-03, UX1-04, UX2-09, UX2-05 (pulo), UX1-12, TEC-12 (som e rádio) | sim |
| 7 | Textos e ficha da loja | P1 | UX2-06, UX1-02 (texto), UX1-08 (texto), UX2-09 (texto), PLAY-01, PLAY-03 (formulário), UX2-11 (textos), UX1-11 | sim |
| 8 | Site | P0 | TEC-08 (fontes), SITE-01 (parte), SITE-03, SITE-04, SITE-07, SITE-05, SITE-08, SITE-09, SITE-10, PLAY-03 (política), SITE-02 (parte), SITE-06 (preparo), SITE-11, SITE-12, SITE-13 (parte) | não |
| 9 | Arte de divulgação | P2 | PLAY-05 | não |

Arquivos de strings novas: Lote 3 `strings_sistema.xml`, Lote 4 `strings_telas.xml`, Lote 5 `strings_toque.xml`, Lote 6 `strings_som.xml` (sempre em `app/src/main/res/values/` e `values-pt/`). Lotes 1 e 2 não precisam de string nova.

---

## Lote 1: Motor do toque

O que toca, quando toca e o que encerra o toque. Prioridade máxima P0.

**Arquivos que o lote pode mexer** (caminhos a partir de `app/src/`):
- `main/java/com/wakewakeup/session/AlarmRingService.kt`, `AlarmSessionState.kt`, `AlarmReceiver.kt`, `RingWatchdog.kt`, `MissionGenerator.kt`
- `test/java/com/wakewakeup/session/ServiceLogicBugTest.kt`, `SessionBugTest.kt`, `RingResilienceBugTest.kt`, `AlarmRingServiceTest.kt`, `AlarmRingServiceEdgeTest.kt`, `AlarmReceiverTest.kt`
- arquivo de teste novo, se precisar: `test/java/com/wakewakeup/session/RingLockedTest.kt`

**Contratos que este lote NÃO pode quebrar** (outros lotes dependem deles):
- Assinaturas públicas de `RingWatchdog` (`save`, `clear`, `saved`, `onWake`, `resumeIfInterrupted`), de `AlarmSessionState` e do `companion` do `AlarmRingService` (`ACTION_*`, `sendAction`, `sendConfirm`, `sendTypedText`, `EXTRA_*`).
- Campos de `RingSession` e `MissionState`: não remover nem renomear.
- **Invariante usado pelo Lote 5**: uma sessão recém-criada tem `mission == null`; a tela `RINGING` só tem `mission != null` depois que uma missão estourou o tempo.

**Itens, em ordem**

1. **LOG-03 + TEC-02 (P0): falha no banco deixa o alarme mudo; dois alarmes no mesmo minuto se misturam.** Detalhe: `auditoria-logica.md` LOG-03 e `auditoria-tecnica.md` TEC-02.
   - `AlarmReceiver.onReceive`: leitura do alarme em `try/catch (e: Exception)` (relançar `CancellationException`). Se a leitura falhar: logar e iniciar o serviço mesmo assim com o `alarmId`. Se der certo: manter as checagens (`null`, `enabled`, `skippedToday`) e trocar a ordem para **primeiro** `ContextCompat.startForegroundService(...)` (em `try/catch (e: IllegalStateException)`) e **depois** `runCatching { repository.armNextRing(alarm) }`. Com `skippedToday`: armar o próximo e sair sem tocar, como hoje.
   - `AlarmRingService.onAlarmFired`: (a) ler com `try/catch`; se a leitura lançar exceção, usar `Alarm(id = alarmId, hour = LocalTime.now().hour, minute = LocalTime.now().minute, label = "", days = emptySet(), enabled = false)`; se vier `null` sem erro (alarme apagado), manter o comportamento de hoje. (b) Depois da leitura, **sem nenhuma suspensão no meio**: `currentAlarm`, `holdUntilMillis = null`, `goodMorningTimeout?.cancel()`, `AlarmSessionState.start(...)`, `acquireWakeLock()`, Espere retomado **ou** `startAudio(...)` **antes de** `startRingActivity()`, `refreshNotification()`, `RingWatchdog.save(...)`, `startTicker()`. (c) Só depois a faxina, em `container.appScope.launch { }` e com cada passo protegido: `runCatching { container.preAlarm.cancelOffer() }` e, para alarme de uma vez ligado, `setEnabled(alarm, false)`.
   - Pronto quando: `ServiceLogicBugTest.theAlarmStillRingsWhenTheDatabaseFailsAtRingTime`, `twoAlarmsInTheSameMinuteKeepLabelAndMissionTogether` e `RingResilienceBugTest.aDatabaseFailureStillStartsTheRingingService` passam sem `@Ignore`; `AlarmRingServiceTest.aOneOffAlarmTurnsItselfOffAsItRings` continua verde (dê `idle()` e espere o `appScope` se precisar); entre o começo de `onAlarmFired` e `startAudio` só existe o `getById`.
   - `@Ignore` a remover: `ServiceLogicBugTest` linhas 120 e 136; `RingResilienceBugTest` linha 39.
2. **LOG-04 + TEC-03 (P0): som escolhido sem reserva depois que começa; `prepare()` na thread principal.** Detalhe: TEC-03 passos 1 a 4 e LOG-04 passos 1 a 4.
   - Cadeia de reserva: som escolhido (arquivo) → `defaultAlarmUri()` → campainha do app (`R.raw.campainha_mecanica`). Todo player ganha `setOnErrorListener` que, se ainda for o player atual, libera e chama o próximo da cadeia no volume atual da sessão. A campainha é o fim da cadeia: o ouvinte dela só registra em log.
   - Som que não é `android.resource://`: `setDataSource` fora da thread principal, `prepareAsync()` e prazo de 3 s (`LOCAL_PREPARE_TIMEOUT_MS`) que cai no próximo da cadeia. Campainha do app continua com `prepare()` síncrono. Cancelar o prazo em `stopAudio()`.
   - Rádio: `setOnInfoListener` com prazo de `STREAM_TIMEOUT_MS` em `MEDIA_INFO_BUFFERING_START`, cancelado em `MEDIA_INFO_BUFFERING_END`.
   - `defaultAlarmUri()` dentro de `runCatching { }.getOrNull()`.
   - Fica de fora o passo 5 do TEC-03 (copiar o arquivo escolhido): ver "Fora deste plano".
   - Pronto quando: `ServiceLogicBugTest.aPlayerThatFailsWhileRingingIsReplaced` e `RingResilienceBugTest.aLocalPlayerThatErrorsMidRingIsReplacedByAnotherSound` passam; teste novo: preparo que nunca termina → em 3 s de relógio do looper há outro player em STARTED; teste novo: rádio em buffering por 8 s → alarme padrão; `grep -n "prepare()" AlarmRingService.kt` só acha o caminho da campainha; `aSoundThatCantPlayFallsBackToTheSystemAlarm`, `aBrokenSystemAlarmSoundFallsBackToTheBundledBell` e `aRadioThatDoesntStartIsReplacedAfterEightSeconds` verdes.
   - `@Ignore` a remover: `ServiceLogicBugTest` linha 149; `RingResilienceBugTest` linha 52.
3. **LOG-02, parte do serviço (P0): volume de alarme baixo.** Detalhe: LOG-02 passos 1 a 3 e 5.
   - `ensureAudible()`: piso de `ceil(max * 0.6f)` em `AudioManager.STREAM_ALARM`; guarda o volume original uma vez por sessão; tudo em `runCatching`. Chamar no começo de `startAudio()` e em `tickRinging()` quando não há Espere. Devolver o volume original em `finishSession()` e em `stopSelfSafely()`.
   - As teclas de volume são do Lote 5.
   - Pronto quando: `ServiceLogicBugTest.theAlarmStreamIsRaisedToAnAudibleFloorWhileRinging` passa; testes novos: baixar durante o toque e avançar 2 s devolve o piso; concluir a missão devolve o volume original.
   - `@Ignore` a remover: `ServiceLogicBugTest` linha 185.
4. **LOG-06 / BUG-1 (P0): ações que chegam na tela errada.** No começo de cada tratador, sair se a tela não for a certa: `onHoldOn` e `onStartMission` só em `RINGING`; `onMissionKey`, `onMissionSetTyped`, `onMissionConfirm` e `onGiveUp` só em `MISSION`. Manter as condições que já existem.
   - Pronto quando: `SessionBugTest.anEspereTapArrivingDuringTheMissionIsIgnored`, `ServiceLogicBugTest.confirmingTwiceRecordsOneWakeUp`, `anEspereTapOnGoodMorningDoesNotRearmTheWatchdog` e `giveUpOnTheRingingScreenDoesNotCreateAMission` passam.
   - `@Ignore` a remover: `SessionBugTest` linha 48; `ServiceLogicBugTest` linhas 84, 98 e 112.
   - **Exceção explícita no `SessionBugTest.kt`**: o teste `changingTheClockByHandRearmsTheAlarms` (BUG-2, linha 71) é do Lote 2, que o recria em `TimeChangeRearmTest.kt`. Este lote **apaga esse método** do `SessionBugTest.kt` (não tire o `@Ignore`: aqui ele falharia).
5. **LOG-01 / TEC-01, parte da hora do toque (P0): tocar com o celular reiniciado e ainda bloqueado.** Com o usuário bloqueado, o banco e as preferências comuns não abrem. O Lote 2 rearma os alarmes nesse estado; este lote garante que o toque acontece.
   - `RingWatchdog.prefs`: usar o armazenamento protegido do aparelho: `val store = context.createDeviceProtectedStorageContext()`; se `UserManagerCompat.isUserUnlocked(context)`, `runCatching { store.moveSharedPreferencesFrom(context, PREFS) }` (roda uma vez, não faz nada depois); devolver `store.getSharedPreferences(PREFS, MODE_PRIVATE)`.
   - Com `!UserManagerCompat.isUserUnlocked(this)`: o `AlarmReceiver` não chama `armNextRing`; o `AlarmRingService` não chama `cancelOffer`, `setEnabled` nem grava o histórico em `finishSession` (só registra em log). A leitura do alarme falha e cai no alarme mínimo do item 1: toca a campainha do app com a missão padrão. Esse é o comportamento desejado.
   - Pronto quando: teste novo (`RingLockedTest`) com `shadowOf(context.getSystemService(UserManager::class.java)).setUserUnlocked(false)` e o banco fechado: entregar o broadcast do alarme cria sessão, um player chega a STARTED, concluir a missão não lança exceção e `RingWatchdog.saved(...)` volta a `null`. Suíte verde.
   - **Validar no celular** (não dá na JVM): ver a lista em "Sugestões que precisam do Vinicius", item 5.
6. **LOG-10 (P1): segundo alarme durante o ritual do primeiro.** Detalhe: LOG-10. Em `onAlarmFired`, com sessão de outro alarme aberta e sem retomada: tela `MISSION` → não reiniciar nada (só a faxina do alarme novo); tela `RINGING` → sessão nova herdando `startedAtMillis`, `waits`, `alarmHour` e `alarmMinute` da anterior (e `mission = null`); `GOOD_MORNING` → sessão nova.
   - Pronto quando: `ServiceLogicBugTest.aSecondAlarmKeepsTheWaitsOfTheFirst` e `aSecondAlarmDuringTheMissionDoesNotResetIt` passam; `AlarmRingServiceTest.aSecondAlarmDuringEspereCanBeSnoozedToo` ajustado (depois do segundo alarme `waits` continua 1).
   - `@Ignore` a remover: `ServiceLogicBugTest` linhas 161 e 174.
7. **UX2-02 (P1): o prazo da missão passa a ser por conta.** Em `onMissionConfirm`, no `copy` da próxima conta, acrescentar `secondsLeft = MISSION_TOTAL_SECONDS`. A frase continua com 120 s no total.
   - Pronto quando: teste novo em `AlarmRingServiceTest` (3 contas, esperar 30 s, acertar a primeira, `secondsLeft == MISSION_TOTAL_SECONDS`); `theTimerCountsDownOncePerSecond`, `givingUpSwitchesToThePhraseAndKeepsTheClockRunning` e `aTimedOutMissionStartsOverFromTheFirstProblem` verdes.
8. **Contrato do invariante (P1, apoio ao UX2-01 do Lote 5).** Teste novo em `AlarmRingServiceTest`: sessão nova tem `mission == null`; depois do estouro de tempo a tela é `RINGING` com `mission != null`; depois de `ACTION_START_MISSION` a tela é `MISSION`. Nenhuma mudança de código além de não quebrar isso.
9. **UX2-11, parte da cor (P2).** Nos dois `NotificationCompat.Builder` do `AlarmRingService` (`buildNotification` e `resurface`): `.setColor(0xFFE8663C.toInt())`.

**Skills**: `anr-audit-and-fix`, `android-intent-security`, `testing-setup`.

**Verificação**
```bash
bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.session.*"
bash ferramentas/gradle.sh :app:testDebugUnitTest
bash ferramentas/gradle.sh :app:lintDebug          # só o erro de base (Labels.kt:74) pode aparecer
grep -rn "@Ignore" app/src/test/java/com/wakewakeup/session/ServiceLogicBugTest.kt app/src/test/java/com/wakewakeup/session/RingResilienceBugTest.kt app/src/test/java/com/wakewakeup/session/SessionBugTest.kt   # nada
```

**Cuidados de orçamento**: o som começa antes da tela (item 1); nada de disco ou rede na thread principal no caminho do toque (item 2); nenhum wake lock novo; o `ensureAudible()` a cada segundo é uma leitura barata, não um laço novo (usa o `tickRinging` que já existe).

---

## Lote 2: Agendamento, boot e partida

O que arma os alarmes, o que os rearma (boot, relógio, restauração, abertura do app) e o aviso "Já acordou?". Prioridade máxima P0.

**Arquivos que o lote pode mexer** (a partir de `app/src/`):
- `main/AndroidManifest.xml` (único lote que mexe no manifesto)
- `main/java/com/wakewakeup/MainActivity.kt`, `WakeWakeUpApplication.kt`, `AppContainer.kt`
- `main/java/com/wakewakeup/session/BootReceiver.kt`, `AlarmScheduler.kt`, `PreAlarmCheck.kt`, `PreAlarmReceiver.kt`, `PreAlarmPlanner.kt`, novo `ArmedRings.kt`
- `main/java/com/wakewakeup/data/AlarmRepository.kt`, `AppDatabase.kt`, novo `RestoreRearmAgent.kt`
- `main/java/com/wakewakeup/ui/edit/EditAlarmViewModel.kt`
- `test/java/com/wakewakeup/AppLogicBugTest.kt`, `AppSetupTest.kt`, `AppConfigTest.kt`, `testing/AppEnv.kt`
- `test/java/com/wakewakeup/session/DirectBootBugTest.kt`, `PreAlarmCheckTest.kt`, novos `TimeChangeRearmTest.kt` e `BootRearmTest.kt`
- `test/java/com/wakewakeup/data/RestoreRearmBugTest.kt`, `AlarmRepositoryTest.kt`, `AlarmRepositoryEdgeTest.kt`
- `test/java/com/wakewakeup/ui/SkipTokenBugTest.kt`

**Contratos que este lote NÃO pode quebrar**: assinaturas públicas de `AlarmRepository`, `AlarmScheduler`, `PreAlarmCheck` (inclusive `confirmSkipIntent`, `postReceipt`, `cancelOffer`, `appSeen`, `reschedule`) e do `AppContainer`; a API pública do `AppEnv` (outros lotes usam nos testes). Este lote só **chama** `RingWatchdog` (arquivo do Lote 1), sem editar.

**Itens, em ordem**

1. **LOG-01 / TEC-01, parte do boot (P0): rearmar com o celular reiniciado e ainda bloqueado.** Desenho escolhido (diferente das duas auditorias, de propósito): o banco Room **fica onde está**; uma cópia mínima do que está armado vai para o armazenamento protegido do aparelho.
   - Manifesto: `android:directBootAware="true"` em `.session.AlarmReceiver`, `.session.BootReceiver`, `.session.AlarmRingService` e `.session.RingActivity` (não em `<application>`, `MainActivity` nem `PreAlarmReceiver`). No filtro do `BootReceiver`: `<action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />`.
   - Novo `session/ArmedRings.kt`: `object ArmedRings` com `SharedPreferences` `"armed_rings"` em `context.createDeviceProtectedStorageContext()`. Uma chave `a_<id>` por alarme armado, com um JSON (`org.json`) de `hour`, `minute`, `days` (texto "0,1,2"), `skipDate` (ou ausente) e `at` (o instante armado). Funções: `put(context, alarm: Alarm, armedAt: Long)`, `remove(context, id: Long)`, `all(context): List<Entry>` com `data class Entry(val alarm: Alarm, val armedAt: Long)`; o `Alarm` devolvido é mínimo (`label = ""`, `enabled = true`). Leitura de JSON ruim devolve a entrada ausente, nunca exceção.
   - `AlarmScheduler`: depois de um `setAlarmClock` que deu certo, `runCatching { ArmedRings.put(context, alarm, triggerAt) }`; em `cancel`, `runCatching { ArmedRings.remove(context, alarm.id) }`. A escrita da cópia nunca pode impedir o armar.
   - `BootReceiver`: acrescentar `Intent.ACTION_LOCKED_BOOT_COMPLETED` a `RESCHEDULE_ACTIONS`. Se `!UserManagerCompat.isUserUnlocked(context)` (vale para qualquer ação que chegue antes do desbloqueio): **não tocar em `app.container`**; para cada `ArmedRings.all(context)`, se for alarme de uma vez (`days` vazio) com `armedAt` já passado, pular; senão `AlarmScheduler(context).schedule(entry.alarm)`. Em seguida, se a ação for `LOCKED_BOOT_COMPLETED`, `runCatching { RingWatchdog.resumeIfInterrupted(context) }`. Com o usuário desbloqueado: comportamento de hoje, e `resumeIfInterrupted` só se `AlarmSessionState.session.value == null` (o `BOOT_COMPLETED` chega depois do `LOCKED_BOOT_COMPLETED`).
   - `PreAlarmCheck`: `reschedule`, `check`, `cancelOffer` e `appSeen` saem logo no começo se `!UserManagerCompat.isUserUnlocked(context)`.
   - `WakeWakeUpApplication` e `AppContainer`: rodam também antes do desbloqueio. Leia `AppDatabase.get`: `Room.databaseBuilder(...).build()` não abre o arquivo, então não precisa mudar. Só troque os campos do `AppContainer` para `by lazy` se achar leitura de disco no construtor.
   - Pronto quando: `DirectBootBugTest` (classe) e os dois testes LOG-01 do `AppLogicBugTest` passam sem `@Ignore`; teste novo em `BootRearmTest`: com um alarme salvo, usuário bloqueado (`ShadowUserManager.setUserUnlocked(false)`), AlarmManager esvaziado e o banco fechado, `LOCKED_BOOT_COMPLETED` deixa o alarme armado no instante certo e nada lança exceção; teste novo: desligar ou apagar o alarme remove a entrada de `ArmedRings`; `AlarmReceiverTest.afterARebootEveryAlarmThatIsOnIsArmedAtItsRealNextRing` verde.
   - `@Ignore` a remover: `DirectBootBugTest` linha 15 (classe); `AppLogicBugTest` linhas 41 e 52.
   - **Validar no celular**: ver "Sugestões que precisam do Vinicius", item 5.
2. **LOG-08 / BUG-2 (P0): relógio mudado à mão não rearma.** Manifesto: `<action android:name="android.intent.action.TIME_SET" />` no filtro do `BootReceiver`; `Intent.ACTION_TIME_CHANGED` em `RESCHEDULE_ACTIONS`. A parte opcional (contar o Espere com `elapsedRealtime`) fica de fora.
   - Teste: criar `session/TimeChangeRearmTest.kt` copiando o teste `changingTheClockByHandRearmsTheAlarms` de `SessionBugTest.kt` (linhas 71 a 82, com o que ele usar do `setUp`), sem `@Ignore`. **Não edite `SessionBugTest.kt`** (é do Lote 1, que apaga o método de lá).
   - Pronto quando: o teste novo passa; `AppSetupTest.otherBroadcastsDontTouchTheAlarms` verde.
3. **LOG-05 (P0): toque interrompido volta quando o app é aberto.** Em `MainActivity.onCreate`, depois de `rearmAlarmsOnce()`: `app.container.appScope.launch { if (AlarmSessionState.session.value == null && RingWatchdog.saved(applicationContext) != null) RingWatchdog.resumeIfInterrupted(applicationContext) }` (fora da thread principal por causa do TEC-04).
   - Pronto quando: `AppLogicBugTest.openingTheAppBringsBackAnInterruptedRing` passa (o teste pode precisar esperar o `appScope`: ajuste o próprio teste); sem sessão salva, nada é iniciado.
   - `@Ignore` a remover: `AppLogicBugTest` linha 59.
4. **LOG-07 (P0): alarme "poupado" no rearme nunca é rearmado.** Em `rearmAlarmsOnce`: trocar `lifecycleScope` por `app.container.appScope` na primeira passada. Segunda passada, 2 min 5 s depois: `Handler(Looper.getMainLooper()).postDelayed({ app.container.appScope.launch { app.container.alarmRepository.rescheduleAll() } }, 125_000L)`.
   - **Não use `delay()` dentro do `appScope` para isso**: o `AppEnv.after()` espera todos os filhos do `appScope` e cada teste que abre a `MainActivity` ficaria 2 minutos parado.
   - Pronto quando: teste novo: alarme vencido há 30 s, AlarmManager vazio, abrir a `MainActivity`, `shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(130))`, esperar o `appScope`: o alarme está armado na próxima ocorrência. `AlarmRepositoryTest` e `AlarmRepositoryEdgeTest` verdes (o repositório não muda aqui).
5. **LOG-09 (P0): editar o horário de um alarme que pula hoje mantém o pulo.** Em `EditAlarmViewModel.save`: `val scheduleChanged = draft.hour != original.hour || draft.minute != original.minute || draft.days != original.days` e salvar `draft.copy(enabled = true, skipDate = if (scheduleChanged) null else draft.skipDate)`.
   - Pronto quando: `AppLogicBugTest.editingTheTimeOfASkippingAlarmDropsTheSkip` passa; teste novo: mudar só o nome mantém o pulo; `AlarmRepositoryTest.savingKeepsASkipOnlyWhileItStillFits` verde.
   - `@Ignore` a remover: `AppLogicBugTest` linha 74.
6. **LOG-11 (P0): desfazer o pulo no último minuto é recusado em silêncio.** Em `AlarmRepository.undoSkipOccurrence`: trocar `if (at - now < ACTION_MARGIN_MS) return false` por `if (at <= now) return false`. Em `PreAlarmReceiver`: só apagar o recibo se todos os `undoSkipOccurrence` devolverem `true`.
   - Pronto quando: `AppLogicBugTest.undoingASkipJustBeforeTheRingIsAccepted` passa; `AlarmRepositoryEdgeTest.undoingASkipTooCloseToTheRingIsRefused` reescrito (a 30 s devolve `true` e arma; depois do instante devolve `false`); `PreAlarmCheckTest.theReceiptsUndoPutsTheRingBack` verde.
   - `@Ignore` a remover: `AppLogicBugTest` linha 89.
7. **TEC-06 (P0): alarmes restaurados de backup não ficam armados.** Detalhe: TEC-06. Criar `data/RestoreRearmAgent.kt` (`BackupAgent` com `onBackup` e `onRestore` vazios; `onRestoreFinished()` com `runCatching { runBlocking(Dispatchers.IO) { AlarmRepository(AppDatabase.get(this@RestoreRearmAgent).alarmDao(), AlarmScheduler(this@RestoreRearmAgent)).rescheduleAll() } }`, sem cast para `WakeWakeUpApplication`). Manifesto, em `<application>`: `android:backupAgent=".data.RestoreRearmAgent"` e `android:fullBackupOnly="true"`.
   - Pronto quando: `RestoreRearmBugTest` passa sem `@Ignore`; teste novo: alarme ligado no banco, `onRestoreFinished()`, `env.scheduledFor(alarm)` não é nulo; `AppConfigTest` verde.
   - `@Ignore` a remover: `RestoreRearmBugTest` linha 12 (classe).
   - **Validar no celular** (`bmgr`): item 5 das sugestões.
8. **TEC-04, partes do repositório, do aviso e da partida (P0, regra de orçamento).** Detalhe: TEC-04 passos 1 a 3.
   - `AlarmRepository` e `PreAlarmCheck`: parâmetro `private val io: CoroutineDispatcher = Dispatchers.IO` no construtor e o corpo de toda função `suspend` pública em `withContext(io) { }`.
   - `MainActivity`: `appSeen()` (em `onStart` e `onStop`), `reschedule()` e `cancelOffer()` passam a ser chamados por `app.container.appScope.launch { }`.
   - `WakeWakeUpApplication.onCreate`: `container.appScope.launch { if (UserManagerCompat.isUserUnlocked(this@WakeWakeUpApplication)) { getSharedPreferences("wwu_prefs", MODE_PRIVATE); getSharedPreferences("pre_alarm", MODE_PRIVATE) } }` (confira os nomes reais dos arquivos de preferências antes).
   - **Não** passe `Dispatchers.Main.immediate` como `io` em teste: dentro de `runBlocking` na thread principal isso trava. Se um teste seu depender de ordem, espere com `waitFor`/`runBlocking`.
   - A parte da lista (`AlarmListScreen.kt:156`) é do Lote 4.
   - Pronto quando: toda função `suspend` pública das duas classes começa com `withContext(io)`; `MainActivity` não lê nem grava preferência direto em `onCreate`/`onStart`/`onStop`, com uma exceção aceita: a leitura do token do item 10, só quando a intent é `ACTION_CONFIRM_SKIP`; suíte inteira verde em duas execuções seguidas.
9. **TEC-11 (P0, regra de orçamento): polling de 250 ms.** Detalhe: TEC-11. `PreAlarmCheck.waitUntilInUse` passa a esperar `ACTION_USER_PRESENT` / `ACTION_SCREEN_OFF` com `suspendCancellableCoroutine` + `withTimeoutOrNull(unlockWaitMs)`, receiver registrado no contexto da aplicação com `RECEIVER_NOT_EXPORTED`, desregistrado ao retomar e no cancelamento. Remover `UNLOCK_POLL_MS`.
   - Pronto quando: nenhum `delay(` em `waitUntilInUse`; os testes do `PreAlarmCheckTest` que simulam o desbloqueio mandam `Intent.ACTION_USER_PRESENT` e passam.
10. **TEC-10 (P1): `CONFIRM_SKIP` aceito de qualquer app.** Detalhe: TEC-10. `PreAlarmCheck.confirmSkipIntent` gera um token (`UUID`), grava em `pre_alarm` na chave `skip_token` e põe em `EXTRA_SKIP_TOKEN` (`"extra_skip_token"`). `MainActivity.skipRequestOf` devolve `null` se o token da intent for diferente do salvo (ler antes do `cancelOffer`). `cancelOffer()` apaga o token.
    - Pronto quando: `SkipTokenBugTest` passa sem `@Ignore` (linha 26, classe). `SkipConfirmUiTest` (Lote 6) e `ScreenshotTour.skipConfirm` (Lote 4) continuam passando sem edição, porque abrem a tela por `confirmSkipIntent`.
11. **LOG-12, parte do app (P1): gravação cancelada no meio.** Em `AlarmRepository`, o corpo de `save`, `delete`, `setEnabled`, `skipNext`, `clearSkip`, `armNextRing` e as partes de escrita de `skipOccurrence` e `undoSkipOccurrence` rodam em `withContext(io + NonCancellable)`. A correção do teste `SkipConfirmUiTest` é do Lote 6.
    - Pronto quando: teste novo no repositório: cancelar o Job logo depois de chamar `clearSkip` nunca deixa `skipDate` nulo com o AlarmManager ainda no dia seguinte.
12. **LOG-13, parte (P2): alarme de uma vez perdido com o celular desligado.** Só a parte barata: no `BootReceiver`, com o usuário desbloqueado e ação `BOOT_COMPLETED` (ou `LOCKED_BOOT_COMPLETED` já desbloqueado), **antes** do `rescheduleAll()`: para cada `ArmedRings.all(context)` de alarme de uma vez com `armedAt < agora - 2 min`, se não há sessão aberta desse alarme e `RingWatchdog.saved(context)?.alarmId` é outro, `setEnabled(alarm, false)` (lendo o alarme real pelo repositório). Assim ele aparece desligado na lista em vez de tocar no dia seguinte. A notificação e o "tocar agora" ficam de fora.
    - Pronto quando: teste novo: boot com toque de uma vez perdido há 2 h → alarme desligado e nada armado para ele; boot sem toque perdido → igual a hoje.
13. **UX2-11, parte da cor (P2).** Nos dois `NotificationCompat.Builder` do `PreAlarmCheck` (oferta e recibo): `.setColor(0xFFE8663C.toInt())`.

**Skills**: `android-permissions-security`, `android-intent-security`, `anr-audit-and-fix`, `testing-setup`.

**Verificação**
```bash
bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "*AppLogicBugTest*" --tests "*DirectBootBugTest*" --tests "*BootRearmTest*" --tests "*TimeChangeRearmTest*" --tests "*RestoreRearmBugTest*" --tests "*SkipTokenBugTest*" --tests "*PreAlarmCheckTest*" --tests "*AlarmRepository*"
bash ferramentas/gradle.sh :app:testDebugUnitTest      # duas vezes seguidas
bash ferramentas/gradle.sh :app:lintDebug
bash ferramentas/gradle.sh :app:assembleRelease        # o manifesto mesclado precisa compilar
```

**Cuidados de orçamento**: nada de disco na thread principal na partida (item 8); nenhum polling (item 9); alarmes só por `setAlarmClock`; nenhum wake lock novo; `ArmedRings` é uma escrita pequena por mudança de alarme, sempre fora da thread principal porque o repositório já roda em `io`.

---

## Lote 3: Sistema visual, componentes e versão

Único lote que mexe em `ui/theme/`, `ui/components/`, nos bitmaps e no `app/build.gradle.kts`. Prioridade máxima P0.

**Arquivos que o lote pode mexer**:
- `app/src/main/java/com/wakewakeup/ui/theme/*.kt` e `ui/components/*.kt`
- `app/src/main/res/drawable-nodpi/` (trocar o PNG por WebP)
- `app/src/main/res/values/strings_sistema.xml` e `values-pt/strings_sistema.xml` (novos)
- `app/build.gradle.kts`
- `app/src/test/java/com/wakewakeup/ui/LabelsLocaleBugTest.kt`, novo `app/src/test/java/com/wakewakeup/AppBudgetTest.kt`

**Contratos**: não renomear nem remover nada público de `ui/theme/` e `ui/components/` (assinaturas, tokens, `CalcKeys`); só acrescentar. `CalculatorKeypad(onKey, modifier, key)` mantém os três parâmetros.

**Itens, em ordem**

1. **TEC-08, parte do app (P0, orçamento): ícone em PNG.** Converter `drawable-nodpi/ic_launcher_foreground_art.png` para WebP sem perda com o mesmo nome de recurso: `ffmpeg -i ic_launcher_foreground_art.png -c:v libwebp -lossless 1 ic_launcher_foreground_art.webp`; apagar o `.png`. Se o WebP sem perda ficar maior que o PNG (117.515 B), usar `-quality 90` e comparar visualmente.
   - Teste: criar `AppBudgetTest.kt` com o teste `noBitmapInDrawablesIsAPng` copiado de `BudgetBugTest.kt`, sem `@Ignore`. **Não edite `BudgetBugTest.kt`** (é do Lote 8).
   - Pronto quando: nenhum `.png` em `app/src/main/res/drawable*`; `mipmap-anydpi-v26/ic_launcher.xml` segue apontando para `@drawable/ic_launcher_foreground_art`; `AppBudgetTest` passa; APK release até 4.269.850 B.
2. **TEC-09 (P0): erro de lint e data em idioma misturado.** Em `strings_sistema.xml`: `<string name="date_locale">en</string>` (EN) e `<string name="date_locale">pt-BR</string>` (PT), com um comentário XML "tag de idioma, não traduzir" e **sem** `translatable="false"` (dá erro de lint quando a chave existe nas duas pastas). Em `Labels.kt`, `panelDate`: `val tag = stringResource(R.string.date_locale)`; `val locale = remember(tag) { Locale.forLanguageTag(tag) }`; o resto fica.
   - Pronto quando: `:app:lintDebug` termina sem erro; `LabelsLocaleBugTest` passa sem `@Ignore` (linha 19, classe); `PortugueseUiTest` verde.
3. **UX2-08 (P1): tirar a tecla "−" do teclado.** Em `Keypads.kt`, a última fileira vira uma tecla só: `CalcKey("0", CalcKind.Digit, onKey, width = key * 3 + GAP_H * 2, height = key)`. Não mexer em `CalcKeys.SIGN` (os testes do serviço mandam o sinal direto). A string `cd_key_sign` fica órfã (sai na junção).
   - Pronto quando: prints 15, 16 e 21 sem a tecla "−"; `AlarmRingServiceTest`, `AlarmRingServiceEdgeTest`, `RingFlowUiTest` e `SkipConfirmUiTest` sem falha nova.
4. **UX2-05, parte dos tipos (P1): rótulos do Espere pequenos demais.** Em `Type.kt`: `espereLabel` com 16 sp e `lineHeight = 20.sp`; `espereSub` com 13 sp e `lineHeight = 16.sp`. Conferir no `EspereBar` (`Hardware.kt`) que as duas linhas cabem na etiqueta com fonte 1.3x; se não couberem, aumentar a altura mínima da etiqueta, não reduzir a fonte. (O botão da missão e as mensagens são dos Lotes 5 e 6.)
   - Pronto quando: prints `pt-13`, `en-13`, `pequeno-13` e `fonte-grande-13` mostram as duas linhas inteiras, sem corte.
5. **PLAY-02 (P1): versão do app.** Em `app/build.gradle.kts`: `versionCode = 3` e `versionName = "1.2"`.
6. **TEC-12, parte do interruptor (P2).** `Hardware.kt:791`: `.offset { IntOffset(knobX.roundToPx(), 3.dp.roundToPx()) }`. (Som e rádio são do Lote 6.)
   - Pronto quando: `AlarmListUiTest` verde; print `pt-02` sem diferença no interruptor.

**Skills**: `impeccable` (typeset e polish), `jetpack-compose-performance`.

**Verificação**
```bash
bash ferramentas/gradle.sh :app:lintDebug                       # zero erros
bash ferramentas/gradle.sh :app:testDebugUnitTest
bash ferramentas/gradle.sh :app:assembleRelease && ls -l app/build/outputs/apk/release/*.apk   # até 4.269.850 B
WWU_SCREENSHOTS=1 bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"
```
Abra os prints 13, 15, 16 e 21 das quatro variantes em `app/build/screenshots/` e confira contra o `GUIA-VISUAL.md`.

**Cuidados de orçamento**: `res/font` não muda (5 arquivos é o limite); nenhum bitmap novo; o WebP não pode ser maior que o PNG; valor animado lido em lambda.

---

## Lote 4: Lista, permissões e editor

Prioridade máxima P0 (TEC-07 e a parte de lista do TEC-04).

**Arquivos que o lote pode mexer** (a partir de `app/src/`):
- `main/java/com/wakewakeup/ui/list/AlarmListScreen.kt`, `AlarmListViewModel.kt`
- `main/java/com/wakewakeup/ui/permissions/PermissionsScreen.kt`, `AlarmPermissions.kt`
- `main/java/com/wakewakeup/ui/edit/EditAlarmScreen.kt` (**não** o `EditAlarmViewModel.kt`, que é do Lote 2)
- `main/res/values/strings_telas.xml` e `values-pt/strings_telas.xml` (novos)
- `test/java/com/wakewakeup/ui/AlarmListUiTest.kt`, `ListWarningsUiTest.kt`, `PermissionsUiTest.kt`, `AlarmLabelUiTest.kt`, `ChannelPermissionBugTest.kt`, `ScreenshotTour.kt`

**Itens, em ordem**

1. **TEC-07 (P0): canal "tocando" desligado não é detectado.** Detalhe: TEC-07. `AlarmPermissions.isGranted(NOTIFICATIONS)` também exige `NotificationManagerCompat.areNotificationsEnabled()` e o canal `ALARM_NOTIFICATION_CHANNEL_ID` com importância `>= IMPORTANCE_HIGH` (canal inexistente conta como certo). `openSettings(NOTIFICATIONS)`: se a permissão do app está dada e o problema é o canal, abrir `Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS` com `EXTRA_APP_PACKAGE` e `EXTRA_CHANNEL_ID`, com o mesmo fallback de hoje.
   - Pronto quando: `ChannelPermissionBugTest` passa sem `@Ignore` (linha 19, classe); teste novo em `ListWarningsUiTest`: canal em `IMPORTANCE_LOW` mostra o aviso na lista, canal em HIGH não mostra; `PermissionsUiTest` verde.
2. **TEC-04, parte da lista (P0, regra de orçamento).** `AlarmListScreen.kt:156`: começar com `missing = 0` e calcular em `LaunchedEffect(Unit) { missing = withContext(Dispatchers.Default) { AlarmPermissions.missingRequired(context).size } }`; no `ON_RESUME` (linha 159), o mesmo com `scope.launch`.
   - Pronto quando: `missingRequired` não roda dentro de `remember { }`; `missingPermissionsShowUnderTheNextDisplay` verde (use `waitFor` se o teste olhar cedo demais).
3. **UX1-01 (P1, alta): acertar os minutos e digitar a hora.** Detalhe: UX1-01 passos 1 a 5. `Drum` com `dragStep` e `friction` (horas 28 dp e 2,5; minutos 16 dp e 1,5); toque no tambor abre o `TimeEntryDialog` (privado, no molde do `LeaveDialog`): visor com `BasicTextField` numérico de 4 dígitos, dica, erro em `ErrorText`, `GraphiteKey` Cancelar e `OrangeKey` Confirmar. Mensagens de dica e de erro em `WwuType.bodyS` (14 sp), não em `caption`.
   - Strings novas em `strings_telas.xml`: `cd_type_time` = "Digitar a hora" / "Type the time"; `type_time_title` = "Digite a hora" / "Type the time"; `type_time_hint` = "Quatro números. Ex.: 0645 para 06:45." / "Four digits, e.g. 0645 for 06:45."; `type_time_invalid` = "Essa hora não existe. Use de 00:00 a 23:59." / "That time doesn't exist. Use 00:00 to 23:59."; `cancel` = "Cancelar" / "Cancel". Confirmar usa a `confirm` que já existe.
   - Pronto quando: testes novos em `AlarmListUiTest` (digitar "0645" salva 06:45; "2575" mostra o erro e não fecha); `theTimeDrumsWorkWithTalkBack` verde; print novo do diálogo nas quatro variantes.
4. **UX1-02 (P1): "Continuar" não continua; sem seta vindo da lista.** Detalhe: UX1-02 passos 1 a 3, 5 e 6. Apagar o estado `nag` (com permissão faltando o botão já nasce `continue_anyway`, secundário, com `perm_nag` visível); vindo da lista, usar `BackHeader` com `perm_short_title` e botão do pé `back_to_list`; tirar o `PrintLabel` de `perm_eyebrow` (a string fica órfã). O texto novo de `perm_body` é do Lote 7.
   - Strings novas: `perm_short_title` = "Permissões" / "Permissions"; `back_to_list` = "Voltar para a lista" / "Back to the list".
   - Testes a atualizar: `PermissionsUiTest.firstLaunchWalksThroughThePermissionsOnce`, `ListWarningsUiTest.theWarningOpensThePermissionsAndOneTapComesBack` e `ScreenshotTour.permissions()` (o print 06 passa a ser a tela aberta pelo aviso da lista: semear uma permissão faltando, abrir a lista, tocar no aviso, tirar o print).
   - Pronto quando: um toque leva à lista com o aviso de risco já visível antes; aberta pela lista tem "‹" e volta em um toque; prints 05 e 06 refeitos; no `pequeno-05` a terceira placa aparece inteira.
5. **UX1-05 (P1): no celular pequeno sobram dois cartões.** Detalhe: UX1-05. `NextStrip` e `Warnings` passam a rolar com a lista como primeiro `item` do `LazyColumn` (`key = "top"`); `ListHeader` continua fixo; a tela vazia não muda.
   - Pronto quando: teste novo com `@Config(qualifiers = "w360dp-h640dp-xhdpi")` e quatro alarmes (rolar até o último tira o PRÓXIMO da tela); `missingPermissionsShowUnderTheNextDisplay`, `aLowAlarmVolumeIsFlaggedAndTurnedUp` e `switchingARecurringAlarmOffOffersToSkipJustOneDay` verdes.
6. **UX1-06 (P1): o editor esconde SOM e MISSÃO no celular pequeno.** Ordem das linhas da placa: MISSÃO, SOM, NOME. Em telas com `screenHeightDp < 700`: tecla SALVAR com 64 dp, excluir com 52 dp, `bottom = 16.dp` na barra.
   - Pronto quando: `pequeno-07` mostra a linha MISSÃO inteira acima da barra; `AlarmLabelUiTest`, `creatingAnAlarmFromScratch`, `editingAnAlarm` e `deletingTakesTwoTaps` verdes. Se o `ScreenshotTour.editor()` depender da ordem das linhas, ajuste-o (o arquivo é deste lote).
7. **UX1-07 (P1): "MISSÃC" cortado com fonte grande.** `EditAlarmScreen.kt:335`: `val labelWidth = with(LocalDensity.current) { WwuType.label.fontSize.toDp() } * 5.4f`; corrigir o comentário da linha 334.
   - Pronto quando: `fonte-grande-07` e `fonte-grande-08` mostram "MISSÃO" inteiro; teste novo em inglês com `fontScale = 1.3f` sem `hasVisualOverflow` em "MISSION".
8. **UX1-08, parte do código (P1): bateria com a mesma tecla laranja.** Em `PermissionPlate`, quando `recommended && !granted`, usar `GraphiteKey` no lugar da `OrangeKey`. O texto novo de `perm_battery_desc` é do Lote 7.
   - Pronto quando: print 05 com a tecla da bateria em grafite; `batteryIsOnlyRecommended` e `theOtherPermissionsOpenTheirSettingsPages` verdes.
9. **UX1-09 (P1): "PRÓXIMO · EM 8 H 59 MIN" sem a hora.** Em `nextRingText`, no ramo de menos de 24 h: `"${formatClock(at.hour, at.minute)} · ${nextAlarmInText(now, trigger)}"`.
   - Pronto quando: `theNextDisplayCountsDownToTheNearestAlarm` atualizado confere a hora; `aRingADayOrMoreAwayShowsItsDayAndTime` e `aRingAWeekOrMoreAwaySaysItIsTheNextOne` verdes; prints 02 das quatro variantes com o texto em uma linha dentro do visor.
10. **UX1-10 (P2): aviso de permissão discreto e sem saída.** `WarningRow` ganha `detail: String? = null`: título em `WwuType.bodyStrong` (`Ink`) e detalhe em `WwuType.bodyXS` (`InkMuted`). Strings novas (o plural antigo `perm_missing` fica órfão): `perm_missing_title` = "O alarme pode não tocar" / "Your alarm may not ring"; plural `perm_missing_detail`: PT one "Falta %d permissão. Toque para liberar.", many e other "Faltam %d permissões. Toque para liberar."; EN one "%d permission missing. Tap to allow.", other "%d permissions missing. Tap to allow." O aviso de volume fica com uma linha, em `bodyStrong`.
    - Pronto quando: `missingPermissionsShowUnderTheNextDisplay` e `theWarningOpensThePermissionsAndOneTapComesBack` atualizados e verdes; `pt-02` e `pequeno-02` com o aviso em duas linhas, sem corte.
11. **UX1-13 (P2): "toca uma vez" sem dizer o dia.** String nova `days_none_hint_at` = "Nenhum dia marcado: toca uma vez, %1$s às %2$s." / "No day selected: rings once, %1$s at %2$s." (a `days_none_hint` fica órfã). No editor, calcular o dia como o `OnceLine` da lista (`nextTriggerEpochDay(draft.copy(enabled = true), from)`, `weekdayName`, `formatClock`).
    - Pronto quando: teste novo (o texto traz dia e hora e muda quando a hora passa de uma que já foi hoje para uma que ainda vem); print 07 das quatro variantes sem corte.

**Textos e descrições que o `ScreenshotTour` procura nestas telas** (manter): `empty_title`, `cd_alarm_switch`, `skip_once`, `undo`, `perm_title`, `create_alarm`, `new_alarm`, `save`, `edit_alarm`, `delete`, `delete_confirm`, o resumo `task_summary_math`, `sound_default`.

**Skills**: `impeccable` (criar e criticar), `ux-writing` (strings novas), `edge-to-edge` (diálogo com teclado), `jetpack-compose-performance`, `android-permissions-security` (TEC-07).

**Verificação**
```bash
bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "*AlarmListUiTest*" --tests "*ListWarningsUiTest*" --tests "*PermissionsUiTest*" --tests "*AlarmLabelUiTest*" --tests "*ChannelPermissionBugTest*"
bash ferramentas/gradle.sh :app:testDebugUnitTest
WWU_SCREENSHOTS=1 bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"
bash ferramentas/gradle.sh :app:lintDebug
```
Abra os prints 01 a 08 e 20 das quatro variantes e confira contra o `GUIA-VISUAL.md` (um laranja por tela, nada cortado, 48 dp).

**Cuidados de orçamento**: `LazyColumn` com `key` e `contentType` (60 FPS na rolagem); nada de IPC na composição; nenhuma dependência nova para o diálogo de hora.

---

## Lote 5: Telas do toque, da missão e do Bom dia

A casca de tela do ritual. Prioridade máxima P0 (TEC-05 e a parte de tela do LOG-02 e do Direct Boot).

**Arquivos que o lote pode mexer** (a partir de `app/src/`):
- `main/java/com/wakewakeup/session/RingActivity.kt` (exceção explícita: fica na pasta `session/`, mas é só a casca de tela; nenhum outro lote mexe nela)
- `main/java/com/wakewakeup/ui/ring/RingScreen.kt`, `RingSessionViewModel.kt`
- `main/java/com/wakewakeup/ui/mission/MissionScreen.kt`
- `main/java/com/wakewakeup/ui/goodmorning/GoodMorningScreen.kt`
- `main/res/values/strings_toque.xml` e `values-pt/strings_toque.xml` (novos)
- `test/java/com/wakewakeup/ui/RingFlowUiTest.kt`, `GoodMorningBackUiTest.kt`, `RingActivityRaceBugTest.kt`

**Este lote não mexe** em `AlarmRingService.kt` nem em `AlarmSessionState.kt` (Lote 1), nem em `Type.kt`, `Hardware.kt` e `Keypads.kt` (Lote 3).

**Itens, em ordem**

1. **TEC-05 (P0): a tela de toque se fecha antes de a sessão existir.** Detalhe: TEC-05. Em `RingActivity.setContent`: `var seenSession by rememberSaveable { mutableStateOf(false) }`; o `LaunchedEffect(session)` marca `seenSession = true` quando há sessão e, sem sessão e sem nunca ter visto uma, espera `SESSION_WAIT_MS = 3_000L` antes de abrir a `MainActivity`. Enquanto espera, desenhar `Box(Modifier.fillMaxSize().background(Housing))`.
   - Pronto quando: `RingActivityRaceBugTest` passa sem `@Ignore` (linha 25, classe); teste novo: sem sessão por 3 s abre a `MainActivity`; `GoodMorningBackUiTest` verde.
2. **LOG-02, parte das teclas (P0): abaixar o volume sem missão.** Em `RingActivity`, sobrescrever `dispatchKeyEvent`: enquanto houver sessão e a tela não for `GOOD_MORNING`, devolver `true` para `KEYCODE_VOLUME_DOWN` e `KEYCODE_VOLUME_MUTE` (aumentar continua valendo).
   - Pronto quando: teste novo em `RingFlowUiTest`: a tecla de volume para baixo é consumida no toque e na missão, e não no Bom dia.
3. **LOG-01 / TEC-01, parte das telas (P0): não quebrar com o celular bloqueado.** Antes do primeiro desbloqueio o banco não abre e a `MainActivity` não pode ser iniciada.
   - `RingActivity`: `runCatching { startActivity(intent) }` ao sair; `finish()` sempre.
   - `RingSessionViewModel`: `loadCompanions`, `skipCompanions` e `undoCompanions` saem no começo se `!UserManagerCompat.isUserUnlocked(getApplication())`, e as chamadas ao repositório ficam em `try/catch (e: Exception)` (relançando `CancellationException`). A leitura do histórico do item 7 segue a mesma regra.
   - Pronto quando: teste novo com `ShadowUserManager.setUserUnlocked(false)` e o banco fechado: chegar ao Bom dia e concluir não lança exceção e a lista de alarmes de reserva fica vazia.
4. **UX2-01 (P1, alta): o tempo da missão acaba sem aviso.** Sem campo novo na sessão: use o invariante garantido pelo Lote 1 (na tela `RINGING`, `session.mission != null` quer dizer que uma missão estourou o tempo). Em `RingScreen.kt`: `private val RingSession.missionTimedOut get() = mission != null`.
   - Tela de toque (estado tocando, sem Espere): quando `missionTimedOut`, no lugar do rótulo "ALARME DAS 06:30" mostrar `mission_timed_out` em `WwuType.bodyStrong`, cor `Ink`, centralizado, `padding(horizontal = 24.dp, top = 12.dp)`, com `semantics { liveRegion = LiveRegionMode.Polite }`.
   - Missão: trocar o `PrintLabel(mission_seconds)` (linha 113) por uma `Row` com `PrintLabel(mission_time_label)` e `VfdText("%d:%02d".format(s / 60, s % 60), 18.sp.nonScaling(), glowAlpha = 0.5f)`; com `secondsLeft <= 20`, cor e brilho `ErrorDisplay`. A string `mission_seconds` fica órfã.
   - Mensagem de apoio em `MathBody` e `PhraseBody`: ordem `wrong` > `secondsLeft <= 20` (`mission_time_low`, `ErrorText`) > `justCorrect` > padrão.
   - A troca do texto da notificação (passo 5 da auditoria) fica de fora.
   - Strings novas em `strings_toque.xml`: `mission_timed_out` = "O tempo acabou. A missão recomeça do início." / "Time ran out. The mission starts over."; `mission_time_label` = "Tempo" / "Time"; `mission_time_low` = "Faltam %1$d s. Depois o alarme volta a tocar." / "%1$d s left. Then the alarm rings again."
   - Pronto quando: teste novo em `RingFlowUiTest`: sessão `RINGING` com missão mostra `mission_timed_out`, sessão nova não mostra; print 16 com o tempo em vermelho aos 23 s.
5. **UX2-04 (P1): teclado da missão longe do visor e só pela largura.** Detalhe: UX2-04. Em `MathBody`: `displayH` 132 ou 150 dp, `keySize = minOf(byWidth, byHeight).coerceIn(52.dp, 76.dp)` e o espaço elástico sobe para antes do visor (visor a 24 dp do teclado).
   - Pronto quando: `pequeno-15` com folga e todas as teclas à vista; `pt-15` com o visor colado no teclado; `RingFlowUiTest` verde.
6. **UX2-05, parte das telas (P1): ações em 12 sp e botão que muda de tamanho.** Em `RingScreen.kt`: um `private val StartMissionLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.14.em)` e um composable privado `StartMissionButton` (96 dp, `baseDepth = 5.dp`, `gap = 10.dp`, rótulo com `StartMissionLabel`, `labelColor = Ink`, `labelPlate = EspereLabelPlate`, `softShadow(24.dp, 14.dp, 0.45f)`, `PlayGlyph(24.dp, 30.dp, nudge = 3.dp)`), usado igual no toque e no Espere, com `padding(start = 24.dp, end = 24.dp, bottom = 36.dp)`. Em `MissionScreen.kt`: dica, erro e contador de caracteres em `WwuType.bodyS` (14 sp) no lugar de `caption`. (Os tamanhos do Espere são do Lote 3; a tela de pulo é do Lote 6.)
   - Pronto quando: o botão laranja tem o mesmo tamanho e a mesma posição nos prints 13 e 14; nenhum texto de ação ou de erro abaixo de 14 sp nas telas deste lote; `RingFlowUiTest` e `PortugueseUiTest` verdes.
7. **UX2-07 (P1): o Bom dia só informa.** Detalhe: UX2-07 passos 1 a 5. `RingSessionViewModel`: `weekAverageMinutes` e `comparisonReady`, calculados em `loadCompanions` a partir de `wakeHistoryRepository.observeLastDays(7).first()` sem o registro de agora. `GoodMorningScreen`: parâmetros `averageMinutes: Int? = null` e `comparisonReady: Boolean = false` (passados em `RingActivity`), uma linha em `bodyStrong` abaixo de "para acordar de verdade."; com menos de 60 s mostra os segundos e `second_short`; os dígitos entram em "00" e viram uma vez depois de 350 ms (com `rememberReduceMotion()` já entram no valor).
   - Strings novas: `gm_first` = "Primeiro despertar registrado." / "Your first wake-up on record."; `gm_faster` = "%1$d min mais rápido que a sua média da semana." / "%1$d min faster than your weekly average."; `gm_same` = "Na sua média da semana." / "Right on your weekly average."; `gm_slower` = "%1$d min acima da sua média da semana." / "%1$d min over your weekly average."
   - Pronto quando: testes novos (média de 10 e despertar de 6 → "4 min mais rápido…"; sem histórico → `gm_first`; 20 s → "20" e "SEG"); `theWholeRitualFromRingingToGoodMorning` verde; nada de pontos, sequência ou confete.
8. **UX2-10 (P2): acerto quase sem retorno.** Em `MathBody`: vibração `HapticFeedbackType.Confirm` no acerto e luzes de progresso (`Lamp(Vfd, lit = i < mission.index, size = 8.dp)`) na mesma linha da conta, só com `mission.count > 1`, com `clearAndSetSemantics { }`. O texto "CONTA 2 DE 3" continua.
   - Pronto quando: print 15 com 3 luzes e 1 acesa; com 10 contas em 360 dp as luzes cabem ao lado de "49 × 19 =".
9. **UX2-12 (P2): conteúdo preso no topo no toque e no Espere.** Detalhe: UX2-12. `BoxWithConstraints` + coluna com `heightIn(min = maxHeight)` e `Arrangement.Center`; `Spacer` inicial de 24 dp.
   - Pronto quando: `pt-13` e `pt-14` com o bloco centrado (diferença entre os vãos menor que 24 dp); `pequeno-13` e `pequeno-14` sem corte.
10. **UX2-13 (P2): erro da frase não diz onde está.** Strings novas: `phrase_wrong_here` = "Não confere. Corrija a partir da letra sublinhada." / "Doesn't match. Fix it from the underlined letter."; `phrase_wrong_extra` = "Não confere. Tem texto sobrando no fim." / "Doesn't match. There's extra text at the end." Na frase: `wrong` com `matched < phrase.length` usa a primeira; com `matched == phrase.length`, a segunda. A conta continua com `wrong_try_again`.
    - Pronto quando: `RingFlowUiTest.aPhraseAlarm` espera `phrase_wrong_here`; teste novo com texto sobrando acha `phrase_wrong_extra`.

**Textos e descrições que o `ScreenshotTour` procura nestas telas** (manter): descrição `start_mission`; texto `espere_returns_in`; "47 + 38 =" como nó de texto; `wrong_try_again` no erro da conta; `type_phrase_short` em caixa alta; `good_morning`.

**Skills**: `impeccable` (criar, animate e criticar), `ux-writing`, `edge-to-edge`, `jetpack-compose-performance`.

**Verificação**
```bash
bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "*RingFlowUiTest*" --tests "*GoodMorningBackUiTest*" --tests "*RingActivityRaceBugTest*" --tests "*PortugueseUiTest*"
bash ferramentas/gradle.sh :app:testDebugUnitTest
WWU_SCREENSHOTS=1 bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"
bash ferramentas/gradle.sh :app:lintDebug
```
Abra os prints 13 a 18 das quatro variantes: 56 dp ou mais em todo alvo, nada abaixo de 14 sp em ação ou erro, um laranja por tela.

**Cuidados de orçamento**: a tela de toque abre em até 1 s e não espera o banco (o item 1 espera a sessão, não bloqueia a composição); relógio 1 vez por minuto e contagem 1 vez por segundo, sem recomposição nova; a animação do Bom dia é finita e respeita "remover animações"; a leitura do histórico roda uma vez por sessão, fora da thread principal.

---

## Lote 6: Som, rádio, estatísticas e confirmar pulo

Prioridade máxima P1.

**Arquivos que o lote pode mexer** (a partir de `app/src/`):
- `main/java/com/wakewakeup/ui/sound/SoundPickerScreen.kt`, `SoundPreview.kt`, `RadioScreen.kt`
- `main/java/com/wakewakeup/ui/stats/StatsScreen.kt`
- `main/java/com/wakewakeup/ui/skip/SkipConfirmScreen.kt`, `SkipConfirmViewModel.kt`
- `main/java/com/wakewakeup/ui/navigation/WwuNavGraph.kt`
- `main/res/values/strings_som.xml` e `values-pt/strings_som.xml` (novos)
- `test/java/com/wakewakeup/ui/SoundAndTaskUiTest.kt`, `StatsUiTest.kt`, `SkipConfirmUiTest.kt`, novo `SoundPreviewTest.kt`

**Itens, em ordem**

1. **UX2-03 (P1, alta): confirmar pulo corta o teclado e tem dois confirmar.** Detalhe: UX2-03 passos 1 a 5. No estágio `CONFIRM`: sai o `OrangeKey` DESATIVAR (o OK do teclado é o único confirmar); MANTER sai da barra fixa e vai para dentro da coluna que rola, depois do teclado (`Spacer(24.dp)` + `GraphiteKey` em largura total); sai o `PrintLabel(skip_confirm_gate)` e o visor ganha `padding(top = 16.dp)`; `val keySize = if (LocalConfiguration.current.screenHeightDp < 700) 56.dp else 64.dp`, passado em `CalculatorKeypad(key = keySize)`.
   - String nova (as antigas `skip_confirm_gate` e `skip_confirm_turn_off` ficam órfãs): `skip_confirm_hint` = "Digite o resultado e toque em OK" / "Enter the result and tap OK", mostrada embaixo do visor quando não há erro.
   - `SkipConfirmUiTest` (linhas 91, 98 e 130): onde usa `skip_confirm_turn_off`, passar a usar a tecla OK (`onNodeWithContentDescription(str(R.string.confirm))`).
   - Pronto quando: `pequeno-21` mostra as quatro fileiras e o OK inteiro sem rolar, com um só controle laranja; `SkipConfirmUiTest` verde.
2. **LOG-12, parte do teste (P1): `SkipConfirmUiTest` instável.** Na linha 107, trocar a conferência direta por `compose.waitFor("rearmed") { env.scheduledFor(alarm)!!.triggerAtMs == at }` (ou mover para antes a espera da volta à lista).
   - Pronto quando: `SkipConfirmUiTest` verde em 5 execuções seguidas.
3. **UX1-03 (P1): som escolhido e "Aumentar aos poucos" fora da vista.** Detalhe: UX1-03 passos 1 a 6. Ordem: placa "Aumentar aos poucos"; placa de fontes (Rádio, `PlateDivider`, Música do celular); `PrintLabel(sounds_app)` + placa dos sons do app; `PrintLabel(sounds_phone)` + placa com "Padrão do sistema", os toques do celular e o arquivo escolhido; `selectableGroup()` na coluna; `BringIntoViewRequester` para a linha selecionada.
   - Strings novas: `sounds_app` = "Sons do app" / "App sounds"; `sounds_phone` = "Sons do celular" / "Phone sounds".
   - Pronto quando: `pt-11` e `pequeno-11` mostram o interruptor no topo, os dois títulos e a linha selecionada sem rolar; `choosingOneOfTheAppsSoundsAndTurningTheRampOff`, `pickingASongFromThePhone`, `goingBackToTheSystemDefault` e `wakingUpToARadioStation` verdes.
4. **UX1-04 (P1): prévia que não para e não avisa.** Detalhe: UX1-04 passos 1 e 3 a 5. `SoundPreview` ganha `loading`, `failed`, `isFailed(uri)` e `toggle(uri, durationMs)`. O `StopGlyph` nasce **neste lote**, em `SoundPreview.kt`: `@Composable internal fun StopGlyph(size: Dp, color: Color) = Canvas(Modifier.size(size)) { drawRoundRect(color, cornerRadius = CornerRadius(1.5.dp.toPx())) }` (não em `Hardware.kt`). `SoundRow` e `StationRow`: tecla alterna tocar e parar, com `cd_stop_preview` enquanto toca; "Conectando…" em `Vfd` e falha em `ErrorText` na linha de detalhe.
   - Strings novas: `cd_stop_preview` = "Parar %1$s" / "Stop %1$s"; `radio_connecting` = "Conectando…" / "Connecting…"; `radio_station_failed` = "Não tocou agora. Tente outra rádio." / "Didn't play just now. Try another station."; `sound_failed` = "Não deu para tocar este som." / "Couldn't play this sound."
   - Pronto quando: `SoundPreviewTest` novo (carregando até o preparo; erro deixa `isFailed` verdadeiro; `toggle` no mesmo uri para); teste de UI: com a prévia tocando a tecla tem `cd_stop_preview` e um toque apaga "PRÉVIA" do visor.
5. **UX2-09 (P1): Estatísticas vazia.** Só os passos 1 e 2 (o passo 4 da auditoria caiu na confirmação; o texto novo de `stats_empty` é do Lote 7): com `state.loaded && !state.hasData`, mostrar `stats_empty` logo abaixo do `BackHeader`, em `WwuType.body`, cor `Ink`, `padding(start = 20.dp, end = 20.dp, top = 12.dp)`, e apagar o `Text` das linhas 129 a 131; `SummaryPlate` não desenha "min" quando o valor é o traço.
   - Pronto quando: `StatsUiTest.withNoWakeUpsYet` verde; `AlarmListUiTest` (linha 434, de outro lote) continua passando sem edição; print novo do estado vazio com a frase no topo e nenhum "min" solto.
6. **UX2-05, parte do pulo (P1).** `SkipConfirmScreen.kt:220`: dica e erro em `WwuType.bodyS` no lugar de `caption`.
7. **UX1-12 (P2): rádio com duas setas de volta e busca sem limpar.** Detalhe: UX1-12. `RadioScreen` ganha `onDone`; no `WwuNavGraph`, `onDone = { entry.ifResumed { navController.popBackStack(Destinations.EDIT, inclusive = false) } }`; depois de escolher, uma placa no pé com `Lamp(Vfd)`, o nome da estação e `OrangeKey(ready)`; no campo de busca, área de 48 dp com `CrossGlyph` para limpar.
   - String nova: `cd_clear_search` = "Limpar busca" / "Clear search".
   - Pronto quando: testes novos em `SoundAndTaskUiTest` (PRONTO leva direto ao editor com o nome da estação; "Limpar busca" volta a "MAIS OUVIDAS"); `wakingUpToARadioStation` e `radioWithoutInternetCanRetryAndSearch` verdes.
8. **TEC-12, parte de som e rádio (P2).** `SoundPickerScreen.kt:299`: `.offset { IntOffset((width * needle.value - 1.dp).roundToPx(), 0) }`. `RadioScreen.kt:91`: `var attempt by remember { mutableIntStateOf(0) }`.
   - Pronto quando: lint sem `UseOfNonLambdaOffsetOverload` nesses arquivos e sem `AutoboxingStateCreation`.

**Textos e descrições que o `ScreenshotTour` procura nestas telas** (manter): `sound_default`, `sound_bell_name`, `radio_row_desc`, "Radio Um" (do `FakeRadio`), `skip_confirm_title`, `cd_stats_icon`, `minutes_to_wake` em caixa alta.

**Skills**: `impeccable` (criar e criticar), `ux-writing`, `jetpack-compose-performance`.

**Verificação**
```bash
bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "*SoundAndTaskUiTest*" --tests "*SoundPreviewTest*" --tests "*StatsUiTest*" --tests "*SkipConfirmUiTest*"
for i in 1 2 3 4 5; do bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "*SkipConfirmUiTest*" --rerun-tasks || break; done
bash ferramentas/gradle.sh :app:testDebugUnitTest
WWU_SCREENSHOTS=1 bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"
bash ferramentas/gradle.sh :app:lintDebug
```
Abra os prints 11, 12, 19 e 21 das quatro variantes.

**Cuidados de orçamento**: a prévia não baixa nada antes do toque do usuário; nenhuma dependência nova; agulha do sintonizador lida em lambda; a política de privacidade diz que o app não copia a música escolhida, então nada de copiar arquivo.

---

## Lote 7: Textos e ficha da loja

Único lote que mexe em `strings.xml`. Prioridade máxima P1.

**Arquivos que o lote pode mexer**:
- `app/src/main/res/values/strings.xml` e `app/src/main/res/values-pt/strings.xml`
- `app/src/test/java/com/wakewakeup/StringsTest.kt`, `app/src/test/java/com/wakewakeup/ui/PortugueseUiTest.kt`
- novos em `assets/marketing/play-store/`: `ficha-pt-BR.md`, `ficha-en-US.md`, `seguranca-dos-dados.md`

**Regras duras deste lote**: não apagar nem renomear chave; não mudar a quantidade nem o tipo dos argumentos (`%1$s`, `%d`); só mudar texto. Caixa alta nunca digitada na string. Vocabulário do `GUIA-VISUAL.md`, seção 8.

**Itens, em ordem**

1. **UX2-06 (P1): "SEM LIMITE" se lê como aviso.** `espere_sub`: "Toque para adiar" / "Tap to snooze". Em `PortugueseUiTest.kt:113`, o literal "SEM LIMITE" vira "TOQUE PARA ADIAR".
   - Pronto quando: `PortugueseUiTest.theRingScreen`, `RingFlowUiTest.afterAnEspereTheBarCountsInsteadOfSayingNoLimit` e `StringsTest` verdes.
2. **UX1-02, parte do texto (P1).** `perm_body`: "O Android pode atrasar ou silenciar alarmes para economizar bateria. Libere os itens abaixo para o alarme tocar mesmo com o celular bloqueado." / "Android can delay or silence alarms to save battery. Allow the items below so the alarm rings even with the phone locked."
3. **UX1-08, parte do texto (P1).** `perm_battery_desc`: "Impede o sistema de fechar o app à noite. Na lista que abrir, procure Wake Wake Up e desligue a otimização de bateria." / "Keeps the system from closing the app at night. In the list that opens, find Wake Wake Up and turn battery optimization off."
4. **UX2-09, parte do texto (P1).** `stats_empty`: "Ainda sem despertares. Conclua a missão de um alarme e o tempo que você levou aparece aqui." / "No wake-ups yet. Finish an alarm's mission and the time it took shows up here."
5. **`StringsTest` cobre os arquivos novos (P1).** Trocar `load(folder)` para ler e somar todos os arquivos `strings*.xml` da pasta (hoje só lê `strings.xml`), mantendo os quatro testes. Assim, depois da junção, as strings novas dos outros lotes também são conferidas nas duas línguas.
6. **PLAY-01 (P1): ficha da Google Play, com a skill `aso`.** Escrever `ficha-pt-BR.md` e `ficha-en-US.md` com: nome (até 30 caracteres, começando por "Wake Wake Up"), descrição curta (até 80), descrição completa (até 4.000, parágrafos curtos, sem promessa que o app não cumpre), categoria sugerida, 5 legendas de print (uma por tela: lista, toque, missão, Bom dia, estatísticas) e o que há de novo na 1.2. Conte os caracteres e escreva a contagem ao lado. Não prometer "toca mesmo com o celular reiniciado" enquanto o Direct Boot não for validado no aparelho.
7. **PLAY-03, parte do formulário (P1).** Escrever `seguranca-dos-dados.md`: as respostas do formulário "Segurança dos dados" da Play Console tiradas de `site/privacidade.html` (não coleta nem compartilha dados; a rádio manda o texto da busca e o código do país a um serviço de terceiros, sem identificação), a lista das permissões do `AndroidManifest.xml` com uma linha de justificativa para cada e os três pontos que a Play pede declaração: alarme exato (`USE_EXACT_ALARM`), notificação de tela cheia (`USE_FULL_SCREEN_INTENT`) e serviço em primeiro plano `systemExempted`.
8. **UX2-11, textos (P2): "pular" em todo o fluxo.**
   - `pre_alarm_title_one` e `pre_alarm_title_many`: "Já acordou?" / "Already up?"
   - `pre_alarm_text_one`: "%1$s toca às %2$s. Quer pular só desta vez?" / "%1$s rings at %2$s. Skip it just this once?"
   - `pre_alarm_text_many`: "%1$s. Quer pular todos só desta vez?" / "%1$s. Skip them all just this once?"
   - `pre_alarm_action` e `skip_confirm_turn_off`: "Pular" / "Skip"
   - `pre_alarm_channel_desc`: trocar "desativá-lo" por "pulá-lo" / "turn it off" por "skip it"
   - `skip_confirm_title`: "Pular só desta vez?" / "Skip just this once?"
   - `skip_confirm_done`: "Pronto, pulado só desta vez." / "Done, skipped just this once."
   - `skip_receipt_text`: "Pulado só desta vez." / "Skipped just this once."
   - `waits_per_wake` (só PT): "Espere: %1$s por alarme"
   - Pronto quando: `grep -i "desativ" app/src/main/res/values-pt/strings.xml` não acha nada; `grep -rn "Desativ" app/src/test` não acha literal quebrado.
9. **UX1-11 (P2).** `delete_confirm`: "Excluir mesmo?" / "Really delete?"
10. **Revisão geral com a skill `ux-writing` (P2).** Uma passada em todas as strings das duas línguas, só para corrigir o que foge do `GUIA-VISUAL.md` (vocabulário, tom, pontuação, frase que não diz a saída). Liste cada mudança no relatório do lote, com antes e depois. Não mude textos que os itens 1 a 4, 8 e 9 já definiram nem o sentido de nenhuma string. Em inglês a pausa continua "Snooze".

**Skills**: `ux-writing`, `aso`, `impeccable` (clarify).

**Verificação**
```bash
bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "*StringsTest*" --tests "*PortugueseUiTest*"
bash ferramentas/gradle.sh :app:testDebugUnitTest
bash ferramentas/gradle.sh :app:lintDebug
WWU_SCREENSHOTS=1 bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"
```
Abra os prints `pt-*`, `en-*` e `fonte-grande-*` das telas com texto mudado (05, 06, 08, 13, 19, 21): nada cortado.

**Cuidados de orçamento**: só texto; os `.md` de `assets/marketing/` não entram no APK.

---

## Lote 8: Site

Sem Gradle. Prioridade máxima P0 (fontes acima do orçamento e o conferidor do APK).

**Arquivos que o lote pode mexer**:
- tudo em `site/`, menos `site/wake-wake-up.apk` e `site/wake-wake-up-video.mp4`; novo `site/og-image-en.png`
- novos `ferramentas/conferir-apk-site.sh` e `ferramentas/og-image.html`
- `README.md` da raiz
- `app/src/test/java/com/wakewakeup/BudgetBugTest.kt` (exceção explícita, ver item 1) e `app/src/test/java/com/wakewakeup/SiteFilesTest.kt` (só se uma regra nova precisar; de preferência não tocar)

**O `SiteFilesTest` confere, e o lote precisa respeitar**: toda mudança de HTML igual nas duas línguas (mesma contagem de `data-download`, `data-apk-note`, `data-desktop-note`, `data-ios-note` e `data-reveal`, todas maiores que zero); todo `id` usado em `$('...')` no `main.js` existe em `index.html` e `en/index.html`; toda âncora `href="#x"` tem o `id` na mesma página; todo `href`/`src`/`poster` local aponta para um arquivo que existe; o carimbo `?v=AAAAMMDD` é o mesmo nas quatro páginas. No fim do lote, trocar o carimbo em todas as páginas (uma vez só) e o `lastmod` do `sitemap.xml`.

**Itens, em ordem**

1. **TEC-08, parte do site (P0, orçamento): 5 fontes woff2.** Apagar a linha do `@font-face` da Instrument Sans 500 (`styles.css:7`) e o arquivo `site/assets/fonts/instrument-sans-500.woff2`; onde o CSS pede Instrument Sans com `font-weight: 500` (`.bomdia__end`, `.apk-note`), usar 400 (a `.apk-note` muda de novo no item 3). Conferir antes com `grep -rn "500" site/*.html site/en/*.html site/styles.css`. Não mexer na Barlow Condensed 500 (`.vfd`).
   - **Exceção explícita no `BudgetBugTest.kt`**: tirar o `@Ignore` da classe (linha 10) e apagar o método `noBitmapInDrawablesIsAPng` (o Lote 3 o recria em `AppBudgetTest.kt`). Fica só `theSiteLoadsAtMostFourFonts` (remova os imports que sobrarem).
   - Pronto quando: 4 `@font-face` e 4 woff2 referenciados.
2. **SITE-01, o que não depende da chave (P0).** Criar `ferramentas/conferir-apk-site.sh` (bash, só leitura) que recebe o caminho do APK (padrão `site/wake-wake-up.apk`) e falha com mensagem clara se: `aapt2 dump badging` mostrar `application-debuggable`; `apksigner verify --print-certs` mostrar `CN=Android Debug` ou não verificar; o `versionName` do APK for diferente do de `app/build.gradle.kts`; o arquivo passar de 12 MB. No fim imprime versão e tamanho. As ferramentas ficam em `$LOCALAPPDATA/Android/Sdk/build-tools/<mais nova>/` (`aapt2.exe` e `lib/apksigner.jar`). Em `site/README.md`, seção "Atualizar o APK": mandar rodar o script antes de enviar e dizer que site e APK sobem juntos. Rodado contra o APK atual, o script tem de **falhar** (é debug 1.0): esse é o teste dele.
   - No bloco de instalação (item 4), a linha: PT "Já tinha uma versão anterior baixada aqui? Desinstale antes de instalar esta: o Android não atualiza por cima dela, e os alarmes precisam ser recriados." EN "Already have an earlier version downloaded here? Uninstall it before installing this one: Android can't update over it, and your alarms need to be set again." No `site/README.md`, anotar que essa linha sai na atualização seguinte.
   - Gerar e publicar o APK assinado é do Vinicius (sugestão 1).
3. **SITE-03 (P1, alta): texto escuro sobre o degradê.** Detalhe: SITE-03 passos 1 a 4. Rótulo do botão em etiqueta `--housing` (`.download__label`); `.hero__test` vira tecla de 48 px; as notas vão para um painel `.final__notes.visor` com texto `--ink` de 15 px; sai a regra do anel de foco escuro.
   - Pronto quando: nenhum texto de 17 px ou menos direto sobre os degradês de `.hero` e `.final`; rótulo, link do topo e notas com 4,5:1 ou mais; em 360x640 o botão e a etiqueta continuam inteiros na primeira tela.
4. **SITE-04 (P1, alta): depois de "Baixar o APK" o visitante fica sozinho.** Detalhe: SITE-04 passos 1 a 5. `site-config.js`: `apkVersion: "1.2"` e `apkSize: "4 MB"` (comentário "troque junto com o APK"); linha `data-apk-meta` sob cada botão; bloco "Como instalar" com três passos dentro do `.final__notes` (`id="instalar"` / `id="install"`, `tabindex="-1"`); link secundário do topo vira "Como instalar" / "How to install"; `'#instalar': '#install'` no mapa do `lang.js`; no modo `apk`, o clique no botão rola até os passos 600 ms depois, sem cancelar o download.
   - Pronto quando: cada botão mostra versão e tamanho vindos do `site-config.js`; sem JavaScript o link do APK e os passos continuam visíveis; âncoras existem nas duas línguas.
5. **SITE-07 (P1): a página nega a soneca e depois oferece uma sem limite.** Textos exatos em SITE-07 passos 1 a 3 (`en/index.html:95` e `:133`, `index.html:132`). O topo em português fica.
6. **SITE-05 (P1): grátis, sem anúncios, sem cadastro, funciona sem internet.** Detalhe: SITE-05 passos 1 a 4. Faixa `.facts` com quatro leituras em `.vfd` logo depois do topo (sem cartão); frase da rádio em "Sons e rádio"; quarto item "Fechou o app?" em `.extras` (o site novo só sobe junto com o APK novo, que tem o vigia). Não citar reinício do celular.
7. **SITE-08 (P1): 6 telas sem botão de baixar no celular.** Detalhe: SITE-08. `#dock` fixo embaixo (tecla de 52 px), visível só quando o botão do topo e a chamada final estão fora da tela, removido em `ios`/`soon`, oculto em 900 px ou mais e sem JavaScript; `padding-bottom` no rodapé para não cobrir o fim.
8. **SITE-09 (P1): SEO e imagem de compartilhamento em inglês.** Detalhe: SITE-09 passos 1 a 5, com `"softwareVersion": "1.2"`. Títulos e descrições novos; JSON-LD com `operatingSystem`, `softwareRequirements`, `softwareVersion`, `isAccessibleForFree`; sem `aggregateRating`.
   - **`site/og-image-en.png` (1200x630) é gerada do CSS do próprio site, não no Canva**, para ficar igual à portuguesa: criar `ferramentas/og-image.html` (página de 1200x630 que usa `../site/styles.css` e as fontes do site, com o relógio flip em 06:30, a frase e a linha de apoio; `?lang=en` troca os textos para "Wake up for real." e "WAKE WAKE UP · ALARM CLOCK FOR ANDROID") e capturar com o Chrome sem interface (`--headless --screenshot --window-size=1200,630`) ou com o Playwright. Compare lado a lado com `site/og-image.png`: mesma composição. Não regravar a imagem portuguesa. Em `en/index.html` e `en/privacy.html`, apontar `og:image`, `twitter:image` e o `image` do JSON-LD para a nova e trocar o `alt` para "Flip clock showing 06:30 and the tagline Wake up for real."
   - Pronto quando: títulos com 60 e descrições com 155 caracteres ou menos; JSON-LD sem erro; Lighthouse SEO 100.
9. **SITE-10 (P1): seletor de idioma de 32 px, sem "voltar" no celular, nome do botão do vídeo.** Detalhe: SITE-10 passos 1 a 3.
   - Pronto quando: `.lang` com 48 px; a política tem link de volta de 48 px no topo em 390 px; Lighthouse sem `label-content-name-mismatch`.
10. **PLAY-03, parte da política (P1).** Em `privacidade.html` e `en/privacy.html`: em "O que fica guardado no seu aparelho", acrescentar que uma cópia mínima dos horários dos alarmes ligados (hora, dias e o dia pulado) fica numa área do aparelho que o sistema lê antes do primeiro desbloqueio, só para o alarme tocar depois de um reinício; conferir a lista "Permissões que o app pede" contra `app/src/main/AndroidManifest.xml` (somente leitura) e a frase da otimização de bateria (o app não pede a permissão, só abre a lista do Android); trocar a data do topo. Não afirmar nada que o app não faz.
11. **SITE-02, parte do README (P1).** Em `site/README.md`, depois de "Publicar na Hostinger", a seção "Conferir depois de publicar" com os seis testes do SITE-02 passo 2 (comando `curl` e resultado esperado de cada um). Publicar é do Vinicius.
12. **SITE-06, preparo (P2): um e-mail só, sem decidir por ele.** Em `site-config.js`, novo campo `testContact: "vinicostamaga@outlook.com"` (o endereço que o cartão já mostra), com o comentário "apague esta linha para o cartão usar o mesmo e-mail do rodapé". O link do cartão vira `data-contact-link` com `data-subject`, e o `main.js` monta o `mailto:` com `config.testContact || config.contact`. Resultado visível igual ao de hoje; a troca vira uma linha. Não mudar o texto do cartão (passo 3 depende do Vinicius).
13. **SITE-11 (P2): visores das estatísticas vazando em 360 px.** Detalhe: SITE-11 passos 1 a 3.
    - Pronto quando: em 320, 345 e 360 px, nas duas línguas, nenhum `.readout` com `scrollWidth > clientWidth`.
14. **SITE-12 (P2): o que lembra template.** Detalhe: SITE-12 passos 1 a 3. Sai a entrada animada igual em todas as seções (ficam só os dois blocos com `data-trigger`; manter pelo menos um `data-reveal` e a mesma contagem nas duas línguas); as notas viram um painel com divisórias; a tecla de apagar vira SVG.
    - Pronto quando: print de página inteira sem rolar mostra todo o conteúdo; o gráfico e os dígitos do "Bom dia" continuam animando ao entrar na tela.
15. **SITE-13, itens 1 e 3 (P2).** Frase dos sons sem número (textos em SITE-13 item 1); no `README.md` da raiz, linha 24: "se o app for encerrado no meio do toque, o alarme volta a tocar" (sem prometer reinício enquanto o Direct Boot não for validado no aparelho). A grafia do nome da empresa e o zip antigo são do Vinicius.

**Skills**: `impeccable` (parte web: criar e criticar), `ux-writing`, `seo-audit`.

**Como abrir e conferir**
```bash
python -m http.server 8765 --directory site      # http://localhost:8765/ e http://localhost:8765/en/
```
- No navegador (painel do navegador ou Playwright): 390x844, 360x640 e 1440x900, PT e EN (para o inglês, `localStorage.setItem('wwu-lang','en')` antes de abrir `/en/`), com e sem "reduzir movimento". Prints do topo, da chamada final e da página inteira.
- Conferências sem Gradle:
```bash
node --check site/main.js && node --check site/lang.js && node --check site/site-config.js
for a in data-download data-apk-note data-desktop-note data-ios-note data-reveal; do echo "$a $(grep -o "$a" site/index.html | wc -l) $(grep -o "$a" site/en/index.html | wc -l)"; done   # pares iguais e maiores que zero
grep -ho '?v=[0-9]*' site/*.html site/en/*.html | sort -u        # uma linha só
grep -c "@font-face" site/styles.css                             # 4
wc -c site/main.js site/lang.js site/site-config.js              # soma até 30.000 B
bash ferramentas/conferir-apk-site.sh                            # tem de falhar com o APK antigo
```
- Lighthouse no celular para `/` e `/en/` (com `--lang=en-US` na inglesa), salvando em `.auditoria/logs/lighthouse-depois*.json`: 90 ou mais nas quatro categorias.
- O `SiteFilesTest` e o `BudgetBugTest` rodam na junção (orquestrador).

**Cuidados de orçamento**: JS próprio até 30 KB (hoje 19,4 KB; SITE-04, 06 e 08 somam, SITE-12 tira); página inicial até 500 KB sem vídeo e APK; 4 woff2; `og-image-en.png` não é carregada pela página, mas passe de 50 KB só se não houver jeito (a portuguesa tem 185 KB em PNG; não converter para não mudar o nome do arquivo já publicado); vídeo segue com `preload="none"`; nenhum pedido a terceiros.

---

## Lote 9: Arte de divulgação

Canva (MCP conectado). Sem Gradle. Prioridade máxima P2. No máximo 3 peças.

**Arquivos que o lote pode mexer**: só `assets/marketing/` (novo): `assets/marketing/play-store/play-destaque-pt.png`, `play-destaque-en.png`, `play-icone-512.png` e `assets/marketing/README.md`. Nada em `app/` nem em `site/`.

**Antes de gerar**: diga no relatório quantas peças vai criar (2 designs no Canva e 1 exportação local). Não use geração de imagem por IA do Canva (`generate-image`): as peças são montadas com recortes reais.

**Itens, em ordem**

1. **PLAY-05, gráfico de destaque da Play Store (P2), 1024x500, PT e EN.** Siga o `GUIA-VISUAL.md`, seção 11.
   - Material real: recorte do relógio flip de `site/og-image.png` (a faixa do relógio, sem os textos) enviado ao Canva como imagem. Não redesenhar o relógio.
   - Composição: fundo `#141017`; nascer do sol só na base (`#6B2433` → `#E8663C` → `#F7D46B`), baixo; relógio à direita; à esquerda, a frase em Instrument Sans SemiBold creme `#F6ECE0` ("Acorde de verdade." / "Wake up for real.") e, abaixo, a linha de apoio em caixa alta 600 com espaçamento largo, cor `#B49E88` ("O DESPERTADOR QUE SÓ DESLIGA COM UMA MISSÃO" / "THE ALARM THAT ONLY TURNS OFF AFTER A MISSION"). Margem de segurança de 15% em toda a volta. Alinhado à esquerda, não centralizado.
   - Se o Canva não tiver Instrument Sans, pare e registre no relatório: não troque por outra família.
   - Exportar PNG 1024x500 (até 1 MB, sem transparência) para os dois arquivos.
2. **PLAY-05, ícone da loja 512x512 (P2), a partir do ícone atual.** Sem Canva, para sair idêntico: do checkout principal, `ffmpeg -i app/src/main/res/drawable-nodpi/ic_launcher_foreground_art.png -vf "crop=344:344:44:44,scale=512:512:flags=lanczos" assets/marketing/play-store/play-icone-512.png` (se o Lote 3 já tiver trocado o arquivo por `.webp`, use o `.webp`). Abra o resultado: o sol no centro, sem borda estranha. A Play aplica o canto arredondado sozinha.
3. **`assets/marketing/README.md`**: uma linha por peça (arquivo, tamanho, onde entra, ferramenta e plano usados, link do design no Canva), para conferir os direitos de uso antes de publicar.

**Skills**: `impeccable` (crítica das peças contra o `GUIA-VISUAL.md`), `aso` (especificações em `.claude/skills/aso/references/google-play-specs.md`).

**Verificação**: abrir os três PNG; conferir dimensões (`ffprobe` ou propriedades do arquivo); texto legível em miniatura (reduza para 25% e leia); nenhum item da lista de proibições do guia.

**Cuidados de orçamento**: nada disso entra no APK nem na página do site.

---

## Junção (orquestrador, depois de todos os lotes)

1. Juntar os lotes e rodar `bash ferramentas/gradle.sh :app:testDebugUnitTest` (inclui `SiteFilesTest`, `BudgetBugTest`, `AppBudgetTest` e `StringsTest` já lendo os `strings_*.xml`), `:app:lintDebug` (zero erros) e `:app:assembleRelease` (até 4.269.850 B).
2. Apagar de `strings.xml` (EN e PT) as chaves órfãs, só se `grep -rn "R.string.<chave>\|R.plurals.<chave>" app/src` não achar uso: `perm_eyebrow`, `mission_seconds`, `cd_key_sign`, `skip_confirm_gate`, `skip_confirm_turn_off`, `days_none_hint`, plural `perm_missing`.
3. `grep -rn "@Ignore" app/src/test --include=*.kt | grep -v ScreenshotTour` não devolve nada.
4. Conferir que `apkVersion` e `softwareVersion` do site batem com o `versionName` do Gradle (1.2).
5. Gerar os prints "depois" (`WWU_SCREENSHOTS=1 ... ScreenshotTour*`) e copiar de 4 a 6 prints `pt` e `en` (lista, toque, missão, Bom dia, estatísticas) para `assets/marketing/play-store/prints/`.
6. Medir `performance-depois.md` contra `orcamento.md`.

---

## Fora deste plano

| O que | Por quê |
|---|---|
| LOG-01, mover o banco Room para o armazenamento protegido do aparelho | Risco de perder alarmes numa migração que não dá para testar sem aparelho. O plano usa uma cópia mínima (`ArmedRings`) e deixa o banco onde está. |
| TEC-01, cópia completa do alarme (nome, som, missão) para tocar igual antes do desbloqueio | Antes do desbloqueio o alarme toca a campainha do app com a missão padrão. Basta para acordar e evita duplicar os dados. |
| TEC-03 passo 5, copiar a música escolhida para dentro do app | A política de privacidade diz que o app não copia o arquivo; ocupa espaço; a cadeia de reserva já cobre o caso. |
| LOG-08, parte opcional (contar o Espere com `elapsedRealtime`) | Baixo impacto: só acontece se o relógio for atrasado durante os 5 min do Espere. |
| LOG-13, notificação "não tocou" e "tocar agora" depois do boot | Baixo impacto e mexe no ponto mais delicado (boot). Entrou só desligar o alarme de uma vez que passou. |
| UX2-01 passo 5, texto da notificação quando o tempo acaba | A tela de toque já avisa; exigiria um campo novo compartilhado entre dois lotes. |
| UX2-09 passo 4, espera no `ScreenshotTour.stats()` | A confirmação mostrou que os prints 19 já saem com dados. |
| SITE-01 passos 1 e 2, SITE-02 passos 1 e 3, SITE-06 passos 1 e 3, SITE-13 itens 2 e 4 | Dependem de chave, publicação ou decisão do Vinicius: ver "Sugestões". |
| Trocar os 7 sons de `res/raw` | ElevenLabs não está conectado e a origem dos arquivos só o Vinicius sabe. Prompts prontos em "Assets". |
| og-image em inglês pelo Canva | Sai do CSS do site (Lote 8) para ficar idêntica à portuguesa. |
| Avisos de lint que não são erro (13 `UseKtx`, versões de Kotlin, Gradle e core-ktx, `ModifierParameter`, `UnusedResources` fora das órfãs) | Esforço sem ganho para o usuário; atualizar dependências agora arrisca a base logo antes do lançamento. |
| Minificar o CSS e tirar o `lang.js` do `<head>` (Lighthouse) | O site não tem etapa de build e o `lang.js` precisa rodar antes da primeira pintura; a nota já é 97. |
| Testes automáticos do JavaScript do site (F81 a F93) | Pediriam uma ferramenta nova de teste de navegador; a conferência é manual no Lote 8. |
| Limite para o Espere; mais de um dia pulado por alarme | Decisões de produto registradas em `funcoes.md`, não defeitos. |
| Grade de furos do alto-falante (suspeita 6 do medidor) | Avaliada e descartada pela auditoria técnica. |

---

## Assets

**Música**: o app não usa. Suno fora.

**Arte (Canva)**: Lote 9, 3 peças para a Google Play (destaque PT e EN, ícone 512). A imagem de compartilhamento em inglês sai do CSS do site (Lote 8). Nenhuma arte do Canva entra no app.

**Sons (ElevenLabs, NÃO conectado nesta sessão)**: nenhum som é gerado neste plano. A origem e a licença dos 7 sons de `res/raw` não estão registradas (PLAY-04). Se o Vinicius não conseguir confirmar a origem, estes são os prompts prontos para gerar substitutos. Identidade sonora do manifesto: rádio-relógio dos anos 70, sons mecânicos e eletrônicos simples, secos e altos.

Regras para a rodada, quando houver: 7 sons x 3 variações = 21 gerações (abaixo do limite de 30); candidatos em `assets/audio/_candidatos/`; duração de 12 s com a opção de loop ligada; o arquivo final mantém **exatamente** o nome atual em `app/src/main/res/raw/` (alarmes salvos guardam o som pelo nome); MP3 de 192 kbps, até 400 KB cada e 2,5 MB no total; volume de alarme, não de efeito de jogo (alvos abaixo), com pico em -1 dB para tirar o clipping de hoje; registrar no manifesto o serviço e o plano.

| Arquivo final (não renomear) | Onde entra | Alvo de volume | Prompt (em inglês, como o ElevenLabs entende melhor) |
|---|---|---|---|
| `campainha_mecanica.mp3` | som do app e reserva quando qualquer outro falha | -9 LUFS | Vintage 1970s mechanical twin-bell alarm clock ringing nonstop, two small steel hammers hitting brass bells very fast, close dry recording, no reverb, no room tone, very loud and harsh, constant ring with no pause, seamless loop |
| `telefone_de_disco.mp3` | som do app | -8 LUFS | Old rotary dial telephone ringing, classic electromechanical double ring then a short pause, repeating, metal bell inside a bakelite body, close dry recording, no reverb, loud, seamless loop |
| `bipe_digital_1.mp3` | som do app | -9 LUFS | 1970s digital alarm clock beep, short insistent square-wave beeps around 2 kHz, four fast beeps then a brief gap, repeating, completely dry, no reverb, loud, seamless loop |
| `bipe_digital_2.mp3` | som do app | -8 LUFS | Vintage clock radio piezo buzzer alarm, steady fast two-tone electronic beep alternating high and low, bright and harsh, completely dry, loud, seamless loop |
| `sirene_1.mp3` | som do app | -6 LUFS | Loud mechanical hand-crank style siren wailing slowly up and down in pitch, harsh and urgent, close dry recording, no reverb, no background noise, seamless loop |
| `sirene_2.mp3` | som do app | -5 LUFS | Loud two-tone electronic alarm siren alternating high and low every half second, 1970s horn speaker burglar alarm, harsh, completely dry, seamless loop |
| `passaros.mp3` | som do app (ambiente) | -15 LUFS | Dawn chorus of songbirds outdoors, several birds singing brightly and close, lively and dense with no quiet gaps, no wind, no traffic, no water, natural dry recording, seamless loop |

Pós-processo de cada escolhido: `ffmpeg -i candidato.mp3 -af "silenceremove=start_periods=1:start_threshold=-50dB,loudnorm=I=<alvo>:TP=-1.0:LRA=7" -ar 44100 -b:a 192k app/src/main/res/raw/<nome>.mp3`. Ouvir no celular antes de trocar: alarme baixo demais é defeito.

---

## Sugestões que precisam do Vinicius

1. **Assinar o APK de release (SITE-01, crítico).** Passo exato: ache ou crie a sua chave de upload (`.jks`); na raiz do projeto crie `keystore.properties` com as quatro linhas `storeFile=`, `storePassword=`, `keyAlias=` e `keyPassword=` (o arquivo já está no `.gitignore`); rode `bash ferramentas/gradle.sh :app:assembleRelease`; rode `bash ferramentas/conferir-apk-site.sh app/build/outputs/apk/release/app-release.apk`; se passar, copie esse arquivo para `site/wake-wake-up.apk` e confira o tamanho em `site/site-config.js` (`apkSize`). Para a Play Store o envio é em AAB: `bash ferramentas/gradle.sh :app:bundleRelease`. Guarde a chave fora do OneDrive e com cópia: sem ela não há atualização.
2. **Publicar o site (SITE-02).** Só junto com o APK novo. Siga `site/README.md`, "Publicar na Hostinger" (enviar `site/` inteiro com `en/` e `.htaccess`, limpar o cache do CDN), rode os seis testes da seção "Conferir depois de publicar" e reenvie o `sitemap.xml` no Search Console. Quem já instalou o APK antigo (debug 1.0) precisa desinstalar antes: a página nova avisa.
3. **Escolher o e-mail (SITE-06).** Hoje o rodapé e a política usam `cyberhat.tech@gmail.com` e o cartão de teste usa `vinicostamaga@outlook.com`. Para ficar com um só (recomendado: o da política), apague a linha `testContact` de `site/site-config.js` e a menção ao outro endereço em `site/README.md`. Se preferir o outro, troque `contact` e as três menções na política (PT e EN).
4. **Teste fechado na Google Play (SITE-06 passo 3).** Se o teste fechado estiver ativo, me diga e eu troco o texto do cartão para "Prefere instalar pela loja?" (texto pronto na auditoria do site). Se não estiver, o cartão fica como está.
5. **Validar no celular** (não há emulador neste PC; nada disso se prova na JVM):
   - Direct Boot (LOG-01/TEC-01): crie um alarme para daqui a 10 min, reinicie o celular e **não desbloqueie**. Esperado: o alarme toca com a campainha do app e a missão padrão, por cima da tela de bloqueio; concluir a missão fecha a tela sem erro. Depois de desbloquear, a lista está igual e o próximo toque aparece no relógio do sistema. Repita com um alarme de uma vez: depois de desbloquear ele aparece desligado.
   - Restauração (TEC-06): `adb shell bmgr backupnow com.wakewakeup`, desinstalar, reinstalar, `adb shell bmgr restore com.wakewakeup` e, sem abrir o app, `adb shell dumpsys alarm | findstr wakewakeup` mostra os alarmes.
   - Volume (LOG-02): alarme com o volume de alarme no mínimo toca audível; a tecla de volume para baixo não faz nada durante o toque; ao concluir, o volume volta ao que era.
   - Som escolhido (LOG-04/TEC-03): música do celular como som; com o alarme tocando, apague o arquivo: outro som assume.
   - Canal (TEC-07): desligue só o canal "Alarmes" nas notificações do app: a lista avisa.
   - Relógio (LOG-08): mude a data para o futuro e volte: `dumpsys alarm` mostra o toque no instante certo.
   - Partida (TEC-04): abrir a frio em até 1,5 s (`adb shell am start -W com.wakewakeup/.MainActivity`).
   - Ouvir os 7 sons e conferir se algum estoura (pico em 0,0 dB em cinco deles).
6. **Sons: origem e licença (PLAY-04).** Confirme de onde vieram os 7 arquivos de `res/raw` (entraram no commit `a405a0b`, em 01/10/2026) e anote serviço, plano e licença em `assets/audio/audio_manifest.md`. Se não der para confirmar, autorize o ElevenLabs em `/mcp` e me peça para gerar os substitutos com os prompts da seção "Assets" (21 gerações).
7. **Play Console.** Com a conta de desenvolvedor: criar o app; colar a ficha de `assets/marketing/play-store/ficha-*.md`; enviar o destaque, o ícone e os prints de `assets/marketing/play-store/`; preencher "Segurança dos dados" com `seguranca-dos-dados.md`; apontar a política para `https://wakewakeup.cyberhat.com.br/privacidade.html` (o site novo precisa estar no ar); declarar alarme exato, notificação de tela cheia e serviço em primeiro plano `systemExempted` (o app é um despertador: é o caso de uso previsto); responder a classificação de conteúdo; contas pessoais novas precisam de teste fechado com 12 testadores por 14 dias antes da produção. Quando o app estiver na loja, preencha `storeUrl` em `site/site-config.js`.
8. **Versão.** O plano usa `versionCode 3` e `versionName "1.2"`. Se quiser outro número, troque em `app/build.gradle.kts` e em `site/site-config.js` (`apkVersion`) e no JSON-LD das duas páginas iniciais.
9. **Pequenas decisões.** Grafia do nome da empresa ("CyberHat" no site, "Cyberhat" no `README.md`): escolha uma. O `wake-wake-up-site.zip` da raiz tem o site e o APK antigos (26,5 MB): posso apagar depois da publicação nova?
10. **Enviar a branch.** Nada foi enviado ao GitHub: `git push` e o merge de `melhorias-claude` são seus.
