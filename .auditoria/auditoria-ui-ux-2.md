# Auditoria de UI e UX 2: Toque, Missão, Bom dia, Estatísticas, Confirmar pulo, notificações e coerência visual

Feita em 2026-11-26 pelo AUDITOR DE UI E UX 2 (somente leitura). Critério: skill impeccable (craft-floor, critique-rubric, android, audit.native) e ux-writing, com a identidade de rádio-relógio dos anos 70 valendo mais que as skills quando há conflito.

Base de código citada: `app/src/main/java/com/wakewakeup/` (abreviado como `…/`). Prints em `.auditoria/prints/antes/app/`.

## Visão geral

- **Entende em 2 segundos?** Na tela de toque, sim para "Começar missão" (único elemento laranja, embaixo, no polegar). O Espere é inconfundível com a missão (barra cinza no topo contra botão laranja embaixo), mas em português ele se lê como aviso e não como botão (UX2-06), e os rótulos das duas ações estão em 12 e 13 sp (UX2-05).
- **Teclado numérico**: confortável no celular padrão (teclas de 76 dp, retorno tátil). Os problemas são a tecla "−" que nunca serve (UX2-08), o tamanho calculado só pela largura (UX2-04) e a tela de confirmar pulo, que corta o teclado no celular pequeno (UX2-03).
- **Bom dia**: bonito e calmo, mas só informa. Não compara com nada, então não dá motivo para acordar mais rápido amanhã (UX2-07).
- **Cara de IA**: baixa. O app tem um mundo próprio e coerente (visor, placas, teclas com base sólida, LEDs). O que mais lembra template é a tela de Estatísticas vazia (dois cartões com "– min", UX2-09). Não há emoji no lugar de ícone, nem vidro, nem degradê sem motivo (os três degradês são o nascer do sol).
- **O que está bom e deve ficar**: dígitos flip, medidor de volume em LED, vibração e tremida no erro, "Não consigo resolver" em dois toques, respeito a "remover animações", leitura por TalkBack, alvos de 48 dp ou mais em todas as telas auditadas, contraste de texto acima de 4,5:1 nos pares conferidos (InkMuted sobre Housing 7,3:1; OnAction sobre Action 5,7:1).
- **Aviso sobre os prints 19**: `en-19`, `pequeno-19` e `fonte-grande-19` mostram o quadro de carregamento da tela de Estatísticas (o `ScreenshotTour.stats()` não espera os dados), não a tela com dados. Só `pt-19` saiu certo. Correção incluída no UX2-09.

Total: 13 itens (0 críticos, 3 altos, 6 médios, 4 baixos).

---

### UX2-01: O tempo da missão acaba sem aviso e o alarme volta a tocar sem explicar por quê
- Gravidade: alta
- Evidência: `…/session/AlarmRingService.kt:435-456` (`tickMission`: ao zerar, volta para `RINGING` com volume de pelo menos 80% e `freshMission`, ou seja, as contas já resolvidas somem); `…/ui/ring/RingScreen.kt:85-201` (a tela de toque não tem nenhum estado ou texto para "o tempo acabou"); `…/ui/mission/MissionScreen.kt:113` (o tempo aparece só como "MISSÃO · 74 S", em rótulo de 12 sp apagado); prints `pt-15-missao.png` e `pt-16-missao-errou.png` (com 23 s restantes nada muda além de 4 LEDs).
- Como reproduzir: alarme com 3 contas. Tocar em "Começar missão", resolver 2 contas e ficar parado até o tempo zerar. A tela troca sozinha para o toque, o som volta alto e, ao tocar de novo em "Começar missão", a missão está em "Conta 1 de 3". Em nenhum momento o app diz que havia prazo, que ele acabou ou que o progresso foi perdido.
- O que mudar:
  1. `…/session/AlarmSessionState.kt`: acrescentar em `RingSession` o campo `val missionTimedOut: Boolean = false`.
  2. `AlarmRingService.tickMission`: no `copy` do estouro de tempo, incluir `missionTimedOut = true`. Em `onStartMission` e `onHoldOn`, incluir `missionTimedOut = false` no `copy`.
  3. Strings novas nos dois idiomas: `mission_timed_out` = "O tempo acabou. A missão recomeça do início." / "Time ran out. The mission starts over."; `mission_time_label` = "Tempo" / "Time"; `mission_time_low` = "Faltam %1$d s. Depois o alarme volta a tocar." / "%1$d s left. Then the alarm rings again."
  4. `RingScreen.Ringing`: quando `session.missionTimedOut`, no lugar do rótulo "ALARME DAS 06:30" (linhas 154-157) mostrar `Text(stringResource(R.string.mission_timed_out), style = WwuType.bodyStrong, color = Ink, textAlign = Center)` com `padding(horizontal = 24.dp, top = 12.dp)` e `semantics { liveRegion = LiveRegionMode.Polite }`.
  5. Notificação: em `buildNotification` (linha 176-177) e `resurface` (linha 212-213), quando `missionTimedOut` for verdadeiro, usar `mission_timed_out` como texto no lugar de `alarm_notification_text`.
  6. `MissionScreen.kt:113`: trocar o `PrintLabel(mission_seconds)` por uma `Row` (8 dp entre os dois, alinhada ao centro) com `PrintLabel(stringResource(R.string.mission_time_label))` e `VfdText("%d:%02d".format(s / 60, s % 60), 18.sp.nonScaling(), glowAlpha = 0.5f)`. Quando `mission.secondsLeft <= 20`, o `VfdText` usa `color = ErrorDisplay` e `glowColor = ErrorDisplay`. Apagar a string `mission_seconds` dos dois arquivos.
  7. Mensagem de apoio: em `MathBody` (linhas 167-171) a ordem passa a ser `wrong` > `secondsLeft <= 20` (texto `mission_time_low`, cor `ErrorText`) > `justCorrect` > padrão. Em `PhraseBody` (linha 328-335), a mesma regra: `wrong` > `secondsLeft <= 20` > contador de caracteres.
