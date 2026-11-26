# Revisão visual (Fase 6): antes x depois

HEAD revisado: `e737427` (base antes dos lotes: `006d028`). Nenhum código foi alterado nesta revisão.
Critérios: `GUIA-VISUAL.md` e a skill impeccable (`craft-floor.md`, `critique-rubric.md`, `android.md`).

## Como foi feito

- App: 88 prints de `prints/depois/app/` contra os 84 de `prints/antes/app/`. Comparação por pixel primeiro: `10-tarefa-frase` e `12-radio` são idênticos nas 4 variantes, e `en-19-estatisticas` também. O resto foi aberto lado a lado (antes | depois).
  - `pequeno`: todas as telas, lado a lado.
  - `pt`: todas as telas do depois; o antes foi conferido pelas variantes `pequeno`, `en` e `fonte-grande` da mesma tela.
  - `en` e `fonte-grande`: 03, 05, 06, 07, 07b, 08, 11, 13, 14 (só fonte-grande), 15, 18, 19, 21.
- Site: início PT e EN (celular e desktop, página inteira) e privacidade PT (celular e desktop). Privacidade EN e 404 não foram abertos (o 404 é idêntico ao antes, byte a byte).
- Contraste do Bom dia medido nos pixels do print (cor do texto contra a cor do degradê na mesma linha).
- Limite: os prints do app são do Robolectric (ScreenshotTour), sem barra de status, sem teclado do sistema e sem animação. O "antes" do site só mostra o topo e o fim da página (as seções entravam por animação e saíram pretas no print), então no site a comparação vale para o topo e o fim; o meio foi avaliado só no depois.

## O que melhorou

App
- **Confirmar pulo (21)**: no celular pequeno o teclado ficava cortado atrás de MANTER/DESATIVAR; agora cabe inteiro. Um laranja só (OK), título "Pular só desta vez?", dica "Digite o resultado e toque em OK".
- **Permissões (05, 06)**: saiu o sobretítulo "ANTES DE COMEÇAR"; só a permissão obrigatória fica laranja (a de bateria virou grafite); vindo da lista a tela tem seta e "VOLTAR PARA A LISTA"; a descrição da bateria diz o que fazer na lista do sistema; a última placa some em degradê em vez de parecer cortada.
- **Novo/Editar alarme (07, 08)**: "MISSÃC" cortado na fonte grande foi corrigido; a ordem virou Missão, Som, Nome (o opcional por último); a dica diz quando toca ("toca uma vez, sexta às 07:00"); "EXCLUIR MESMO?" no lugar de "TOQUE DE NOVO"; diálogo novo "Digite a hora" (07b) com visor, uma tecla laranja e alvos de 48 dp.
- **Lista (01 a 04, 20)**: PRÓXIMO mostra a hora ("22:00 · EM 6 H 22 MIN"); o aviso de permissão virou título + linha de ação e não quebra mais no meio na fonte grande.
- **Som (11)**: grupos com serigrafia ("SONS DO APP", "SONS DO CELULAR"), "Aumentar aos poucos" subiu para o topo, e a tela rola até o som marcado.
- **Toque e Espere (13, 14)**: rótulos maiores; "JÁ ACORDEI, COMEÇAR MISSÃO" ganhou etiqueta escura (antes ficava direto no degradê); "TOQUE PARA ADIAR" no lugar de "SEM LIMITE".
- **Missão (15 a 17)**: tempo em "TEMPO 1:14" (antes "MISSÃO · 74 S"), luzes de contas resolvidas no visor, dica em 14 sp, tecla "−" removida e 0 mais largo.
- **Bom dia (18)**: linha de comparação com a própria média ("Primeiro despertar registrado."), sem pontos nem elogio.

Site
- Rótulo do botão, versão e "Como instalar" agora ficam em etiquetas escuras sobre o degradê (antes: texto de 12 px e link sublinhado direto no laranja).
- O fim da página deixou de ser um degradê vazio: tem os passos de instalação num painel escuro e o cartão de teste.
- As seções não dependem mais de animação de entrada para aparecer.
- Privacidade: data atualizada e item novo (cópia mínima dos horários); layout igual.

## O que piorou (regressões)

| ID | Gravidade | Onde aparece | Problema |
|---|---|---|---|
| VIS-01 | alta | `depois/app/pequeno-18-bom-dia.png` | A linha nova de comparação fica direto sobre o degradê, mais embaixo (mais claro): 3,81:1 no celular pequeno. O guia pede 4,5:1 e etiqueta escura para texto pequeno sobre degradê. |
| VIS-02 | média | `depois/app/pequeno-19-estatisticas.png` | O print saiu com a tela ainda sem dados: só traços, sem a tabela e sem a frase de vazio. A variante pequena das Estatísticas não foi validada. |
| VIS-03 | média | `depois/app/en-21`, `fonte-grande-21`, `pt-21-confirmar-desativar.png` | Em tela alta o teclado e o MANTER ficam soltos no meio, com cerca de 25% da tela vazia embaixo. Antes as teclas ficavam no pé. |
| VIS-05 | baixa | `depois/app/pequeno-11-som.png`, `en-11-som.png`, `fonte-grande-11-som.png` | Depois da rolagem automática, a linha de cima fica fatiada pelo visor fixo ("Campainha mecânica" cortada ao meio; no EN a placa "Increase gradually" sem a borda de cima). |

