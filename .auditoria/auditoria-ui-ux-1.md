# Auditoria de UI e UX 1: Permissões, Lista de alarmes, Editar alarme, Tarefa, Som e Rádio

Feita em 2026-11-26 pelo AUDITOR DE UI E UX 1 (somente leitura). Critério: skill impeccable do projeto (craft-floor, critique-rubric, android, audit.native) e ux-writing. Onde a skill bate de frente com a identidade de rádio-relógio dos anos 70, a identidade vence.

Base de código citada: `app/src/main/java/com/wakewakeup/` (abreviado como `…/`). Prints em `.auditoria/prints/antes/app/`. Foram abertos os prints 01 a 12 e 20 nas quatro variantes (pt, en, pequeno, fonte-grande).

## Visão geral

- **Cara de IA: baixa.** O app tem um mundo próprio e coerente: visor, placas, teclas com base sólida, LEDs, grade de alto-falante só na faixa PRÓXIMO, dial de sintonia na tela de Som. Não há emoji no lugar de ícone, vidro, degradê decorativo nem fonte única (Barlow Condensed nos números, Instrument Sans no resto). O único vício de template é o rótulo "ANTES DE COMEÇAR" em cima do título das Permissões (entra no UX1-02).
- **O que está bom e deve ficar:** dígitos flip, interruptor deslizante com 48 dp de toque, gaveta "Pular só sexta?" ao desligar um alarme recorrente, aviso de volume baixo com tecla que resolve, confirmação ao sair do editor com alterações, exclusão em dois toques com a tecla parada no lugar, estado vazio com orientação e botão, leitura por TalkBack bem cuidada, alvos de 48 dp ou mais em todas as teclas conferidas, contraste de texto acima de 4,5:1 nos pares conferidos (InkMuted sobre Housing 7,3:1).
- **Onde dói:** acertar os minutos no editor (UX1-01), o primeiro botão que o usuário vê na vida não faz o que diz (UX1-02), a tela de Som esconde o que está escolhido e o "Aumentar aos poucos" (UX1-03), a prévia de som não avisa quando falha (UX1-04) e o celular pequeno mostra pouco na lista e no editor (UX1-05 e UX1-06).
- **Inglês:** nenhum texto cortado nos prints `en-*` em fonte normal. O risco está na fonte grande (UX1-07).
- **Desempenho:** nenhuma das mudanças abaixo adiciona dependência, imagem, fonte ou trabalho fora da thread de UI. Nada encosta no orçamento.

Total: 13 itens (0 críticos, 1 alto, 8 médios, 4 baixos).

---