- Critério de pronto: teste novo em `AlarmRingServiceTest` prova que `missionTimedOut` fica verdadeiro quando o tempo zera e volta a falso em `ACTION_START_MISSION`; teste novo em `RingFlowUiTest` prova que o texto `mission_timed_out` aparece na tela de toque depois do estouro; `aTimedOutMissionStartsOverFromTheFirstProblem` e `runningOutOfTimeRingsAgainLouder` continuam passando; `StringsTest` passa; print 16 refeito mostra o tempo em vermelho com 23 s.
- Status: CONFIRMADO — tickMission (AlarmRingService.kt:435-456) volta a RINGING com freshMission sem aviso; MissionScreen.kt:113 mostra o tempo só como PrintLabel de 12 sp; RingScreen não tem estado de tempo esgotado; testes citados existem. Custo de performance nulo.

### UX2-02: Os mesmos 120 s valem para 1 conta ou para 10
- Gravidade: alta
- Evidência: `…/session/AlarmSessionState.kt:10` (`MISSION_TOTAL_SECONDS = 120`); `…/ui/edit/EditAlarmViewModel.kt:109` (quantidade de contas de 1 a 10); `…/session/AlarmRingService.kt:542` (ao acertar uma conta, o `copy` mantém `secondsLeft`); `…/session/MissionGenerator.kt:50-53` (difícil = multiplicação de 12..49 por 6..19).
- Como reproduzir: criar um alarme com 6 contas no difícil. Ao tocar, começar a missão. Sobram 20 s por multiplicação de dois dígitos, de cabeça e com sono. O tempo zera, o alarme volta a 80% e a missão recomeça da conta 1. O ciclo se repete até a pessoa descobrir o "Não consigo resolver". Somado ao UX2-01, o app parece quebrado.
- O que mudar: o prazo passa a ser por conta. Em `AlarmRingService.onMissionConfirm`, no `copy` da linha 542 (próxima conta), acrescentar `secondsLeft = MISSION_TOTAL_SECONDS`. Nada mais muda: o estouro continua voltando a tocar e recomeçando a missão, e a frase continua com 120 s no total. O medidor de LED da missão continua dividindo por `MISSION_TOTAL_SECONDS`.
- Critério de pronto: teste novo em `AlarmRingServiceTest`: com 3 contas, esperar 30 s, acertar a primeira e conferir `mission.secondsLeft == MISSION_TOTAL_SECONDS`; `theTimerCountsDownOncePerSecond`, `givingUpSwitchesToThePhraseAndKeepsTheClockRunning` e `aTimedOutMissionStartsOverFromTheFirstProblem` continuam passando.
- Status: CONFIRMADO — MISSION_TOTAL_SECONDS=120 único; o copy da próxima conta (AlarmRingService.kt:~542) não reinicia secondsLeft; difícil é 12..49 x 6..19. Mudança de uma linha, testes citados existem. Mantém a missão obrigatória.