## O que ainda está ruim (já existia, vale corrigir agora)

| ID | Gravidade | Onde aparece | Problema |
|---|---|---|---|
| VIS-04 | média | `depois/app/*-03-lista-desligou-recorrente.png`, `*-20-lista-oferta-pular.png` | Dois laranjas na mesma tela: PULAR e NOVO. O guia diz que é erro, e era o critério de pronto do lote da lista. |
| VIS-06 | baixa | `depois/app/pequeno-06-permissoes-aviso.png`, `*-05-permissoes.png` | Vindo da lista, o rodapé reserva espaço para uma nota vazia: sobram uns 72 dp embaixo do botão enquanto a 4ª placa é cortada. A nota "Sem esses itens…" está em 12 sp (o guia pede 13 sp para texto de decisão). |
| VIS-07 | média | `depois/site/desktop-inicio-pt.png`, `celular-inicio-pt.png`, EN, `desktop-privacidade-pt.png` | Sobretítulo em cima de título em 4 seções do início e na privacidade. Está na lista de proibidos do guia (seção 9). |
| VIS-08 | baixa | `depois/site/desktop-inicio-pt.png` e EN (passos 01 e 02, bloco "Acordou antes do alarme?") | As demonstrações não acompanham o app: teclado com "−", Espere com "SEM LIMITE", texto com "desativar esse alarme só desta vez". |

## Detalhe e correção

### VIS-01 (alta): texto do Bom dia abaixo de 4,5:1 no celular pequeno
- Print: `depois/app/pequeno-18-bom-dia.png`. Medido: "para acordar de verdade." 4,36:1 (já era assim no antes); "Primeiro despertar registrado." 3,81:1 (novo). Nas telas altas a mesma linha mede 5,4 a 5,9:1, porque o degradê é por fração da altura e no celular curto o texto cai numa faixa mais clara.
- Piora com os textos longos, que não aparecem no print: "12 min mais rápido que a sua média da semana." quebra em duas linhas em 360 dp e a segunda cai mais embaixo ainda.
- Arquivo: `app/src/main/java/com/wakewakeup/ui/goodmorning/GoodMorningScreen.kt` (linhas 127 a 141); cores em `ui/theme/Gradients.kt` (`GoodMorningStops`).
- Correção: pôr `to_really_wake` e a linha de comparação numa etiqueta escura, como o rótulo do Começar missão: `Modifier.background(EspereLabelPlate, WwuShape.field).padding(horizontal = 12.dp, vertical = 6.dp)`. Não mexer no degradê. Refazer o print `pequeno-18` e conferir 4,5:1.

### VIS-02 (média): Estatísticas no celular pequeno fotografada sem dados
- Print: `depois/app/pequeno-19-estatisticas.png` (85 KB contra 197 KB do antes). As outras três variantes mostram os dados.
- Causa provável: o tour espera só o texto "MINUTOS ATÉ ACORDAR", que já existe com `loaded = false`; a foto saiu antes do primeiro valor do banco. A tela desenha o esqueleto com traços enquanto carrega.
- Arquivos: `app/src/test/java/com/wakewakeup/ui/ScreenshotTour.kt` (teste `stats()`, linha 290); `app/src/main/java/com/wakewakeup/ui/stats/StatsScreen.kt` (linha 87 em diante).
- Correção: no tour, esperar um texto que só existe com dados (por exemplo `compose.waitForText("06:42")`) antes do `shot`. Na tela, só desenhar o conteúdo do `Column` rolável quando `state.loaded` for verdadeiro, para o aparelho real não piscar a semana vazia. Refazer o print e comparar com `antes/app/pequeno-19-estatisticas.png`.

### VIS-03 (média): Confirmar pulo solto no meio em tela alta
- Prints: `depois/app/en-21-confirmar-desativar.png`, `fonte-grande-21-…`, `pt-21-…`. No `pequeno-21` está certo (ocupa a tela).
- Arquivo: `app/src/main/java/com/wakewakeup/ui/skip/SkipConfirmScreen.kt` (linhas 96 a 130).
- Correção: envolver o `Column` rolável em `BoxWithConstraints` e dar a ele `Modifier.heightIn(min = maxHeight)` com `verticalArrangement = Arrangement.SpaceBetween`, em dois grupos: em cima os cartões (`SkipRowCard`), embaixo `Gate` + `Spacer(24.dp)` + MANTER. Em tela alta o teclado encosta no pé, como na Missão; em tela pequena continua rolando como hoje.

### VIS-04 (média): dois laranjas na lista
- Prints: `depois/app/pt-03`, `en-03`, `fonte-grande-03`, `pequeno-20`, `pt-20`.
- Arquivo: `app/src/main/java/com/wakewakeup/ui/list/AlarmListScreen.kt` linha 564 (`OrangeKey(skip_once)`); a linha 351 (`OrangeKey(alarm_volume_up)`) tem o mesmo problema quando o aviso de volume aparece.
- Correção: trocar as duas por `GraphiteKey`, igual ao DESFAZER da mesma gaveta. NOVO fica como o único laranja.