### UX1-01: Acertar os minutos exige arrasto longo e não dá para digitar a hora
- Gravidade: alta
- Evidência: `…/ui/edit/EditAlarmScreen.kt:401` (`DRAG_STEP = 28.dp`: cada minuto custa 28 dp de arrasto); `:474` (`exponentialDecay(frictionMultiplier = 2.5f)`: a distância do giro é velocidade / 10,5, então um flick comum de 1000 a 3000 dp/s anda só 3 a 10 minutos); `:504-516` (os vizinhos de cima e de baixo andam 1 por toque); `:393-396` (o passo de 10 minutos existe só como ação do TalkBack). Prints `pt-07-novo-alarme.png` e `pequeno-07-novo-alarme.png`: o tambor fica entre 131 e 205 dp do topo da tela, então arrastar para cima (que avança) tem no máximo uns 168 dp de curso, ou seja, 6 minutos por arrasto.
- Como reproduzir: tocar em "+" (abre em 07:00) e tentar marcar 07:30. São 30 passos: 840 dp de arrasto (mais que a altura de um celular de 640 dp), ou 5 arrastos para cima, ou de 3 a 10 flicks, ou 30 toques no vizinho. Não existe como digitar "0730".
- O que mudar:
  1. `Drum` ganha dois parâmetros: `dragStep: Dp` e `friction: Float`. Horas: `dragStep = 28.dp`, `friction = 2.5f` (como hoje). Minutos: `dragStep = 16.dp`, `friction = 1.5f`. Trocar os usos de `DRAG_STEP` e do `2.5f` fixo por eles e apagar a constante `DRAG_STEP`.
  2. Digitar a hora: no `Box` do tambor (o que tem `semantics` e `pointerInput`), acrescentar ANTES do `.pointerInput(Unit)` um `.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = stringResource(R.string.cd_type_time)) { spin?.cancel(); onType() }`. `Drum` recebe `onType: () -> Unit`. O arrasto continua funcionando: o detector de arrasto consome o gesto depois do limiar e cancela o clique.
  3. `TimePickerVisor` guarda `var typing by rememberSaveable { mutableStateOf(false) }`, passa `onType = { typing = true }` aos dois tambores e, quando `typing`, mostra `TimeEntryDialog(onDismiss = { typing = false }, onConfirm = { h, m -> onHour(h); onMinute(m); typing = false })`.
  4. `TimeEntryDialog` (novo, privado, no mesmo arquivo, no molde do `LeaveDialog`: `Dialog` + `Column` com `faceplate(WwuShape.faceplateLarge, softShadow = false).padding(20.dp)`): título `type_time_title` em `WwuType.titleS`; um visor (`visor(WwuShape.visorSmall, lip = false)`, altura 64 dp, conteúdo centralizado) com um `BasicTextField` cujo valor é `digits` (`rememberSaveable`, começa vazio, `onValueChange = { digits = it.filter(Char::isDigit).take(4) }`), `KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)`, `KeyboardActions(onDone = { confirmar })` e `decorationBox = { inner -> Box(contentAlignment = Center) { VfdText(formatado, 36.sp.nonScaling(), color = VfdBright); Box(Modifier.alpha(0f)) { inner() } } }`, onde `formatado = digits.padEnd(4, '_').let { "${it.take(2)}:${it.drop(2)}" }`. Foco pedido ao abrir (`FocusRequester` + `LaunchedEffect(Unit)`). Abaixo, `type_time_hint` em `WwuType.caption`, `InkMuted`; quando `digits.length == 4` e a hora não existe (hora > 23 ou minuto > 59), o texto vira `type_time_invalid` em `ErrorText`. Rodapé: `GraphiteKey(cancel)` e `OrangeKey(confirm)`, alinhados à direita com 10 dp entre eles; confirmar só age com 4 dígitos válidos.
  5. Strings novas nos dois arquivos: `cd_type_time` = "Digitar a hora" / "Type the time"; `type_time_title` = "Digite a hora" / "Type the time"; `type_time_hint` = "Quatro números. Ex.: 0645 para 06:45." / "Four digits, e.g. 0645 for 06:45."; `type_time_invalid` = "Essa hora não existe. Use de 00:00 a 23:59." / "That time doesn't exist. Use 00:00 to 23:59."; `cancel` = "Cancelar" / "Cancel". O botão de confirmar usa a `confirm` que já existe.
- Critério de pronto: teste novo em `AlarmListUiTest`: abrir novo alarme, clicar no tambor das horas, digitar "0645", confirmar e salvar; o alarme salvo tem `hour == 6` e `minute == 45`. Segundo teste: digitar "2575" mostra `type_time_invalid` e confirmar não fecha o diálogo. `theTimeDrumsWorkWithTalkBack` continua passando. `StringsTest` passa. Print novo do diálogo nas quatro variantes.
- Status: CONFIRMADO — DRAG_STEP = 28.dp (EditAlarmScreen.kt:401) e frictionMultiplier 2.5f (:474) conferidos; sem digitação de hora; viável, só Compose, sem dependência

### UX1-02: Nas Permissões, "CONTINUAR" não continua no primeiro toque, e vindo da lista a tela não tem seta de voltar
- Gravidade: média
- Evidência: `…/ui/permissions/PermissionsScreen.kt:145-159` (com permissão faltando, o primeiro toque só faz `nag = true`; o rótulo troca para "Continuar mesmo assim" e aparece um texto de 12 sp no pé); `:160-170`; `:93` (rótulo "ANTES DE COMEÇAR" em cima do título, padrão banido no craft-floor da skill); `:85-96` (sem `BackHeader`, nem quando `fromList`); `res/values-pt/strings.xml:164` (parágrafo de três frases que descreve a posição dos itens: "os três primeiros", "o último"). Prints `pt-05-permissoes.png` e `pt-06-permissoes-aviso.png` (mesma tela antes e depois do toque: só mudam o rótulo do botão e a legenda), `pequeno-05-permissoes.png` (a introdução ocupa 4 linhas e empurra a terceira placa para o esmaecido).
- Como reproduzir: primeira abertura com uma permissão faltando. Tocar em "CONTINUAR": nada navega; o botão passa a dizer "CONTINUAR MESMO ASSIM". É preciso tocar de novo. Depois, na lista, tocar no aviso "O alarme pode não tocar": a tela de permissões abre sem "‹", ao contrário de Editar, Missão, Som, Rádio e Estatísticas.
- O que mudar:
  1. Apagar o estado `nag`. Quando `!all && !fromList`: o botão já nasce com `continue_anyway` (`primary = false`) e o texto `perm_nag` fica visível desde o início; um toque continua. Quando `all`: `continue_label`, `primary = true`, texto `perm_all_set` (como hoje). A altura mínima de 34 dp do texto já existe, então o layout não muda.
  2. Quando `fromList`: no lugar do bloco de título, usar `BackHeader(title = stringResource(R.string.perm_short_title), backDescription = stringResource(R.string.cd_back), onBack = onContinue)` e, dentro da rolagem, só o `perm_body`. O botão do pé passa a dizer `back_to_list`, com `primary = all`.
  3. Apagar o rótulo `perm_eyebrow` (linha 93) e a string nos dois arquivos. O título "Para o alarme tocar de verdade" se sustenta sozinho.
  4. Encurtar `perm_body`: PT "O Android pode atrasar ou silenciar alarmes para economizar bateria. Libere os itens abaixo para o alarme tocar mesmo com o celular bloqueado." EN "Android can delay or silence alarms to save battery. Allow the items below so the alarm rings even with the phone locked."
  5. Strings novas: `perm_short_title` = "Permissões" / "Permissions"; `back_to_list` = "Voltar para a lista" / "Back to the list".
  6. Atualizar o comentário KDoc do topo da tela e os testes que dependem dos dois toques: `PermissionsUiTest.firstLaunchWalksThroughThePermissionsOnce` (um toque em `continue_anyway`), `ListWarningsUiTest.theWarningOpensThePermissionsAndOneTapComesBack` (volta por `back_to_list` ou pela seta) e `ScreenshotTour.permissions` (o print 06 passa a ser a tela aberta pelo aviso da lista).