### UX2-03: Confirmar pulo corta o teclado no celular pequeno e tem dois botões de confirmar, um deles mudo
- Gravidade: alta
- Evidência: print `pequeno-21-confirmar-desativar.png` (a fileira do "0" e metade do OK ficam escondidas atrás de MANTER e DESATIVAR; num aparelho real ainda entram as barras do sistema); print `pt-21-confirmar-desativar.png` (OK laranja no teclado e DESATIVAR laranja embaixo fazem a mesma coisa); `…/ui/skip/SkipConfirmScreen.kt:129-145` (barra fixa) e `:228` (teclado de 64 dp fixo); `…/ui/skip/SkipConfirmViewModel.kt:92-93` (`confirm()` sai em silêncio se nada foi digitado, então tocar em DESATIVAR logo ao abrir não faz nada e não explica).
- Como reproduzir: abrir a tela pela notificação "Já acordou?" num celular de 360x640 dp: o zero não aparece sem rolar, e nada indica que a tela rola. Em qualquer celular: tocar em DESATIVAR sem digitar a resposta. Nada acontece.
- O que mudar (em `SkipConfirmScreen.kt`):
  1. No estágio `CONFIRM`, tirar o `OrangeKey` DESATIVAR (linha 137). A tecla OK do teclado passa a ser o único confirmar.
  2. No estágio `CONFIRM`, tirar MANTER da barra fixa e colocá-lo dentro da coluna que rola, logo depois do teclado: `Spacer(24.dp)` e `GraphiteKey(stringResource(R.string.skip_confirm_keep), onClick = onDone, modifier = Modifier.fillMaxWidth())`. Os estágios `DONE` e `STALE` continuam com a barra fixa como hoje.
  3. Em `Gate`: apagar o `PrintLabel(skip_confirm_gate)` (linhas 193-196) e dar `padding(top = 16.dp)` ao visor da conta. A string `skip_confirm_gate` muda de texto e passa a ser a dica embaixo do visor quando não há erro (linha 219, no lugar de `enter_result`): "Digite o resultado e toque em OK" / "Enter the result and tap OK".
  4. Tamanho das teclas: `val keySize = if (LocalConfiguration.current.screenHeightDp < 700) 56.dp else 64.dp` e `CalculatorKeypad(onKey = viewModel::pressKey, key = keySize)`.
  5. `SkipConfirmUiTest` (linhas 91, 98 e 130): onde clica ou procura `skip_confirm_turn_off`, passar a usar a tecla OK (`onNodeWithContentDescription(str(R.string.confirm))`). Apagar a string `skip_confirm_turn_off` dos dois idiomas se ficar sem uso.
- Critério de pronto: no print `pequeno-21` refeito, as quatro fileiras do teclado e o OK inteiro aparecem sem rolar e há um só controle laranja na tela; `SkipConfirmUiTest` passa (o teste `turningOffTakesASumAndCanBeUndone` já falha na linha de base por outro motivo, a notificação `pre_alarm`; não pode ganhar falha nova); `StringsTest` passa.
- Status: CONFIRMADO — no print pequeno-21 a fileira do 0 e metade do OK ficam atrás de MANTER/DESATIVAR; no print há dois laranjas com a mesma função; confirm() sai em silêncio com typed em branco (SkipConfirmViewModel.kt:92). SkipConfirmUiTest linhas 91, 98 e 130 usam skip_confirm_turn_off, como descrito. Viável.

### UX2-04: Teclado da missão é dimensionado só pela largura e fica longe do visor
- Gravidade: média
- Evidência: `…/ui/mission/MissionScreen.kt:188` (tamanho da tecla só pela largura), `:201` (visor com 150 dp fixos), `:227-229` (o espaço livre vai todo para entre o visor e o teclado); print `pequeno-15-missao.png` (em 640 dp de altura, sem barras do sistema, o conteúdo ocupa a tela inteira sem nenhuma folga: com barra de status e de navegação reais, a última fileira e o "Não consigo resolver" saem da tela); print `pt-15-missao.png` (cerca de 245 dp vazios entre a conta e as teclas: o olho pula esse vão a cada dígito para conferir o que digitou).
- Como reproduzir: abrir a missão de contas num celular baixo (640 dp com barras do sistema) e num alto (891 dp). O cálculo do celular baixo é por conta, a partir do print sem folga: não há aparelho para confirmar.
- O que mudar (em `MathBody`):
  1. Dentro do `BoxWithConstraints`: `val displayH = if (maxHeight < 520.dp) 132.dp else 150.dp`; `val byWidth = (maxWidth - 40.dp - 16.dp * 3) / 4`; `val byHeight = (maxHeight - displayH - 167.dp) / 4` (167 = 18 de margem do visor + 24 + 3 vãos de 14 do teclado + 16 + 51 da tecla "Não consigo resolver" com a base + 16); `val keySize = minOf(byWidth, byHeight).coerceIn(52.dp, 76.dp)`. Usar `displayH` no `.height(...)` do visor.
  2. Mover o `Spacer(Modifier.weight(1f))` da linha 228 para o começo da coluna, antes do visor. A ordem fica: espaço elástico, visor, 24 dp, teclado, 16 dp, "Não consigo resolver", 16 dp. O visor fica colado no teclado, como numa calculadora de mão, e o vazio sobe para baixo do medidor de tempo.
