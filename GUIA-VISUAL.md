# Guia visual: Wake Wake Up

Regra única para quem mexer em tela, texto ou arte. Registra a direção que já existe no código (`ui/theme/`, `ui/components/`) e fecha o que estava solto. Em conflito entre uma skill de design e este guia, vale o guia. Não muda: o nome, o propósito (só desliga com a missão) e a identidade de rádio-relógio dos anos 70.

## 1. A ideia
Cada tela é o painel frontal de um rádio-relógio de cabeceira: carcaça fosca escura, placas em relevo, visores rebaixados com números âmbar, teclas com base sólida que afundam, LEDs. Tema escuro fixo (o app é usado no quarto, de madrugada). Nada na tela é "cartão de app": é placa, visor, tecla ou serigrafia.

## 2. Cor (tokens de `ui/theme/Color.kt`; nunca hex solto numa tela)
| Papel | Tokens | Uso |
|---|---|---|
| Carcaça | `Housing`, `HousingDeep` | fundo de toda tela; `HousingDeep` em costuras e sombras |
| Placa | `PlateTop` → `PlateBottom` | superfície em relevo que agrupa controles (`Modifier.faceplate`) |
| Visor | `Visor`, `Drawer`, `SwitchTrack` | rebaixos: onde se lê hora, conta, próximo toque |
| Serigrafia | `Ink`, `InkSoft`, `InkMuted`, `InkOff` | texto. `InkMuted` é o mínimo para texto que precisa ser lido (7,3:1); `InkOff` só para estado desligado |
| Aceso | `Vfd`, `VfdBright`, `LedOff` | informação viva: dígitos do visor, LED, item selecionado. Nunca é botão |
| Ação | `Action`, `ActionLight`, `ActionDark`, `ActionBase`, `OnAction` | o "laranja Braun": a ação principal |
| Secundária | `GraphiteLight`/`GraphiteDark`, `KeyFaceTop`/`KeyFaceBottom` | teclas grafite |
| Teclado | `CalcDigit*`, `FnKey*` | dígitos escuros, C e apagar em creme |
| Perigo e erro | `Danger*` (excluir), `ErrorDisplay` (erro dentro de visor), `ErrorText` (erro em texto) | |

- **Um laranja por tela.** `Action` marca a única ação principal (Começar missão, Salvar, Novo, OK, permissão obrigatória). Todo o resto é grafite. Duas teclas laranja na mesma tela é erro.
- Âmbar (`Vfd`) informa, laranja (`Action`) age, coral (`ErrorText`/`ErrorDisplay`) avisa. Não trocar os papéis.
- Degradê só existe como nascer do sol (`ringSunrise`, `espereSunrise`, `goodMorningSunrise` em `Gradients.kt`): toque, Espere e Bom dia. Em nenhum outro lugar.
- Contraste mínimo 4,5:1 para qualquer texto. Texto pequeno nunca fica direto sobre o degradê: vai numa etiqueta escura (`EspereLabelPlate`).

## 3. Tipografia (`ui/theme/Type.kt`)
- **Barlow Condensed** (500 em `WwuType.vfd`, 600 em `WwuType.flip`): só números e leituras de visor. **Instrument Sans** (400/500/600): todo o resto. Nenhuma terceira família.
- Escala: `display` 40, `titleL` 28, `titleM` 26, `titleS` 20, `bodyStrong` 15/600, `body` 15, `bodyInput` 16, `bodyS` 14, `bodyXS` 13, `caption` 12, `keyLabel` 14/600, `label` 12/600 em caixa alta com +12% (a serigrafia).
- **Mínimos.** Nas telas de toque, missão e confirmar pulo, texto de ação, instrução ou erro tem 14 sp ou mais (`bodyS`; rótulo de ação principal com 15 a 16 sp/600). Nas outras telas, texto que a pessoa precisa ler para decidir tem 13 sp ou mais. `caption` e `label` (12 sp) ficam para serigrafia: nomes de seção, unidades, legendas que repetem o que outro elemento já diz.
- Tudo em `sp`. Exceção: dígitos presos ao tamanho de uma peça (flip, visor de altura fixa) usam `nonScaling()`.
- Toda tela é conferida em quatro prints: `pt`, `en`, `pequeno` (360x640 dp) e `fonte-grande` (1.3x). Texto cortado ou quebrado no meio da palavra reprova.

## 4. Toque e espaço
- Alvo mínimo **48 dp**, com 8 dp entre alvos. Nas telas de toque, missão e confirmação: **56 dp** (teclado numérico de 52 a 76 dp só quando a altura não deixa; Começar missão com 96 dp).
- Grade de 4 dp. Margem lateral: 16 dp para placas, 20 dp para texto solto e visores. Dentro da placa: 14 a 20 dp. Entre placas: 14 dp. Entre grupos: 24 dp. Mais espaço acima de um título do que abaixo.
- Cantos: só os de `WwuShape` (placa 12/14/18, visor 8/10/12/14, tecla 5/6, pílula 28).
- A ação principal fica embaixo, ao alcance do polegar. O que se lê fica em cima. Conteúdo que não cabe rola; a ação principal não some atrás de barra fixa.
- Alinhar à esquerda por padrão. Centralizar só o que é o herói da tela (relógio do toque, número do Bom dia).