### VIS-05 (baixa): linha fatiada debaixo do visor do Som
- Prints: `depois/app/pequeno-11-som.png`, `en-11-som.png`, `fonte-grande-11-som.png` (o `pt-11` não rola e está certo).
- Arquivo: `app/src/main/java/com/wakewakeup/ui/sound/SoundPickerScreen.kt` linha 167; `ui/components/Hardware.kt` linha 210 (`bottomFade`).
- Correção: criar um `topFade` espelhado do `bottomFade` (mesmos 24 dp, cor `Housing`) em `Hardware.kt` e aplicar no `Column` rolável do Som, para a lista sumir em degradê sob o visor em vez de ser cortada seca.

### VIS-06 (baixa): rodapé das Permissões
- Prints: `depois/app/pequeno-06-permissoes-aviso.png` (espaço vazio), `pt-05`, `pequeno-05` (nota pequena).
- Arquivo: `app/src/main/java/com/wakewakeup/ui/permissions/PermissionsScreen.kt` linhas 173 a 183.
- Correção: só compor o `Text` da nota quando o texto não for vazio (tirar o `heightIn(min = 34.dp)` fixo) e trocar `WwuType.caption` por `WwuType.bodyXS`.

### VIS-07 (média): sobretítulos no site
- Prints: `depois/site/desktop-inicio-pt.png`, `desktop-inicio-en.png`, `celular-inicio-pt.png`, `celular-inicio-en.png`, `desktop-privacidade-pt.png`, `celular-privacidade-pt.png`.
- Arquivos: `site/index.html` linhas 119, 135, 227, 252; `site/en/index.html` linhas 119, 136, 228, 253; `site/privacidade.html` linha 48; `site/en/privacy.html` linha 48; espaçamento em `site/styles.css`.
- Correção: apagar esses `<div class="label">` ("Em 28 segundos", "Como funciona", "Sons e rádio", "Estatísticas", "Wake Wake Up") nas duas línguas e conferir o espaço acima dos títulos. O dado "28 segundos" já está no botão ASSISTIR · 0:28. Não apagar os `label` que são serigrafia de painel (Volume, Hoje, Como instalar, "Com rádio, o alarme de verdade assume se"). O "Despertador para Android" do topo fica acima do relógio, não do título: pode ficar.

### VIS-08 (baixa): demonstrações do site diferentes do app
- Prints: `depois/site/desktop-inicio-pt.png`, `desktop-inicio-en.png`, `celular-inicio-pt.png`, `celular-inicio-en.png`; comparar com `depois/app/pt-13-tocando.png`, `pt-15-missao.png`, `pt-21-confirmar-desativar.png`.
- Arquivos: `site/index.html` linhas 144, 180, 210; `site/en/index.html` linhas 145, 181, 211; `site/main.js` linhas 18, 29, 330, 347; `site/privacidade.html` linha 76; `site/en/privacy.html` linha 77; `site/styles.css` linhas 274 a 281.
- Correção: tirar a tecla `data-key="M"` e deixar o 0 ocupar três colunas; trocar `noLimit` para "TOQUE PARA ADIAR" / "TAP TO SNOOZE"; trocar "desativar esse alarme só desta vez" por "pular esse alarme só desta vez" e "turn that alarm off just this once" por "skip that alarm just this once". Só publicar junto com o APK novo (o guia diz que a página não promete o que o APK publicado não faz).

## Observações que ficaram fora do retorno (gosto ou redesenho)

- **Missão em tela alta (15, 16)**: o visor desceu para junto do teclado e o vazio (uns 27% da tela) passou para cima, entre a barra de tempo e o visor. Não é pior que antes, só mudou de lugar.
- **Espere na fonte grande (14)**: "JÁ ACORDEI, COMEÇAR MISSÃO" quebra em duas linhas e a etiqueta fica colada no botão (uns 5 dp). Nada cortado.
- **Placa "RECOMENDADO" nas Permissões**: é um rótulo em cima de um título dentro da placa. Funciona como estado, não como enfeite; deixei.
- **Botão de play do vídeo no site**: fica em cima do relógio do pôster e a etiqueta "ASSISTIR · 0:28" cai sobre o medidor de volume. Mover exige decidir onde o pôster tem área livre.
- **Três passos no desktop**: os cartões 01 e 03 têm um vazio grande entre o texto e a demonstração (as demonstrações alinham por baixo). Resolver pede redesenho do bloco.
- **Vídeo em português na página EN**: está avisado no texto. O pôster ainda mostra "SEM LIMITE" (precisa regravar).
- **Cara de IA**: no app não achei degradê genérico, vidro, emoji nem cartões idênticos. No site sobram os sobretítulos (VIS-07) e o trio de cartões numerados, que aqui se justifica porque são passos em ordem e cada um tem uma demonstração de verdade.