- Critério de pronto: print `pequeno-15` refeito com teclas menores e folga; print `pt-15` refeito com o visor a 24 dp do teclado; `ScreenshotTourSmall` rodado também com `h568dp` (trocar o qualificador só para conferir, sem versionar) mostra todas as teclas e o "Não consigo resolver" sem rolar; `RingFlowUiTest` passa.
- Status: CONFIRMADO — MissionScreen.kt: keySize só por maxWidth, visor com height(150.dp) e Spacer(weight) entre visor e teclado, como descrito. Parte do celular baixo é cálculo (sem aparelho), mas o vão entre visor e teclado no celular alto é observável no código e no print pt-15.

### UX2-05: As duas ações da tela de toque e as mensagens da missão estão em 12 e 13 sp, e o botão da missão muda entre Toque e Espere
- Gravidade: média
- Evidência: `…/ui/theme/Type.kt:49-50` (`espereLabel` 13 sp, `espereSub` 12 sp); `…/ui/ring/RingScreen.kt:185-199` (botão de 96 dp, legenda `labelWide` de 12 sp sobre plaquinha) contra `:279-288` (botão de 84 dp, legenda `label` de 12 sp sem plaquinha); `…/ui/mission/MissionScreen.kt:208-216` e `…/ui/skip/SkipConfirmScreen.kt:218-226` ("Digite o resultado" e "Errado. Tente de novo." em `caption` de 12 sp); prints `pt-13-tocando.png`, `pt-14-espere.png`, `pt-16-missao-errou.png`.
- Como reproduzir: abrir a tela de toque com o celular a um braço de distância, sem óculos. O relógio se lê; "ESPERE 5 MIN", "SEM LIMITE" e "COMEÇAR MISSÃO" quase não. Tocar em Espere: o botão laranja encolhe de 96 para 84 dp, perde a plaquinha e desce alguns dp.
- O que mudar:
  1. `Type.kt`: `espereLabel` passa a 16 sp com `lineHeight = 20.sp`; `espereSub` passa a 13 sp com `lineHeight = 16.sp`; criar `val actionLabel = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.14.em)`.
  2. `RingScreen.kt`: criar um composable privado `StartMissionButton(label: String, onClick: () -> Unit, modifier: Modifier)` com `size = 96.dp`, `baseDepth = 5.dp`, `gap = 10.dp`, `labelStyle = WwuType.actionLabel`, `labelColor = Ink`, `labelPlate = EspereLabelPlate`, `softShadow = softShadow(24.dp, 14.dp, 0.45f)` e `PlayGlyph(24.dp, 30.dp, nudge = 3.dp)`. Usar o mesmo nas duas telas, centralizado, com `padding(start = 24.dp, end = 24.dp, bottom = 36.dp)`.
  3. `MissionScreen.kt:210` e `SkipConfirmScreen.kt:220`: trocar `WwuType.caption` por `WwuType.bodyS` (14 sp) nas mensagens de dica e de erro. O contador de caracteres da frase (`MissionScreen.kt:330`) também.
- Critério de pronto: prints 13, 14, 15, 16 e 21 refeitos nas quatro variantes sem texto cortado nem quebrado no meio de palavra; no `fonte-grande-13`, a barra do Espere (84 dp) comporta as duas linhas; o botão laranja ocupa a mesma posição e o mesmo tamanho nos prints 13 e 14; `RingFlowUiTest` e `PortugueseUiTest` passam.
- Status: CONFIRMADO — Type.kt: espereLabel 13 sp e espereSub 12 sp; botão da missão 96 dp (RingScreen:185) contra 84 dp sem plaquinha (:279-288); mensagens da missão e do pulo em caption 12 sp. Print pt-13 confere. Sem custo de performance.