- Critério de pronto: com permissão faltando, um único toque no botão do pé leva à lista, e o texto de risco está na tela antes desse toque (teste). Aberta pelo aviso da lista, a tela tem "‹" com `cd_back` e volta em um toque (teste). `perm_eyebrow` não existe mais. Prints 05 e 06 refeitos nas quatro variantes; no `pequeno`, a terceira placa aparece inteira.
- Status: CONFIRMADO — estado nag e perm_eyebrow conferidos em PermissionsScreen.kt:65,93,146-156; sem BackHeader; strings existem; viável

### UX1-03: Na tela de Som, o som escolhido e o "Aumentar aos poucos" ficam fora da vista, e a lista não tem seções
- Gravidade: média
- Evidência: `…/ui/sound/SoundPickerScreen.kt:107-120` (ordem: sons do app, "Padrão do sistema", depois todos os toques de alarme do celular lidos em `:344-356`, sem nenhum título entre eles); `:157-244` (a placa "Aumentar aos poucos" é o último filho da coluna rolável, e nada rola até a linha selecionada ao abrir); `:208-219` ("Música do celular" depois de tudo). Prints `pequeno-11-som.png` (o visor diz "PADRÃO DO SISTEMA · SELECIONADO", mas a linha acesa e o interruptor não aparecem na tela) e `pt-11-som.png` (com só 9 sons o interruptor já está na borda de baixo; num celular real, os toques do sistema entram antes dele).
- Como reproduzir: num celular de 360x640 dp, Editar alarme > Som. A lista abre no topo; para ver qual som está marcado ou para achar "Aumentar aos poucos" é preciso rolar até o fim. Num aparelho com 20 toques de alarme, são três telas de rolagem.
- O que mudar (ordem dos filhos da coluna rolável, de cima para baixo):
  1. A placa "Aumentar aos poucos" (bloco das linhas 222-242) sobe para primeiro, com `top = 12.dp`.
  2. Placa "fontes": a linha Rádio e, depois de um `PlateDivider`, a linha "Música do celular" (as duas com seta, que abrem outra coisa).
  3. `PrintLabel(stringResource(R.string.sounds_app))` com `padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)` e uma placa só com os sons de `bundled`.
  4. `PrintLabel(stringResource(R.string.sounds_phone))` com o mesmo padding e uma placa com "Padrão do sistema", os `systemSounds` e, se houver, o arquivo escolhido.
  5. O `selectableGroup()` sai das placas e vai para a coluna rolável, para o TalkBack continuar lendo um grupo só.
  6. Ao abrir, trazer a linha selecionada para a vista: `val selectedRequester = remember { BringIntoViewRequester() }`; `SoundRow` ganha `modifier: Modifier = Modifier`; a linha selecionada recebe `Modifier.bringIntoViewRequester(selectedRequester)`; `LaunchedEffect(systemSounds != null) { if (systemSounds != null) selectedRequester.bringIntoView() }`. Se o selecionado for uma rádio, nada rola (a linha Rádio já está no topo).
  7. Strings novas: `sounds_app` = "Sons do app" / "App sounds"; `sounds_phone` = "Sons do celular" / "Phone sounds".