## 5. Componentes de hardware (`ui/components/`) e quando usar
| Componente | Use para | Não use para |
|---|---|---|
| `Modifier.faceplate` (placa) | agrupar controles ligados entre si; linhas separadas por `PlateDivider` | envolver a tela toda; placa dentro de placa |
| `Modifier.visor` + `VfdText` | o que se lê e muda: hora, conta, contagem, selecionado | texto corrido, botões |
| `FlipDigit` / `FlipNumber` | a hora e o número herói | decoração |
| `PrintLabel` | serigrafia: nome de seção, unidade | "sobretítulo" em cima de um título (proibido) |
| `RoundButton` / `LabeledRoundButton` | a ação principal redonda (Novo, Salvar, Começar missão) | mais de um laranja por tela |
| `OrangeKey` / `GraphiteKey` / `SunkKey` | ação principal em linha / secundária / estado já concluído | |
| `LatchKey` + `KeyLed`, `DayKeys` | escolhas que travam (dias, modo) | |
| `SlideSwitch` | ligar e desligar | escolher entre opções |
| `LedMeter`, `LedSegment`, `Lamp` | nível, tempo, progresso, estado pontual | enfeite |
| `EspereBar` | só o Espere | |
| `Modifier.speakerGrille` | só a faixa PRÓXIMO da lista | |
| `BackHeader` | toda tela interna (seta + título) | |
| `CalculatorKeypad` | missão e confirmar pulo | |
| Glifos (`PlusGlyph`, `CheckGlyph`, `CrossGlyph`, `PlayGlyph`, `Chevron`) | ícones: sempre desenhados | emoji ou caractere Unicode no lugar de ícone |

Componente novo de uso geral só entra em `ui/components/` pelo lote dono dessa pasta. Os outros criam o que precisarem como `private` no arquivo da própria tela, com os mesmos tokens.

## 6. Estados
Toda tela tem: vazio com orientação e a ação que resolve; carregando ("Buscando", "Conectando…"); erro que diz o problema e a saída; sucesso discreto. Tecla tem repouso, pressionada (afunda) e, se couber, desligada (`InkOff`). Nada falha em silêncio.

## 7. Movimento
- Curto e mecânico: tecla afunda em 80 ms (`PRESS_MS`), chave encaixa em 240 ms com leve ressalto (`SnapEasing`), dígito vira em 420 ms. Nada flutua, pulsa por enfeite ou entra deslizando.
- Um momento de movimento por tela (o dígito que vira, o sol que sobe). Sem a mesma entrada em todo bloco.
- `rememberReduceMotion()` (o "remover animações" do sistema) troca tudo por corte seco. Animação nova sem esse desvio reprova.
- Valor animado é lido em lambda (`graphicsLayer { }`, `offset { }`, `drawBehind`), nunca na composição. Nada recompõe sem parar: relógio 1 vez por minuto, contagem 1 vez por segundo.
- Resposta tátil: erro vibra e treme, acerto confirma (`HapticFeedbackType.Confirm`).

## 8. Texto
- PT-BR primeiro, EN junto. Curto, direto, sem marketing e sem exclamação. Frase completa onde explica, rótulo de 1 a 3 palavras onde age.
- Botão diz o que faz ("Salvar", "Começar missão", "Excluir mesmo?"). Erro diz o problema e a saída ("Essa hora não existe. Use de 00:00 a 23:59.").
- Um nome por coisa: **Espere** (a pausa; "Snooze" em inglês), **missão** (não "tarefa" nem "desafio"), **pular** (não "desativar") para o toque que não acontece só desta vez, **toque** só para o som do alarme (para o gesto, "tocar em").
- Caixa alta só na serigrafia e nos rótulos de tecla, feita no código, nunca digitada na string.
- Nada de culpa, pontos, sequência de dias, confete. O app informa e compara com a própria média; não premia.

## 9. Proibido (cara de IA)
Degradê genérico ou texto em degradê; emoji ou caractere no lugar de ícone; vidro e desfoque (glassmorphism); tudo centralizado; cartões idênticos de ícone + título + texto; sobretítulo em cima de título; barra colorida num lado só de uma caixa; sombra dura sem desfoque; brilho colorido sem motivo (brilho só em `Vfd` aceso); fonte do sistema ou terceira família; tema claro; ilustração genérica; a estética Graffiti Core.

## 10. Site (`site/`)
- Mesmo sistema: as variáveis de `styles.css` espelham os tokens (`--housing`, `--plate`, `--visor`, `--ink`, `--ink-muted`, `--vfd`, `--action`, `--on-action`), as mesmas duas famílias (`--cond`, `--sans`) e o mesmo ressalto (`--snap`). No máximo 4 arquivos woff2.
- Alvos de 48 px ou mais. Texto de 17 px ou menos nunca direto sobre o degradê de `.hero` e `.final`: usa etiqueta `--housing` ou painel `.visor`.
- Um laranja por dobra: o botão de baixar. O resto é tecla escura (`.pill`).
- Movimento só com autoria (dígitos que viram, LEDs do gráfico). Nada de entrada igual em todas as seções. `prefers-reduced-motion` respeitado.
- Tudo o que muda em PT muda em EN. A página não promete o que o APK publicado não faz. Sem pedido a terceiros, sem cookie.

## 11. Arte de divulgação (Canva)
Fundo `#141017`; nascer do sol só na base (`#6B2433` → `#E8663C` → `#F7D46B`); relógio flip real (recorte de print do app ou do site, nunca redesenhado); frase em Instrument Sans 600 creme `#F6ECE0`; linha de apoio em caixa alta 600 com espaçamento largo. Sem foto de banco de imagens, sem moldura de celular brilhante, sem emoji, sem selo de "novo". Arte do Canva não entra no app.