### UX2-06: "ESPERE 5 MIN · SEM LIMITE" se lê como aviso, não como botão
- Gravidade: média
- Evidência: `app/src/main/res/values-pt/strings.xml:97-98`; `…/ui/components/Hardware.kt:846-931` (barra estriada escura, quase da cor do fundo, sem nada que diga "toque"); print `pt-13-tocando.png`. Em inglês o título é "SNOOZE 5 MIN" (`values/strings.xml:94`), que já é um verbo de ação conhecido.
- Como reproduzir: mostrar a tela de toque a alguém que nunca usou o app. "Espere 5 min" no imperativo parece o app mandando esperar. "Sem limite" não diz limite de quê, e é a única informação da tela que convida a continuar dormindo, o contrário do propósito do app.
- O que mudar: só texto, o comportamento do Espere fica igual. `espere_sub`: "Sem limite" vira "Toque para adiar"; em inglês "No limit" vira "Tap to snooze". Depois do primeiro uso a barra continua mostrando "USADO 2×" (`espere_used_n`), como hoje. Atualizar o texto literal em `app/src/test/java/com/wakewakeup/ui/PortugueseUiTest.kt:113` de "SEM LIMITE" para "TOQUE PARA ADIAR".
- Critério de pronto: print `pt-13` refeito mostra "ESPERE 5 MIN" e "TOQUE PARA ADIAR"; `PortugueseUiTest.theRingScreen`, `RingFlowUiTest.afterAnEspereTheBarCountsInsteadOfSayingNoLimit` e `StringsTest` passam.
- Status: CONFIRMADO — print pt-13 mostra 'ESPERE 5 MIN / SEM LIMITE'; espere_sub só é usado em RingScreen:133 e :234; PortugueseUiTest.kt:113 tem o literal 'SEM LIMITE'. Só texto, comportamento igual. Obs.: 'Toque para adiar' funciona, mas conviria alinhar o verbo com o título 'Espere'.

### UX2-07: O Bom dia informa, mas não recompensa, e quem acorda mais rápido vê "00 MIN"
- Gravidade: média
- Evidência: `…/ui/goodmorning/GoodMorningScreen.kt:70` (`minutes = (durationSec + 30) / 60`, que dá 0 para quem termina em menos de 30 s) e `:83-100` (só "Você levou NN min para acordar de verdade."); print `pt-18-bom-dia.png` (número grande, três leituras, nada que diga se foi bom ou ruim; quase metade da tela é degradê vazio). É o molde "número gigante + rótulo + três estatísticas" que a skill impeccable pede para não usar por reflexo: aqui ele se justifica como visor, mas falta o sentido do número.
- Como reproduzir: concluir a missão em dois dias seguidos, um em 12 min e outro em 4 min. As duas telas são iguais, só muda o número. Concluir em 20 s: a tela mostra "00 MIN".
- O que mudar:
  1. `…/ui/ring/RingSessionViewModel.kt`: novos estados `var weekAverageMinutes by mutableStateOf<Int?>(null)` e `var comparisonReady by mutableStateOf(false)`. Em `loadCompanions` (mesma trava de uma vez por sessão), ler `container.wakeHistoryRepository.observeLastDays(7).first()`, tirar o registro de agora (mesma data de hoje, mesmo `wokeHour`, `wokeMinute` e `durationSec` de `session.finished`) e calcular a média dos outros em minutos com o mesmo arredondamento das estatísticas (`(segundos / 60.0).roundToInt()`); lista vazia dá `null`. No fim, `comparisonReady = true`.
  2. `GoodMorningScreen`: novos parâmetros `averageMinutes: Int? = null` e `comparisonReady: Boolean = false`, passados em `…/session/RingActivity.kt:71-79`. Logo abaixo de "para acordar de verdade." (linha 99), quando `comparisonReady`, uma linha em `WwuType.bodyStrong`, cor `Ink`: sem histórico, `gm_first`; `averageMinutes - minutes >= 1`, `gm_faster` com a diferença; igual a 0, `gm_same`; menor que 0, `gm_slower` com a diferença em valor absoluto.
  3. Strings novas: `gm_first` = "Primeiro despertar registrado." / "Your first wake-up on record."; `gm_faster` = "%1$d min mais rápido que a sua média da semana." / "%1$d min faster than your weekly average."; `gm_same` = "Na sua média da semana." / "Right on your weekly average."; `gm_slower` = "%1$d min acima da sua média da semana." / "%1$d min over your weekly average."
  4. Menos de 1 minuto: se `finished.durationSec < 60`, o `FlipNumber` mostra `"%02d".format(finished.durationSec)`, a unidade é `second_short` em maiúsculas e o `spokenText` é `"${finished.durationSec} s"`. A comparação do passo 2 continua usando `minutes`.
  5. Um único momento de movimento: o `FlipNumber` entra em "00" e troca para o valor 350 ms depois (`LaunchedEffect(Unit)` com `delay(350)`), para os dígitos virarem uma vez. Com `rememberReduceMotion()` verdadeiro, já entra no valor. O `spokenText` é sempre o valor final. Se `RingFlowUiTest` conferir os dígitos na hora, avançar o relógio do teste 500 ms.