- Critério de pronto: prints `pt-11` e `pequeno-11` refeitos mostram "Aumentar aos poucos" no topo, os dois títulos de seção e a linha selecionada visível sem rolar. `choosingOneOfTheAppsSoundsAndTurningTheRampOff`, `pickingASongFromThePhone`, `goingBackToTheSystemDefault` e `wakingUpToARadioStation` continuam passando. `StringsTest` passa.
- Status: CONFIRMADO — ordem dos filhos e ausência de seções conferidas em SoundPickerScreen.kt:106-115; print pequeno-11 mostra a linha escolhida e o 'Aumentar aos poucos' fora da vista; viável

### UX1-04: A prévia de som e de rádio não dá para parar e não avisa quando está conectando ou quando falhou
- Gravidade: média
- Evidência: `…/ui/sound/SoundPreview.kt:37-71` (erro do player e estouro dos 10 s de espera chamam `stop()` e mais nada); `…/ui/sound/SoundPickerScreen.kt:339` e `…/ui/sound/RadioScreen.kt:260` (a tecla é sempre um triângulo de play; tocando, só muda de cor); `RadioScreen.kt:104-108` (escolher uma estação grava a escolha e toca a prévia; se a prévia falhar, a estação continua escolhida sem aviso). Prints `pt-12-radio.png` e `pt-11-som.png` (a tecla não tem estado de parar).
- Como reproduzir: Som > Rádio, tocar numa estação cujo stream está fora do ar (ou ligar o modo avião depois que a lista carregou). A lâmpada acende, o triângulo fica âmbar por até 10 s e volta a branco. Nada diz se estava carregando, se tocou ou se falhou, e a estação fica escolhida. Com um som de 2,5 s tocando alto no volume do alarme, tocar de novo na tecla recomeça o som em vez de parar.
- O que mudar:
  1. `SoundPreview`: acrescentar `var loading by mutableStateOf(false)` e `var failed by mutableStateOf<String?>(null)`, os dois com `private set`. Em `play()`: `failed = null; loading = true`. No `setOnPreparedListener`: `loading = false`. No `setOnErrorListener`, no estouro do `START_TIMEOUT_MS` e no `onFailure`: guardar `val key = previewing`, chamar `stop()` e depois `failed = key`. Em `stop()`: `loading = false` (sem limpar `failed`). Novas funções: `fun isFailed(uri: String?) = failed == (uri ?: DEFAULT)` e `fun toggle(uri: String?, durationMs: Long) { if (isPreviewing(uri)) stop() else play(uri, durationMs) }`.
  2. `…/ui/components/Hardware.kt`: novo `StopGlyph(size: Dp, color: Color)` ao lado do `PlayGlyph`: `Canvas(Modifier.size(size)) { drawRoundRect(color, cornerRadius = CornerRadius(1.5.dp.toPx())) }`.
  3. `SoundRow` e `StationRow`: a tecla chama `player.toggle(...)`; o desenho é `if (playing) StopGlyph(10.dp, Vfd) else PlayGlyph(9.dp, 12.dp, Ink, nudge = 1.dp)`; a descrição é `cd_stop_preview` enquanto toca e `cd_play_preview` no resto. Tocar na linha continua escolhendo e tocando a prévia.
  4. `StationRow`: a linha de detalhe mostra `radio_connecting` em `Vfd` quando `playing && player.loading`, e `radio_station_failed` em `ErrorText` quando `player.isFailed(station.streamUrl)`; senão, o detalhe de hoje. `SoundRow`: quando `player.isFailed(option.uri)`, a descrição dá lugar a `sound_failed` em `ErrorText`. As duas funções recebem esses dois booleanos como parâmetros, não o player.
  5. Strings novas: `cd_stop_preview` = "Parar %1$s" / "Stop %1$s"; `radio_connecting` = "Conectando…" / "Connecting…"; `radio_station_failed` = "Não tocou agora. Tente outra rádio." / "Didn't play just now. Try another station."; `sound_failed` = "Não deu para tocar este som." / "Couldn't play this sound."
- Critério de pronto: testes novos de `SoundPreview` (Robolectric, `ShadowMediaPlayer`): depois de `play()`, `loading` é verdadeiro até o preparo; um erro deixa `isFailed(uri)` verdadeiro e `previewing` nulo; `toggle()` no mesmo uri que está tocando para a prévia. Teste de UI: com a prévia tocando, a tecla da linha passa a ter a descrição `cd_stop_preview` e um toque nela apaga o rótulo "PRÉVIA" do visor. `wakingUpToARadioStation` e `choosingOneOfTheAppsSoundsAndTurningTheRampOff` continuam passando.
- Status: CONFIRMADO — SoundPreview só chama stop() no erro (:67) e não tem loading/failed; tecla sempre play; viável sem custo de performance