- Critério de pronto: teste novo de UI com histórico semeado (média de 10 min, despertar de 6 min) encontra "4 min mais rápido que a sua média da semana."; sem histórico encontra `gm_first`; com `durationSec = 20` a tela mostra "20" e "SEG"; nada de streak, pontos, confete ou som; `RingFlowUiTest.theWholeRitualFromRingingToGoodMorning` e `StringsTest` passam; nenhuma dependência nova.
- Status: CONFIRMADO — GoodMorningScreen.kt:70 (durationSec+30)/60 dá 00 abaixo de 30 s; tela só informa, sem comparação. observeLastDays(7) existe no WakeHistoryRepository, wakeHistoryRepository está no AppContainer e loadCompanions é ponto natural para a leitura única. Sem dependência nova; o item de animação do passo 5 é opcional e deve respeitar rememberReduceMotion.

### UX2-08: A tecla "−" ocupa lugar no teclado e nunca serve
- Gravidade: média
- Evidência: `…/session/MissionGenerator.kt:43-58` (as três dificuldades são soma ou multiplicação de números positivos: a resposta nunca é negativa); `…/ui/skip/SkipConfirmViewModel.kt:43` (o pulo usa a soma média); `…/ui/components/Keypads.kt:87-90` (a tecla "−" fica ao lado do "0", na fileira do polegar); prints `pt-15-missao.png` e `pt-21-confirmar-desativar.png`.
- Como reproduzir: na missão, tocar em "−" por engano ao mirar o "0" ou o OK: a resposta vira "−8", fica errada, e nada explica que o sinal não faz parte de nenhuma conta.
- O que mudar: em `Keypads.kt`, trocar a fileira das linhas 87-90 por uma só tecla: `CalcKey("0", CalcKind.Digit, onKey, width = key * 3 + GAP_H * 2, height = key)`. Não mexer em `CalcKeys.SIGN`, em `AlarmRingService.onMissionKey` nem em `SkipConfirmViewModel.pressKey`: os testes de serviço (`AlarmRingServiceTest:337-353`, `AlarmRingServiceEdgeTest:135-145`) mandam o sinal direto e continuam valendo. Apagar `cd_key_sign` dos dois arquivos de strings.
- Critério de pronto: prints 15, 16 e 21 refeitos sem a tecla "−" e com o "0" do tamanho de três teclas; `AlarmRingServiceTest`, `AlarmRingServiceEdgeTest`, `RingFlowUiTest`, `SkipConfirmUiTest` e `StringsTest` sem falha nova.
- Status: CONFIRMADO — MissionGenerator só gera soma ou multiplicação de positivos, então a resposta nunca é negativa; a tecla '−' está em Keypads.kt:89 ao lado do 0 e não há teste de UI que use cd_key_sign (testes de serviço mandam o sinal direto e seguem valendo). Viável.

### UX2-09: Estatísticas sem registros mostra dois cartões "– min", 84 LEDs apagados e uma frase pequena no fim
- Gravidade: média
- Evidência: `…/ui/stats/StatsScreen.kt:92-103` (os dois cartões aparecem com "–" e a unidade "min" mesmo sem dado; legenda vazia), `:129-131` (a única orientação é `stats_empty` em 13 sp apagado, depois do gráfico); prints `en-19-estatisticas.png`, `pequeno-19-estatisticas.png` e `fonte-grande-19-estatisticas.png`, que mostram o quadro de carregamento, igual ao vazio menos essa frase; `app/src/test/java/com/wakewakeup/ui/ScreenshotTour.kt:248` (espera só o título do gráfico, por isso três variantes saíram antes dos dados).
- Como reproduzir: criar o primeiro alarme e abrir Estatísticas antes de concluir qualquer missão.
- O que mudar:
  1. `StatsScreen.kt`: quando `state.loaded && !state.hasData`, mostrar `stats_empty` logo abaixo do `BackHeader`, antes dos cartões, em `WwuType.body`, cor `Ink`, com `padding(start = 20.dp, end = 20.dp, top = 12.dp)`. Apagar o `Text` das linhas 129-131.
  2. `SummaryPlate`: não desenhar a unidade "min" quando o valor for o traço (comparar com `dash` ou receber `showUnit: Boolean`).
  3. Texto de `stats_empty`: "Ainda sem despertares. Conclua a missão de um alarme e o tempo que você levou aparece aqui." / "No wake-ups yet. Finish an alarm's mission and the time it took shows up here."
  4. `ScreenshotTour.stats()`: antes do `shot`, esperar também `compose.waitForText(str(R.string.woke_at_time, "06:42"))`, para as quatro variantes saírem com dados.
- Critério de pronto: `StatsUiTest.withNoWakeUpsYet` passa (continua achando `stats_empty` e pelo menos dois "–"); `AlarmListUiTest` (linha 434) passa; os quatro prints 19 refeitos mostram a semana com dados; um print novo do estado vazio mostra a frase no topo e nenhum "min" solto.
- Status: CONFIRMADO COM RESSALVA — o estado vazio do código confere (StatsScreen: cartões '–' com unidade 'min' e stats_empty em bodyXS só depois do gráfico; SummaryPlate sempre desenha a unidade). Mas a premissa dos prints está errada: en-19, pequeno-19 e fonte-grande-19 mostram a tela COM dados (12 min, barras), não o quadro de carregamento; logo o passo 4 (esperar texto no ScreenshotTour) é desnecessário e deve ser dropado. Implementar só os passos 1 a 3; o critério de prints 19 refeitos não se aplica, e o print do estado vazio é novo.

### UX2-10: Acertar uma conta quase não dá retorno, e o progresso é só um texto
- Gravidade: baixa
- Evidência: `…/ui/mission/MissionScreen.kt:145-157` (vibração e tremida só no erro), `:167-171` (o acerto é a frase "Certo. Próxima conta." em 12 sp), `:114-122` (o progresso é "CONTA 2 DE 3"); print `pt-15-missao.png`.
- Como reproduzir: missão com 3 contas. Acertar a primeira: a conta troca por outra parecida, a resposta some e nada vibra. Com sono, dá para achar que o OK não pegou.
- O que mudar:
  1. Em `MathBody`: `LaunchedEffect(mission.index) { if (mission.justCorrect) haptic.performHapticFeedback(HapticFeedbackType.Confirm) }`, com `val haptic = LocalHapticFeedback.current`.
  2. Luzes de progresso dentro do visor, na mesma linha da conta: trocar o `VfdText` da linha 206 por uma `Row(verticalAlignment = CenterVertically)` com o `VfdText` em `Modifier.weight(1f)` e, à direita, uma `Row(horizontalArrangement = spacedBy(6.dp), modifier = Modifier.clearAndSetSemantics {})` com `mission.count` componentes `Lamp(Vfd, lit = i < mission.index, size = 8.dp)`. Só quando `mission.count > 1`. Não aumenta a altura do visor. O texto "CONTA 2 DE 3" continua (TalkBack lê por ele).
- Critério de pronto: print 15 refeito mostra 3 luzes com 1 acesa; teste de UI existente do fluxo completo passa; com 10 contas no celular de 360 dp as luzes cabem ao lado de uma conta de multiplicação ("49 × 19 =").
- Status: CONFIRMADO — haptic só no erro (rememberShake), acerto só vira frase em caption 12 sp; HapticFeedbackType.Confirm já é usado em Hardware.kt:688 e Lamp existe em Hardware.kt:582, então não há dependência nova. Gravidade baixa, ganho claro.

### UX2-11: A mesma ação se chama "pular" na lista e no Bom dia e "desativar" na notificação, na confirmação e no recibo
- Gravidade: baixa
- Evidência: `app/src/main/res/values-pt/strings.xml:15-17` ("Pular só %1$s?", "Pular"), `:241` ("Pular também %1$s?") contra `:225-240` ("Você quer desativar o próximo alarme?", "Desativar", "Desativar só desta vez?", "Desativado só desta vez."). No Bom dia a pessoa toca em PULAR e o recibo que chega diz "Desativado só desta vez." (`…/ui/ring/RingSessionViewModel.kt:157-165` chama `postReceipt`). O título da notificação tem 38 a 40 caracteres e pode ser cortado no celular pequeno; a pergunta que dá nome ao recurso ("Já acordou?") está escondida no meio do texto. Nenhuma das quatro notificações define cor (`…/session/AlarmRingService.kt:157` e `:210`, `…/session/PreAlarmCheck.kt:140` e `:171`), então o ícone do sol sai cinza. Em Estatísticas, "1,0 Espere por alarme" (`strings.xml:149`, print `pt-19-estatisticas.png`) não se lê como frase.
- Como reproduzir: receber a notificação uma hora antes de um alarme da manhã com o celular em uso; ou, no Bom dia com alarme de reserva, tocar em PULAR e ler o recibo.
- O que mudar (só strings, mais uma linha por notificação):
  - `pre_alarm_title_one` e `pre_alarm_title_many`: "Já acordou?" / "Already up?"
  - `pre_alarm_text_one`: "%1$s toca às %2$s. Quer pular só desta vez?" / "%1$s rings at %2$s. Skip it just this once?"
  - `pre_alarm_text_many`: "%1$s. Quer pular todos só desta vez?" / "%1$s. Skip them all just this once?"
  - `pre_alarm_action`: "Pular" / "Skip"
  - `pre_alarm_channel_desc`: trocar "desativá-lo" por "pulá-lo" / "turn it off" por "skip it"
  - `skip_confirm_title`: "Pular só desta vez?" / "Skip just this once?"
  - `skip_confirm_done`: "Pronto, pulado só desta vez." / "Done, skipped just this once."
  - `skip_receipt_text`: "Pulado só desta vez." / "Skipped just this once."
  - `waits_per_wake` (só PT): "Espere: %1$s por alarme"
  - Nos quatro `NotificationCompat.Builder`: `.setColor(0xFFE8663C.toInt())` (o laranja `Action`).