### UX1-05: No celular pequeno, a faixa PRÓXIMO e os avisos ficam presos no topo e sobram dois cartões de alarme
- Gravidade: média
- Evidência: `…/ui/list/AlarmListScreen.kt:166-186` (`ListHeader`, `NextStrip` e `Warnings` ficam numa `Column` fixa acima do `LazyColumn`); `:126` (`BasePlateHeight = 136.dp` esmaecendo o pé). Prints `pequeno-02-lista.png`, `pequeno-03-lista-desligou-recorrente.png` e `pequeno-04-lista-pulando-um-dia.png`: o primeiro cartão começa em 218 dp e o botão NOVO em 520 dp, então sobram uns 300 dp para a lista, onde cabem dois cartões de 100 dp. Com o aviso de volume baixo junto (mais 62 dp), cabe um e meio.
- Como reproduzir: celular de 360x640 dp com quatro alarmes e uma permissão faltando. Só dois cartões aparecem inteiros, o terceiro some no esmaecido, e rolar não libera espaço porque o topo não se move.
- O que mudar:
  1. `ListHeader` (título e botão de estatísticas) continua fixo.
  2. `NextStrip` e `Warnings` passam a rolar com a lista: `AlarmList` ganha o parâmetro `top: @Composable () -> Unit` e o `LazyColumn` recebe, antes dos alarmes, `item(key = "top", contentType = "top") { top() }`.
  3. Como `NextStrip` e `Warnings` já têm o próprio padding lateral, o `contentPadding` do `LazyColumn` passa a `PaddingValues(top = 0.dp, bottom = 140.dp)` e cada `AlarmCard` recebe `Modifier.padding(horizontal = 16.dp).animateItem()`. O `top()` termina com `Spacer(Modifier.height(4.dp))` para manter os 18 dp entre os avisos e o primeiro cartão (4 + 14 do `spacedBy`).
  4. A tela vazia não muda: ali `NextStrip` e `Warnings` continuam chamados direto na `Column`.
- Critério de pronto: teste novo com `@Config(qualifiers = "w360dp-h640dp-xhdpi")` e quatro alarmes: depois de `performScrollToIndex` no último item, o texto do PRÓXIMO não está mais na tela e o último cartão está. `missingPermissionsShowUnderTheNextDisplay`, `aLowAlarmVolumeIsFlaggedAndTurnedUp` e `switchingARecurringAlarmOffOffersToSkipJustOneDay` continuam passando. Print novo `pequeno-02b-lista-rolada` mostra três cartões inteiros.
- Status: CONFIRMADO — print pequeno-02 mostra só dois cartões e meio sob PRÓXIMO e aviso fixos; ListHeader/NextStrip/Warnings na Column fixa (:166-186); viável

### UX1-06: No celular pequeno, o editor esconde SOM e MISSÃO, e a missão (a razão do app) é a última linha
- Gravidade: média
- Evidência: `…/ui/edit/EditAlarmScreen.kt:202-244` (ordem das linhas da placa: NOME, SOM, MISSÃO); `:249-288` (barra de baixo fixa com tecla de 76 dp, legenda e 32 dp de margem: cerca de 140 dp). Prints `pequeno-07-novo-alarme.png` e `pequeno-08-editando-excluir-armado.png`: só a linha NOME aparece; SOM some no esmaecido e MISSÃO nem começa.
- Como reproduzir: celular de 360x640 dp, tocar em "+". Sem rolar, a tela mostra hora, dias e o campo de nome (que é opcional). Quem salva direto nunca vê que pode escolher a missão.
- O que mudar:
  1. Inverter a ordem das linhas da placa para MISSÃO, SOM, NOME, mantendo os `PlateDivider` entre elas. A linha NOME continua com `minHeight = 56.dp`.
  2. Em telas baixas (`LocalConfiguration.current.screenHeightDp < 700`): tecla SALVAR com 64 dp (o `CheckGlyph` acompanha), tecla de excluir com 52 dp (o `CrossGlyph` acompanha) e `bottom = 16.dp` na barra. Nas demais telas, tudo como hoje.
- Critério de pronto: print `pequeno-07` refeito mostra a linha MISSÃO inteira acima da barra de baixo; `pequeno-08` (com dias marcados) mostra MISSÃO e SOM. Prints `pt-07` e `pt-08` mudam só a ordem das linhas. `AlarmLabelUiTest`, `creatingAnAlarmFromScratch`, `editingAnAlarm` e `deletingTakesTwoTaps` continuam passando.
- Status: CONFIRMADO — ordem NOME, SOM, MISSÃO conferida no código e no print fonte-grande-07 (MISSÃO fica abaixo); viável