- Critério de pronto: `grep -i "desativ" app/src/main/res/values-pt/strings.xml` não acha mais nada no fluxo de pulo; `PreAlarmCheckTest`, `SkipConfirmUiTest`, `StatsUiTest`, `PortugueseUiTest` e `StringsTest` sem falha nova (os testes usam os recursos, não o texto literal; conferir literais com `grep -rn "Desativ" app/src/test`).
- Status: CONFIRMADO — strings PT: 'Pular' na lista (linhas 15-17, 241) e 'desativar' no fluxo de notificação/confirmação/recibo (225-240); nenhum dos builders (AlarmRingService:157,210; PreAlarmCheck:140,171) chama setColor; print pt-19 mostra '1,0 Espere por alarme'. Só strings, testes usam recursos (StatsUiTest:94 usa str()); conferir literais com grep antes.

### UX2-12: Nas telas de toque e de Espere o conteúdo fica preso no topo e sobra um vão grande no meio
- Gravidade: baixa
- Evidência: `…/ui/ring/RingScreen.kt:139-143` (`Spacer` fixo de 56 dp e conteúdo ancorado em cima) e `:240-244` (72 dp); prints `pt-13-tocando.png` (cerca de 320 dp vazios entre o medidor de volume e o botão) e `pt-14-espere.png` (cerca de 350 dp vazios).
- Como reproduzir: abrir as duas telas num celular alto (891 dp). No celular pequeno (`pequeno-13`, `pequeno-14`) o problema não aparece.
- O que mudar: nas duas colunas internas que rolam, envolver em `BoxWithConstraints(Modifier.weight(1f).fillMaxWidth())` e usar na coluna `Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)` com `verticalArrangement = Arrangement.Center`. Trocar o `Spacer` inicial (56 e 72 dp) por `Spacer(Modifier.height(24.dp))`, mantendo o de 24 dp no fim. O bloco do relógio fica centrado entre a barra do Espere e o botão da missão, com o vazio dividido em dois.
- Critério de pronto: prints `pt-13` e `pt-14` refeitos com o bloco centrado (diferença entre o vão de cima e o de baixo menor que 24 dp); `pequeno-13` e `pequeno-14` sem sobreposição e sem corte; nenhuma recomposição nova (o relógio continua atualizando 1 vez por minuto e a contagem 1 vez por segundo).
- Status: CONFIRMADO — RingScreen.kt:139-143 e 240-244 ancoram o conteúdo com Spacer fixo de 56/72 dp; print pt-13 mostra grande vão vazio entre o medidor e o botão. Centralizar com BoxWithConstraints é viável e não adiciona recomposição.

### UX2-13: Na frase, "Errado. Tente de novo." não diz onde está o erro
- Gravidade: baixa
- Evidência: `…/ui/mission/MissionScreen.kt:328-335` (no erro, o contador vira `wrong_try_again`), `:266-273` (o visor já acende o trecho certo e sublinha a próxima letra, mas o texto não aponta para isso); print `pt-17-missao-frase.png`.
- Como reproduzir: missão de frase. Digitar a frase com uma letra trocada no meio e confirmar. Aparece "Errado. Tente de novo." e a pessoa relê tudo procurando o erro.
- O que mudar: duas strings novas e uma condição na linha 329. Se `mission.wrong` e `matched < phrase.length`: `phrase_wrong_here` = "Não confere. Corrija a partir da letra sublinhada." / "Doesn't match. Fix it from the underlined letter." Se `mission.wrong` e `matched == phrase.length`: `phrase_wrong_extra` = "Não confere. Tem texto sobrando no fim." / "Doesn't match. There's extra text at the end." A conta continua com `wrong_try_again`.
- Critério de pronto: `RingFlowUiTest.aPhraseAlarm` (linha 268) passa a esperar `phrase_wrong_here` no lugar de `wrong_try_again` (ele digita "nada a ver", que erra já na primeira letra) e passa; teste de UI novo digita a frase do dia inteira mais " x" no fim, confirma e acha `phrase_wrong_extra`; `StringsTest` passa.
- Status: CONFIRMADO — wrong_try_again é a única mensagem no erro da frase (MissionScreen.kt:~329); o visor já sublinha a próxima letra via matchedPrefixLength; RingFlowUiTest.kt:268 espera wrong_try_again com 'nada a ver', como descrito. Cuidado menor: se o texto digitado é só um prefixo correto, 'corrija a partir da letra sublinhada' é levemente impreciso, aceitável.