### UX1-07: Com fonte grande, o rótulo MISSÃO sai cortado no editor ("MISSÃC")
- Gravidade: média
- Evidência: `…/ui/edit/EditAlarmScreen.kt:335` (`val labelWidth = with(LocalDensity.current) { 64.sp.toDp() }`). No Android 14 ou mais novo a escala de fonte não é linear: 12 sp cresce 30% com fonte 1.3x, mas 64 sp quase não cresce, então a coluna fica com os mesmos 64 dp e o rótulo de 12 sp não cabe. Prints `fonte-grande-07-novo-alarme.png` e `fonte-grande-08-editando-excluir-armado.png` (lê-se "MISSÃC"). Em inglês "MISSION" já ocupa 59 dos 64 dp em fonte normal (`en-08-editando-excluir-armado.png`), então corta ainda mais.
- Como reproduzir: Configurações do Android > Tamanho da fonte em 1.3x (Android 14+), abrir o editor de alarme.
- O que mudar: na linha 335, medir pela fonte do próprio rótulo: `val labelWidth = with(LocalDensity.current) { WwuType.label.fontSize.toDp() } * 5.4f` (dá 64,8 dp em fonte normal e 84 dp em 1.3x). Corrigir o comentário da linha 334. Não mexer em `nonScaling()` nem no `26.dp.toSp()` do `Neighbour`: são conversões de dp para sp, que continuam certas.
- Critério de pronto: prints `fonte-grande-07` e `fonte-grande-08` refeitos mostram "MISSÃO" inteiro. Teste novo com `@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi", fontScale = 1.3f)`: o nó de texto "MISSION" não tem `hasVisualOverflow` (via `SemanticsActions.GetTextLayoutResult`).
- Status: CONFIRMADO — print fonte-grande-07 mostra 'MISSÃC' cortado; labelWidth = 64.sp.toDp() em EditAlarmScreen.kt:335; correção simples

### UX1-08: A permissão de bateria, que é só recomendada, tem a mesma tecla laranja da obrigatória e abre uma lista sem dizer o que fazer nela
- Gravidade: média
- Evidência: `…/ui/permissions/PermissionsScreen.kt:192-197` (toda placa pendente usa `OrangeKey`); `…/ui/permissions/AlarmPermissions.kt:86-88` (a bateria abre `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, a lista geral de apps do sistema); `res/values-pt/strings.xml:174` (a descrição não diz o que fazer na lista). Prints `pt-05-permissoes.png` e `en-06-permissoes-aviso.png`: duas teclas laranja iguais, uma embaixo da outra; a única diferença é o rótulo pequeno "RECOMENDADO".
- Como reproduzir: na tela de permissões, tocar em PERMITIR na placa "Sem limite de bateria". Abre uma lista do Android com todos os apps; o usuário precisa descobrir sozinho que tem de achar o Wake Wake Up e mudar a opção. Ao voltar sem fazer nada, a placa continua igual e nada explica.
- O que mudar:
  1. Em `PermissionPlate`, quando `recommended && !granted`, usar `GraphiteKey(stringResource(R.string.allow), onClick = onAllow, contentDescription = stringResource(R.string.cd_allow, title))` no lugar da `OrangeKey`. Assim o laranja fica só no que é obrigatório.
  2. Trocar `perm_battery_desc`: PT "Impede o sistema de fechar o app à noite. Na lista que abrir, procure Wake Wake Up e desligue a otimização de bateria." EN "Keeps the system from closing the app at night. In the list that opens, find Wake Wake Up and turn battery optimization off."
  3. O destino do botão não muda (a escolha da lista geral é deliberada, ver o comentário em `AlarmPermissions.kt:86`).
- Critério de pronto: print 05 refeito mostra a tecla da bateria em grafite e a obrigatória em laranja. `batteryIsOnlyRecommended` e `theOtherPermissionsOpenTheirSettingsPages` continuam passando. `StringsTest` passa. No print `fonte-grande-05` a placa da bateria não corta texto.
- Status: CONFIRMADO — OrangeKey para toda placa pendente (PermissionsScreen.kt:196) e perm_battery_desc sem instrução (strings.xml:174) conferidos; viável

### UX1-09: "PRÓXIMO · EM 8 H 59 MIN" não diz a que horas nem qual alarme
- Gravidade: média
- Evidência: `…/ui/list/AlarmListScreen.kt:257-267` (`nextRingText` devolve só a contagem quando falta menos de um dia); `:281-283`. Print `pt-02-lista.png`: o próximo a tocar é o "Remédio" das 22:00, o quarto cartão; quem lê "EM 8 H 59 MIN" e olha o primeiro cartão (06:30) precisa fazer conta para descobrir que não é ele.
- Como reproduzir: ter um alarme de manhã no topo da lista e outro à noite, e abrir o app à tarde. A faixa mostra uma contagem que não bate com o primeiro cartão, e nada liga a contagem ao cartão certo.
- O que mudar: em `nextRingText`, no ramo de menos de 24 h, devolver a hora seguida da contagem: `"${formatClock(at.hour, at.minute)} · ${nextAlarmInText(now, trigger)}"`, calculando `at` como já é feito no ramo de mais de um dia. Resultado: "22:00 · EM 8 H 59 MIN". O pior caso ("22:00 · EM 23 H 59 MIN") mede cerca de 176 dp e cabe na faixa de 320 dp do celular de 360 dp, mesmo com o rótulo PRÓXIMO em fonte 1.3x; o `maxLines = 1` fica. Nenhuma string nova.
- Critério de pronto: `theNextDisplayCountsDownToTheNearestAlarm` atualizado confere também a hora do alarme no texto da faixa; `aRingADayOrMoreAwayShowsItsDayAndTime` e `aRingAWeekOrMoreAwaySaysItIsTheNextOne` continuam passando. Prints `pt-02`, `pequeno-02` e `fonte-grande-02` refeitos mostram o texto inteiro, em uma linha, dentro do visor.
- Status: CONFIRMADO — nextRingText (AlarmListScreen.kt:257-260) devolve só a contagem abaixo de 24 h; print pequeno-02 mostra 'EM 8 H 59 MIN' sem hora; viável, sem string nova

### UX1-10: O aviso "O alarme pode não tocar" é a mensagem mais grave da lista e a mais discreta, e não diz o que fazer
- Gravidade: baixa
- Evidência: `…/ui/list/AlarmListScreen.kt:343-366` (uma linha em `WwuType.bodyXS`, 13 sp, na mesma placa dos cartões); `res/values-pt/strings.xml:30-34` ("O alarme pode não tocar · falta 1 permissão": problema e causa, sem a saída; a única pista de que é tocável é a seta). Prints `pt-02-lista.png` e `fonte-grande-02-lista.png`: logo acima, o PRÓXIMO brilha em âmbar prometendo que o alarme toca.
- Como reproduzir: faltar uma permissão obrigatória e abrir a lista.
- O que mudar:
  1. `WarningRow` ganha `detail: String? = null`. O texto vira uma `Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp))` com o título em `WwuType.bodyStrong` (`Ink`) e, se houver, o detalhe em `WwuType.bodyXS` (`InkMuted`).
  2. Strings: nova `perm_missing_title` = "O alarme pode não tocar" / "Your alarm may not ring". O plural `perm_missing` passa a ser só o detalhe: PT one "Falta %d permissão. Toque para liberar.", many e other "Faltam %d permissões. Toque para liberar."; EN one "%d permission missing. Tap to allow.", other "%d permissions missing. Tap to allow."
  3. O aviso de volume continua com uma linha só, agora em `bodyStrong`.
- Critério de pronto: `missingPermissionsShowUnderTheNextDisplay` e `theWarningOpensThePermissionsAndOneTapComesBack` atualizados e passando. `StringsTest` passa. Prints `pt-02` e `pequeno-02` refeitos mostram o aviso em duas linhas, sem corte.
- Status: CONFIRMADO — print pequeno-02 mostra o aviso em uma linha discreta sem saída; strings perm_missing conferidas; viável

### UX1-11: "TOQUE DE NOVO" embaixo do botão de excluir se lê como "tocar de novo" num despertador
- Gravidade: baixa
- Evidência: `res/values-pt/strings.xml:62` (`delete_confirm` = "Toque de novo"); `…/ui/edit/EditAlarmScreen.kt:259`. Prints `pt-08-editando-excluir-armado.png` e `pequeno-08-editando-excluir-armado.png`: um botão com "X" e a legenda "TOQUE DE NOVO" em coral. Num app em que "toque" é o som do alarme, a frase não diz que a ação é excluir. Em inglês ("TAP AGAIN", `en-08`) também não diz o que o segundo toque faz. A missão já resolve isso com "Toque de novo para trocar" (`strings.xml:122`).
- Como reproduzir: abrir um alarme existente e tocar uma vez no "X".
- O que mudar: só o texto de `delete_confirm`: PT "Excluir mesmo?", EN "Really delete?". O `reserveLabel` já reserva o espaço da legenda mais longa, então a tecla não se mexe.
- Critério de pronto: `deletingTakesTwoTaps` continua passando (usa o id da string). Prints 08 refeitos nas quatro variantes mostram a legenda nova em uma linha.
- Status: CONFIRMADO — delete_confirm = 'Toque de novo' / 'Tap again' (strings.xml:62 pt, :61 en); troca de texto trivial e coerente com o vocabulário do app

### UX1-12: Depois de escolher uma rádio, o caminho de volta são duas setas, e a busca não tem como limpar
- Gravidade: baixa
- Evidência: `…/ui/sound/RadioScreen.kt:104-108` (escolher só acende a lâmpada da linha e toca a prévia); `:110-118` (a única saída é "‹", que volta para Som; de lá, outro "‹" até o editor); `:120-142` (campo de busca sem tecla de limpar: para voltar às "Mais ouvidas" é preciso apagar letra por letra). Print `pt-12-radio.png`. A tela de Missão, no mesmo nível, termina com uma tecla PRONTO (`pt-09-tarefa-matematica.png`).
- Como reproduzir: Editar alarme > Som > Rádio, buscar "jovem", tocar numa estação. Nada além da lâmpada confirma a escolha. Para salvar o alarme: "‹", "‹", SALVAR.
- O que mudar:
  1. `RadioScreen` ganha `onDone: () -> Unit`; no `WwuNavGraph`, `onDone = { entry.ifResumed { navController.popBackStack(Destinations.EDIT, inclusive = false) } }`.
  2. `var pickedHere by rememberSaveable { mutableStateOf(false) }`, ligado em `select()`. Quando `pickedHere`, depois da lista (fora do `LazyColumn`, preso no pé), mostrar uma placa: `Row` com `padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)`, `fillMaxWidth()`, `faceplate(WwuShape.faceplateSmall, softShadow = false)`, `padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)`, contendo `Lamp(Vfd)`, `Text(draft.soundName.orEmpty(), style = WwuType.bodyXS, color = Ink, maxLines = 1, overflow = Ellipsis, modifier = Modifier.weight(1f))` e `OrangeKey(stringResource(R.string.ready)) { player.stop(); onDone() }`, com 12 dp entre os elementos.
  3. No campo de busca: trocar o `Box` por uma `Row` com o campo em `weight(1f)` e, quando `query.isNotEmpty()`, uma área de 48 dp à direita com `CrossGlyph(48.dp, InkMuted)`, `clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_clear_search)) { query = "" }` e a mesma string como `contentDescription`. Com a tecla presente, o padding direito do visor cai para 0.
  4. String nova: `cd_clear_search` = "Limpar busca" / "Clear search".
- Critério de pronto: teste novo em `SoundAndTaskUiTest`: escolher uma estação mostra a tecla PRONTO, e um toque nela leva direto ao editor com o nome da estação na linha SOM. Segundo teste: com texto na busca, tocar em "Limpar busca" volta a mostrar o título "MAIS OUVIDAS". `wakingUpToARadioStation` e `radioWithoutInternetCanRetryAndSearch` continuam passando.
- Status: CONFIRMADO — RadioScreen sem onDone nem limpar busca conferido no código (select/query); mudança viável e dentro do orçamento

### UX1-13: O editor diz "toca uma vez", mas não diz em que dia
- Gravidade: baixa
- Evidência: `res/values-pt/strings.xml:56` (`days_none_hint` = "Nenhum dia marcado: toca uma vez."); `…/ui/edit/EditAlarmScreen.kt:186-193`. Print `pt-07-novo-alarme.png`. A lista já responde a pergunta ("UMA VEZ · QUINTA", `pt-02-lista.png`), mas só depois de salvar.
- Como reproduzir: às 13:00, criar um alarme para as 07:00 sem marcar dias. O editor não diz se toca hoje ou amanhã; só o cartão na lista, depois de salvar, mostra o dia.
- O que mudar:
  1. `days_none_hint` passa a ter dois argumentos: PT "Nenhum dia marcado: toca uma vez, %1$s às %2$s." EN "No day selected: rings once, %1$s at %2$s."
  2. No editor, calcular o dia como o `OnceLine` da lista faz: `val from = remember(draft.hour, draft.minute) { LocalDateTime.now() }`; `val day = remember(draft.hour, draft.minute) { LocalDate.ofEpochDay(nextTriggerEpochDay(draft.copy(enabled = true), from)) }`; passar `weekdayName(day, from.toLocalDate())` e `formatClock(draft.hour, draft.minute)` à string. O bloco continua dentro do `if (draft.days.isEmpty())`.
- Critério de pronto: teste novo: num alarme novo sem dias, o texto embaixo das teclas de dia traz o nome do dia e a hora, e muda quando a hora passa de uma que já foi hoje para uma que ainda vem. `StringsTest` passa. Print 07 refeito nas quatro variantes sem corte.
- Status: CONFIRMADO — days_none_hint sem dia/hora (strings.xml:56 pt, :55 en); print pt-07 mostra o texto genérico; nextTriggerEpochDay já usado na lista; viável
