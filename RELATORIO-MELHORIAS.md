# Relatório de melhorias: Wake Wake Up

Branch `melhorias-claude`, 26/11/2026. Tudo foi feito neste PC. Nada foi enviado ao GitHub, nada foi publicado no site e não houve merge no `main`.

## 1. Resumo

- **O que era:** despertador Android (Kotlin + Jetpack Compose) que só desliga com uma missão, mais o site de download em `site/`. A suíte de testes nunca tinha rodado neste PC (o caminho com "Área de Trabalho" quebrava o Gradle).
- **O que foi feito:** 5 auditorias (lógica, técnica, UI/UX em duas partes e site) acharam 64 problemas. Todos foram confirmados e nenhum foi descartado. Depois vieram 9 lotes de correção e uma revisão que achou mais 20 itens, também todos corrigidos.
- **Alarme:** agora toca com o celular reiniciado e ainda bloqueado, não fica mudo se o banco falhar, não dá para baixar o volume sem missão e relógio mudado à mão rearma os alarmes.
- **Telas:** hora digitável, teclado da missão maior, aviso quando o tempo da missão acaba, Bom dia compara com a sua média, textos mais claros ("pular" em todo lugar).
- **Site:** versão e tamanho do APK sob o botão, passos de instalação, botão de baixar fixo no celular, 4 fontes (eram 5), SEO e imagem de compartilhamento em inglês.
- **Números finais:** 385 testes, 0 falhas, 28 ignorados (só os do tour de prints). Lint com 0 erros e 57 avisos. APK release 4.132.133 B, +1,61% (limite +5%). Lighthouse do site 97/100/100/100.
- **Funções:** 111 mapeadas. 98 funcionam com teste, 13 não são testáveis na JVM (JavaScript do site). Nenhuma quebrada.
- **Depende de você:** validar no celular (principalmente o toque com o celular bloqueado), assinar o APK de release, publicar o site, escolher o e-mail, confirmar a origem dos 7 sons e criar o app na Play Console.
- **Não foi feito:** o gráfico de destaque da Play (o Canva não deixa escolher a fonte do guia) e os sons novos (ElevenLabs não estava autenticado).
- **Atenção:** `.auditoria/prints/` e `.auditoria/logs/` não vão para o git e ficam só neste PC.

## 2. O que foi melhorado

### Alarme e confiabilidade

- **Tocar com o celular reiniciado e bloqueado** (LOG-01, TEC-01): os alarmes são rearmados já no boot, a partir de uma cópia mínima guardada numa área que abre antes do desbloqueio. Antes do desbloqueio toca a campainha do app com a missão padrão. O banco não mudou de lugar, de propósito (risco de perder alarmes). Só se prova no aparelho.
- **Falha no meio do toque não deixa o alarme mudo** (LOG-03, LOG-04, TEC-02, TEC-03): se o banco falhar o alarme toca mesmo assim, e se o som escolhido falhar, outro assume. As falhas do player são tratadas.
- **Volume** (LOG-02): o alarme sobe para um volume audível enquanto toca, a tecla de baixar volume e o mudo não valem no toque e na missão, e o volume original volta no fim.
- **Ações na tela errada** (LOG-06, que era o BUG-1): "Espere" durante a missão, confirmar duas vezes e "Espere" no Bom dia deixam de contar errado.
- **Segundo alarme durante a missão do primeiro** (LOG-10) não zera mais nada. O prazo da missão passa a valer por conta (UX2-02).
- **Rearme em outras situações:** relógio mudado à mão (LOG-08, que era o BUG-2), abrir o app depois de um toque interrompido (LOG-05), alarme "poupado" no rearme (LOG-07), restauração de backup (TEC-06), editar a hora de um alarme que está pulando (LOG-09) e desfazer pulo no último minuto (LOG-11).
- **Alarme de uma vez perdido com o celular desligado** passa a aparecer desligado (LOG-13, em parte).
- **Segurança:** o pulo pela notificação só vale com um token guardado pelo app (TEC-10). O canal "tocando" desligado nas configurações passa a gerar aviso (TEC-07).
- **Partida e threads:** repositório, aviso de permissões e partida fora da thread principal (TEC-04). Sem polling de 250 ms: a espera usa broadcast (TEC-11). Gravações não são cortadas no meio (LOG-12).
- **Revisão de código** (REV-01 a REV-09): um alarme de uma vez que tocou com o celular bloqueado voltava a tocar no dia seguinte; o rearme do boot podia falhar; havia uma retomada dupla na espera; o volume original podia se perder se o processo morresse; e outros casos de borda.

### Telas do app

- **Hora:** tambores com passo e inércia próprios, e diálogo para digitar a hora com 4 dígitos (UX1-01). Dica "toca uma vez, sexta às 07:00" (UX1-13).
- **Permissões:** "Continuar" continua no primeiro toque, tem seta vindo da lista, e a bateria (só recomendada) usa tecla grafite (UX1-02, UX1-08).
- **Lista:** "PRÓXIMO" mostra a hora; faixa e avisos rolam com a lista no celular pequeno; aviso com título e saída (UX1-05, UX1-09, UX1-10).
- **Editor:** Missão e Som aparecem antes do Nome; "MISSÃO" não corta com fonte grande (UX1-06, UX1-07).
- **Som e rádio:** "Aumentar aos poucos" no topo, seções "do app" e "do celular", prévia que para, mostra "Conectando" e avisa falha, rádio com "Pronto" e tecla de limpar a busca (UX1-03, UX1-04, UX1-12).
- **Toque e missão:** aviso quando o tempo acaba, teclado maior e colado no visor, sem a tecla "−", vibração e luzes de progresso ao acertar, conteúdo centrado, erro da frase diz onde está (UX2-01, UX2-04, UX2-08, UX2-10, UX2-12, UX2-13). Rótulos do Espere maiores (UX2-05).
- **Bom dia:** compara com a média da semana e mostra segundos quando passa rápido (UX2-07).
- **Estatísticas vazia** orienta no topo (UX2-09). **Confirmar pulo** sem o segundo botão de confirmar e cabe no celular pequeno (UX2-03).
- **Revisão visual** (VIS-01 a VIS-06): texto do Bom dia sobre etiqueta escura (4,5:1), Estatísticas só desenha depois de ler o banco, Confirmar pulo no pé em tela alta, um laranja só na lista, topo do Som sem linha fatiada, nota das Permissões só com texto.
- **Sistema:** ícone em WebP, data num idioma só, versão 1.2 (code 3), interruptor e sintonizador sem recompor a cada quadro (TEC-08, TEC-09, PLAY-02, TEC-12).

### Textos

- "SEM LIMITE" virou "Toque para adiar" (UX2-06). "Pular" em todo o fluxo no lugar de "desativar" (UX2-11). "Excluir mesmo?" no lugar de "Toque de novo" (UX1-11).
- Textos de permissões, bateria e estatísticas vazias reescritos (UX1-02, UX1-08, UX2-09).
- Revisão geral de todas as strings PT e EN com a skill ux-writing.
- Ficha da Google Play em PT-BR e EN-US e respostas do formulário "Segurança dos dados" (PLAY-01, PLAY-03), em `assets/marketing/play-store/`.

### Site

- Texto claro sobre etiqueta escura no lugar de texto escuro sobre o degradê (SITE-03).
- Versão e tamanho do APK sob o botão, bloco "Como instalar" (SITE-04). Faixa "grátis, sem anúncios, sem cadastro, funciona sem internet" (SITE-05).
- Sem contradição sobre a soneca (SITE-07). Botão de baixar fixo no celular (SITE-08).
- SEO: títulos e descrições novos, dados do app no JSON-LD, imagem de compartilhamento em inglês (SITE-09). Seletor de idioma de 48 px e link de volta nas páginas internas (SITE-10).
- Visores das estatísticas sem vazar em 320 a 360 px (SITE-11). Menos cara de template (SITE-12). Pequenos dados acertados (SITE-13, em parte).
- 4 fontes em vez de 5 (TEC-08). Política de privacidade descreve a cópia mínima dos horários (PLAY-03).
- Revisão: sem sobretítulos em cima de títulos (VIS-07), demonstrações do site iguais ao app (VIS-08), vocabulário igual ao do app (REV-12), "Fechou o app?" sem prometer demais (REV-10), script do APK confere a versão anunciada (REV-11).

### Testes e ferramentas

- Gradle wrapper 9.5 adicionado e `ferramentas/gradle.sh`, que contorna o acento no caminho. Antes disso a suíte nunca tinha rodado neste PC.
- A suíte foi de 189 para 385 testes. Os testes de bug (`*BugTest`) estavam ignorados e hoje rodam e passam.
- `ferramentas/conferir-apk-site.sh` confere se o APK do site é de release, assinado, com a versão certa e abaixo de 12 MB (SITE-01).
- `ferramentas/og-image.html` gera a imagem de compartilhamento.
- Lint: o erro `NonObservableLocale` foi corrigido (0 erros).

### Play Store

- Ícone da loja 512x512 (`assets/marketing/play-store/play-icone-512.png`), ficha PT e EN, formulário de segurança dos dados e versão 1.2. O gráfico de destaque ficou pendente (seção 7).

## 3. Funções

111 funções (F01 a F111). Status: FUNCIONA (um teste automatizado prova), QUEBRADO (havia teste de bug ignorado), NÃO TESTÁVEL (não dá para provar na JVM). "Antes" é o estado de `006d028`; "depois" é o estado depois dos lotes e das correções. F95 a F111 são funções novas. O teste que prova cada uma está em `.auditoria/funcoes.md`.

| status | antes | depois |
|---|---|---|
| FUNCIONA | 79 | 98 |
| QUEBRADO | 2 | 0 |
| NÃO TESTÁVEL | 13 | 13 |
| total | 94 | 111 |

F46 (Espere durante a missão) e F94 (mudar a hora à mão) saíram de QUEBRADO para FUNCIONA. As 13 não testáveis são o JavaScript do site (F81 a F93).

| id | função | antes | depois |
|---|---|---|---|
| F01 | Criar alarme | FUNCIONA | FUNCIONA |
| F02 | Editar alarme | FUNCIONA | FUNCIONA |
| F03 | Sair do editor sem salvar (descartar) | FUNCIONA | FUNCIONA |
| F04 | Excluir alarme (dois toques) | FUNCIONA | FUNCIONA |
| F05 | Ligar e desligar alarme | FUNCIONA | FUNCIONA |
| F06 | Escolher dias da semana | FUNCIONA | FUNCIONA |
| F07 | Definir hora e minuto (roda, passos de 10 min) | FUNCIONA | FUNCIONA |
| F08 | Nome do alarme (rótulo) | FUNCIONA | FUNCIONA |
| F09 | Ordem da lista por horário | FUNCIONA | FUNCIONA |
| F10 | Próximo alarme no visor (contagem) | FUNCIONA | FUNCIONA |
| F11 | Estado vazio (convite para criar o primeiro) | FUNCIONA | FUNCIONA |
| F12 | Excluir e tocar em "+" (novo alarme) | FUNCIONA | FUNCIONA |
| F13 | Pular só o próximo dia (lista, gaveta) | FUNCIONA | FUNCIONA |
| F14 | Pular um alarme de uma vez só (sem dias) = desligar | FUNCIONA | FUNCIONA |
| F15 | Desfazer o pulo do dia (voltar a tocar) | FUNCIONA | FUNCIONA |
| F16 | Ligar de volta é o mesmo que tirar o pulo; ligar/desligar à mão apaga o pulo | FUNCIONA | FUNCIONA |
| F17 | Pulo some ao salvar se o dia já passou ou o alarme está desligado | FUNCIONA | FUNCIONA |
| F18 | Pular um toque específico pela notificação "Já acordou?" (confirmar com conta) | FUNCIONA | FUNCIONA |
| F19 | Desfazer o pulo de um toque (pelo recibo ou pela tela) | FUNCIONA | FUNCIONA |
| F20 | Skip de um dia que já passou é ignorado (limpeza no boot ou no reagendamento) | FUNCIONA | FUNCIONA |
| F21 | Perguntar, uma hora antes, sobre alarmes matinais (03:00 a 11:59) | FUNCIONA | FUNCIONA |
| F22 | Agrupar alarmes de reserva numa só pergunta | FUNCIONA | FUNCIONA |
| F23 | Não perguntar nos últimos 5 min antes de um toque | FUNCIONA | FUNCIONA |
| F24 | Não perguntar se o app foi aberto há menos de 90 min | FUNCIONA | FUNCIONA |
| F25 | Cada toque é perguntado uma só vez | FUNCIONA | FUNCIONA |
| F26 | Desligados e pulados ficam de fora da pergunta | FUNCIONA | FUNCIONA |
| F27 | Não perguntar com o telefone em uso ou com a tela desligada (espera) | FUNCIONA | FUNCIONA |
| F28 | Nada é perguntado durante um toque | FUNCIONA | FUNCIONA |
| F29 | Retirar a oferta se o alarme mudou | FUNCIONA | FUNCIONA |
| F30 | Sem permissão de notificação, não há checagem | FUNCIONA | FUNCIONA |
| F31 | Mostrar recibo de pulo com opção de desfazer | FUNCIONA | FUNCIONA |
| F32 | Tocar no horário (som, tela cheia sobre a tela de bloqueio) | FUNCIONA | FUNCIONA |
| F33 | Alarme de uma vez se desliga ao tocar; repetido continua ligado | FUNCIONA | FUNCIONA |
| F34 | Som do alarme: sons do app (5 sons incluídos) | FUNCIONA | FUNCIONA |
| F35 | Som do celular: toques do celular e padrão do sistema | FUNCIONA | FUNCIONA |
| F36 | Música do celular (escolher arquivo) | FUNCIONA | FUNCIONA |
| F37 | Rádio da internet: escolher uma estação popular ou buscar por nome | FUNCIONA | FUNCIONA |
| F38 | Rádio que não começa é trocado pelo alarme padrão após 8 s | FUNCIONA | FUNCIONA |
| F39 | Rádio que tocando sem reação dá lugar ao alarme em volume total (carência de 3 min) | FUNCIONA | FUNCIONA |
| F40 | Aumentar aos poucos (rampa de 15% a 100% em 60 s) | FUNCIONA | FUNCIONA |
| F41 | Vibração enquanto toca, parando com o som | FUNCIONA | FUNCIONA |
| F42 | Notificação do toque diz o que está acontecendo | FUNCIONA | FUNCIONA |
| F43 | Voltar não dispensa o alarme (tela do toque) | FUNCIONA | FUNCIONA |
| F44 | Tela do toque ao abrir depois que o alarme acabou volta ao app | FUNCIONA | FUNCIONA |
| F45 | Tela do toque (hora, botões Espere e Começar missão) | FUNCIONA | FUNCIONA |
| F46 | Espere: 5 min de silêncio, conta de quantas vezes, volta em volume baixo | QUEBRADO | FUNCIONA |
| F47 | Começar missão: silencia o som e mostra o primeiro problema | FUNCIONA | FUNCIONA |
| F48 | Missão matemática: teclado da calculadora (dígitos, C, apagar, sinal) | FUNCIONA | FUNCIONA |
| F49 | Problemas de matemática (fácil: soma de 1 dígito; média: soma de 2 dígitos; difícil: multiplicação) | FUNCIONA | FUNCIONA |
| F50 | Quantidade de problemas (1 a N) e avanço | FUNCIONA | FUNCIONA |
| F51 | Missão de frase do dia (digitar a frase, sem acento e sem diferenças de caixa) | FUNCIONA | FUNCIONA |
| F52 | Prévia da frase do dia na configuração da tarefa | FUNCIONA | FUNCIONA |
| F53 | "Não consigo resolver" troca para a frase | FUNCIONA | FUNCIONA |
| F54 | Tempo da missão (2 min); acabou, toca de novo mais alto (pelo menos 80%) | FUNCIONA | FUNCIONA |
| F55 | Concluir a missão: grava o despertar e encerra o toque | FUNCIONA | FUNCIONA |
| F56 | Tela Bom dia (tempo que levou, esperas, Concluir) | FUNCIONA | FUNCIONA |
| F57 | Bom dia fecha sozinha em 10 min | FUNCIONA | FUNCIONA |
| F58 | Bom dia oferece pular os outros alarmes matinais da próxima hora | FUNCIONA | FUNCIONA |
| F59 | Voltar fecha a tela Bom dia | FUNCIONA | FUNCIONA |
| F60 | Ver estatísticas da semana (histórico de 7 dias) | FUNCIONA | FUNCIONA |
| F61 | Link de privacidade nas estatísticas | FUNCIONA | FUNCIONA |
| F62 | Onboarding de permissões no primeiro uso | FUNCIONA | FUNCIONA |
| F63 | Notificações pedidas no próprio app (só na primeira vez) | FUNCIONA | FUNCIONA |
| F64 | Outras permissões abrem a página de configurações do sistema | FUNCIONA | FUNCIONA |
| F65 | Permissão concedida nas configurações acende ao voltar | FUNCIONA | FUNCIONA |
| F66 | Lê as permissões do sistema (exato, tela cheia, notificações) | FUNCIONA | FUNCIONA |
| F67 | Bateria: só recomendada | FUNCIONA | FUNCIONA |
| F68 | Aviso na lista de permissões faltando; toque leva às permissões e volta com um toque | FUNCIONA | FUNCIONA |
| F69 | Aviso de volume do alarme baixo, com ajuste | FUNCIONA | FUNCIONA |
| F70 | Alarmes reagendados após reinício do celular | FUNCIONA | FUNCIONA |
| F71 | Alarmes reagendados ao trocar de fuso horário | FUNCIONA | FUNCIONA |
| F72 | Alarmes reagendados após atualização do app ou force stop (ao abrir) | FUNCIONA | FUNCIONA |
| F73 | Toque interrompido (processo morto, reinício do celular) continua tocando | FUNCIONA | FUNCIONA |
| F74 | Migração do banco (dados mantidos; amostras antigas removidas; pausa vira pulo) | FUNCIONA | FUNCIONA |
| F75 | Backup do Android (dados extraídos e backup completo) | FUNCIONA | FUNCIONA |
| F76 | Textos em português e inglês | FUNCIONA | FUNCIONA |
| F77 | Tema escuro fixo, independente do sistema | FUNCIONA | FUNCIONA |
| F78 | Canais de notificação (toque silencioso no canal do alarme, pergunta sem som) | FUNCIONA | FUNCIONA |
| F79 | Componentes do manifesto registrados | FUNCIONA | FUNCIONA |
| F80 | Fuso e horário do Brasil nos testes de horário | FUNCIONA | FUNCIONA |
| F81 | Baixar o app | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F82 | Trocar idioma PT/EN e redirecionar pelo idioma do navegador | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F83 | Relógio em flip na capa | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F84 | Vídeo de demonstração com botão de play | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F85 | Demonstração do volume com rampa (interruptor) | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F86 | Demonstração do Espere (segurar o botão) | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F87 | Demonstração da calculadora (checar a resposta, teclado) | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F88 | Demonstração de escolha de som (dial de estações) | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F89 | Gráfico de exemplo "minutos até acordar" | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F90 | Animação Bom dia (12 minutos) | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F91 | Entrada das seções ao rolar | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F92 | Teste no Android (pedir acesso de teste por e-mail) | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F93 | Link de contato no rodapé | NÃO TESTÁVEL | NÃO TESTÁVEL |
| F94 | Reagendar os alarmes ao mudar a hora do celular à mão | QUEBRADO | FUNCIONA |
| F95 | Digitar a hora no editor (4 dígitos; erro para hora que não existe, sem fechar o diálogo) | (nova) | FUNCIONA |
| F96 | Parar a prévia de som (a tecla vira Parar; fonte que não abre falha e a próxima tentativa limpa) | (nova) | FUNCIONA |
| F97 | Aviso de tempo esgotado na tela do toque quando a missão estoura | (nova) | FUNCIONA |
| F98 | Rearme dos alarmes no Direct Boot (cópia protegida lida antes do desbloqueio) | (nova) | FUNCIONA |
| F99 | Rearme dos alarmes ao mudar a hora do celular (ACTION_TIME_CHANGED) | (nova) | FUNCIONA |
| F100 | Token do pulo: um CONFIRM_SKIP forjado por outro app abre só a lista | (nova) | FUNCIONA |
| F101 | Comparação com a média da semana no Bom dia (mais rápido, mais lento, primeiro registro, tempo em segundos) | (nova) | FUNCIONA |
| F102 | Piso de volume audível do alarme enquanto toca, devolvido ao original no fim | (nova) | FUNCIONA |
| F103 | Aviso de canal do toque com importância baixa, na lista | (nova) | FUNCIONA |
| F104 | Tecla de baixar volume consumida no toque e na missão (não no Bom dia) | (nova) | FUNCIONA |
| F105 | Relógio da missão em minutos, com dica nos últimos 20 s | (nova) | FUNCIONA |
| F106 | Busca de rádio com tecla de limpar; "Pronto" volta ao editor | (nova) | FUNCIONA |
| F107 | Site: versão e tamanho do APK sob o botão, passos de instalação, barra fixa de baixar (estrutura do HTML) | (nova) | FUNCIONA |
| F108 | Site: imagem de compartilhamento em inglês, título e descrição curtos, limite de 30 KB de JavaScript, 4 fontes | (nova) | FUNCIONA |
| F109 | Site: script conferidor do APK (depuração, assinatura, versão, 12 MB) | (nova) | FUNCIONA |
| F110 | Site: e-mail do cartão de teste vem de testContact | (nova) | FUNCIONA |
| F111 | App: drawables sem bitmap fora de PNG; versão 1.2 (code 3) igual à do site | (nova) | FUNCIONA |

## 4. Bugs

Legenda: "Corrigido" quer dizer corrigido no código e coberto por teste ou conferido nos prints. "Parcial" quer dizer que a parte que depende do Vinicius ou do aparelho ficou de fora. Todos os 84 itens foram confirmados e nenhum foi descartado. O UX2-09 foi confirmado com ressalva: os prints da auditoria mostravam a tela com dados, então um passo do plano caiu.

### 4.1 Auditoria (64 itens)

| id | título curto | gravidade | confirmado | situação |
|---|---|---|---|---|
| LOG-01 | Celular reiniciado e bloqueado: nenhum alarme toca | alta | confirmado | Parcial: rearme no boot por cópia mínima (`ArmedRings`); o banco não foi movido (risco de perder alarmes). Validar no aparelho |
| LOG-02 | Volume baixo no toque e dá para baixá-lo sem missão | alta | confirmado | Corrigido |
| LOG-03 | Falha antes do som derruba o toque; dois alarmes no mesmo minuto se misturam | média | confirmado | Corrigido |
| LOG-04 | Som escolhido sem proteção depois de começar | média | confirmado | Corrigido |
| LOG-05 | Toque interrompido não volta ao abrir o app | média | confirmado | Corrigido |
| LOG-06 | Ações do toque na tela errada (BUG-1) | média | confirmado | Corrigido |
| LOG-07 | Alarme "poupado" no rearme nunca é rearmado | média | confirmado | Corrigido |
| LOG-08 | Relógio mudado à mão não rearma (BUG-2) | média | confirmado | Corrigido (parte opcional do Espere com `elapsedRealtime` fora do plano: baixo impacto) |
| LOG-09 | Editar a hora de um alarme que pula mantém o pulo | média | confirmado | Corrigido |
| LOG-10 | Segundo alarme durante o ritual do primeiro zera tudo | baixa | confirmado | Corrigido |
| LOG-11 | Desfazer o pulo no último minuto é recusado em silêncio | baixa | confirmado | Corrigido |
| LOG-12 | Falha do `SkipConfirmUiTest` é corrida do teste | baixa | confirmado | Corrigido (teste e gravações não cancelam no meio) |
| LOG-13 | Alarme perdido com o celular desligado não avisa | baixa | confirmado | Parcial: o de uma vez que passou é desligado no boot; notificação "não tocou" fora do plano (mexe no ponto mais delicado) |
| TEC-01 | Sem Direct Boot | alta | confirmado | Parcial: igual ao LOG-01 (cópia completa do alarme fora do plano) |
| TEC-02 | Idas ao banco antes do som; erro no banco deixa mudo | alta | confirmado | Corrigido |
| TEC-03 | `prepare()` na thread principal e player sem `OnErrorListener` | alta | confirmado | Parcial: copiar a música para dentro do app ficou fora (a política de privacidade diz que o app não copia; a cadeia de reserva cobre) |
| TEC-04 | Disco e binder na thread principal na partida e na lista | média | confirmado | Corrigido |
| TEC-05 | Tela do toque fecha antes de a sessão existir | média | confirmado | Corrigido |
| TEC-06 | Backup restaurado: alarmes ligados mas não armados | média | confirmado | Corrigido |
| TEC-07 | Canal "tocando" desligado não é detectado | média | confirmado | Corrigido |
| TEC-08 | Orçamento estourado: 5 fontes e ícone em PNG | média | confirmado | Corrigido (4 fontes; ícone em WebP, 117.515 B para 90.774 B) |
| TEC-09 | Erro de lint `NonObservableLocale` e data em dois idiomas | média | confirmado | Corrigido |
| TEC-10 | `CONFIRM_SKIP` aceito de qualquer app | baixa | confirmado | Corrigido (token) |
| TEC-11 | Polling de 250 ms no `PreAlarmCheck` | baixa | confirmado | Corrigido (broadcast) |
| TEC-12 | Offsets animados recompõem a cada quadro | baixa | confirmado | Corrigido (interruptor, som e rádio) |
| UX1-01 | Minutos exigem arrasto longo; não dá para digitar a hora | alta | confirmado | Corrigido |
| UX1-02 | "Continuar" não continua; sem seta vindo da lista | média | confirmado | Corrigido |
| UX1-03 | Som escolhido e "Aumentar aos poucos" fora da vista | média | confirmado | Corrigido |
| UX1-04 | Prévia de som e rádio não para nem avisa | média | confirmado | Corrigido |
| UX1-05 | Celular pequeno: faixa e avisos presos no topo | média | confirmado | Corrigido |
| UX1-06 | Celular pequeno: editor esconde SOM e MISSÃO | média | confirmado | Corrigido |
| UX1-07 | Fonte grande corta "MISSÃO" | média | confirmado | Corrigido |
| UX1-08 | Bateria (só recomendada) com a mesma tecla laranja | média | confirmado | Corrigido |
| UX1-09 | "PRÓXIMO" sem a hora | média | confirmado | Corrigido |
| UX1-10 | Aviso de permissão discreto e sem saída | baixa | confirmado | Corrigido |
| UX1-11 | "TOQUE DE NOVO" lido como "tocar de novo" | baixa | confirmado | Corrigido |
| UX1-12 | Rádio: duas setas de volta e busca sem limpar | baixa | confirmado | Corrigido |
| UX1-13 | "Toca uma vez" sem o dia | baixa | confirmado | Corrigido |
| UX2-01 | Tempo da missão acaba sem aviso | alta | confirmado | Corrigido (texto na notificação fora do plano: a tela já avisa) |
| UX2-02 | Os mesmos 120 s para 1 ou 10 contas | alta | confirmado | Corrigido (prazo por conta) |
| UX2-03 | Confirmar pulo corta o teclado e tem dois confirmar | alta | confirmado | Corrigido |
| UX2-04 | Teclado da missão só pela largura e longe do visor | média | confirmado | Corrigido |
| UX2-05 | Ações em 12 e 13 sp; botão da missão muda de tamanho | média | confirmado | Corrigido |
| UX2-06 | "SEM LIMITE" lido como aviso | média | confirmado | Corrigido |
| UX2-07 | Bom dia informa mas não recompensa | média | confirmado | Corrigido |
| UX2-08 | Tecla "−" nunca serve | média | confirmado | Corrigido |
| UX2-09 | Estatísticas vazia: cartões "– min" e frase no fim | média | confirmado com ressalva | Corrigido (o passo da espera no tour caiu: os prints já saíam com dados) |
| UX2-10 | Acertar uma conta quase não dá retorno | baixa | confirmado | Corrigido |
| UX2-11 | "Pular" e "desativar" para a mesma ação | baixa | confirmado | Corrigido |
| UX2-12 | Toque e Espere com conteúdo preso no topo | baixa | confirmado | Corrigido |
| UX2-13 | "Errado. Tente de novo." na frase não diz onde | baixa | confirmado | Corrigido |
| SITE-01 | O APK do site é de debug, versão 1.0 | crítica | confirmado | Parcial: script conferidor e passos no README feitos; assinar o APK e trocar o arquivo dependem da sua chave |
| SITE-02 | Produção ainda é o site de 01/10; `/en/` dá 404 | alta | confirmado | Parcial: seção "Conferir depois de publicar" no README; publicar é seu |
| SITE-03 | Texto escuro sobre o degradê (contraste 2,2:1 a 3,8:1) | alta | confirmado | Corrigido |
| SITE-04 | Depois de "Baixar" o visitante fica sozinho | alta | confirmado | Corrigido |
| SITE-05 | Não diz que é grátis, sem anúncios, sem cadastro, offline | média | confirmado | Corrigido |
| SITE-06 | Dois e-mails de contato e "acesso de teste" sem explicação | média | confirmado | Parcial: e-mail do cartão agora vem de `testContact`; escolher o e-mail e o teste fechado são seus |
| SITE-07 | Nega a soneca e depois oferece uma sem limite | média | confirmado | Corrigido |
| SITE-08 | No celular, 6 telas sem botão de baixar | média | confirmado | Corrigido |
| SITE-09 | SEO e imagem de compartilhamento em português na página EN | média | confirmado | Corrigido |
| SITE-10 | Seletor de idioma de 32 px, sem "voltar", nome do botão do vídeo | média | confirmado | Corrigido |
| SITE-11 | Visores das estatísticas vazam no celular pequeno em inglês | baixa | confirmado | Corrigido |
| SITE-12 | Cara de template | baixa | confirmado | Corrigido |
| SITE-13 | Pequenos dados que não batem | baixa | confirmado | Parcial: os itens 2 e 4 dependem de decisão sua |

### 4.2 Revisão de código e revisão visual (20 itens)

| id | título curto | gravidade | confirmado | situação |
|---|---|---|---|---|
| REV-01 | Alarme de uma vez toca de novo no dia seguinte depois de um toque com o celular bloqueado | alta | confirmado | Corrigido |
| REV-02 | Falha em `switchOffMissedOneTimeAlarms` impede o rearme depois do reinício | média | confirmado | Corrigido |
| REV-03 | `resume` duplo em `waitUntilInUse` | média | confirmado | Corrigido |
| REV-04 | TalkBack anuncia a contagem a cada segundo | média | confirmado | Corrigido |
| REV-05 | Mover as preferências do vigia pode apagar uma sessão gravada antes do desbloqueio | baixa | confirmado | Corrigido |
| REV-06 | Volume original perdido se o processo morrer durante o toque | baixa | confirmado | Corrigido |
| REV-07 | Toque sem leitura do banco não arma o próximo toque | baixa | confirmado | Corrigido |
| REV-08 | `MainActivity` presa por 125 s | baixa | confirmado | Corrigido |
| REV-09 | Disco na thread principal no `BootReceiver` bloqueado | baixa | confirmado | Corrigido |
| REV-10 | "Fechou o app?" promete demais | baixa | confirmado | Corrigido |
| REV-11 | Site anuncia uma versão que o arquivo ainda não é | baixa | confirmado | Corrigido (o README manda não publicar sem o APK conferido) |
| REV-12 | Vocabulário do site diferente do app | baixa | confirmado | Corrigido |
| VIS-01 | Texto do Bom dia abaixo de 4,5:1 no celular pequeno | alta | confirmado | Corrigido |
| VIS-02 | Estatísticas no celular pequeno fotografada sem dados | média | confirmado | Corrigido |
| VIS-03 | Confirmar pulo solto no meio em tela alta | média | confirmado | Corrigido |
| VIS-04 | Dois laranjas na lista | média | confirmado | Corrigido |
| VIS-05 | Linha fatiada embaixo do visor do Som | baixa | confirmado | Corrigido |
| VIS-06 | Rodapé das Permissões reserva espaço vazio | baixa | confirmado | Corrigido |
| VIS-07 | Sobretítulos em cima de títulos no site | média | confirmado | Corrigido |
| VIS-08 | Demonstrações do site diferentes do app | baixa | confirmado | Corrigido |

Itens extras da Play Store, fora dos 64: PLAY-01 (ficha), PLAY-02 (versão 1.2), PLAY-03 (política e formulário de dados) feitos; PLAY-04 (origem dos sons) e a parte do gráfico de destaque de PLAY-05 pendentes; o ícone 512 do PLAY-05 foi feito.

## 5. Telas antes e depois

As pastas `.auditoria/prints/` estão no `.gitignore`: não vão para o git e ficam só neste PC. Os links abaixo são relativos à raiz do projeto e só abrem aqui.

Os prints do app saem do `ScreenshotTour` (Robolectric, sem emulador). Cada tela tem 4 variantes: `pt`, `en`, `pequeno` (360x640 dp) e `fonte-grande` (1,3x). A tabela usa a variante `pt`, e `pequeno` quando o problema era no celular pequeno. Os dois conjuntos têm o mesmo nome de arquivo. Os 88 prints do app e os 13 do site de "depois" foram refeitos no estado final (HEAD `f3d7b10`) e o console do site não teve nenhum erro.

### 5.1 App

| tela | antes | depois | o que mudou |
|---|---|---|---|
| 01-lista-vazia (pt) | <img src=".auditoria/prints/antes/app/pt-01-lista-vazia.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-01-lista-vazia.png" width="180"> | Sem mudança de layout apontada nas revisões. |
| 02-lista (pequeno) | <img src=".auditoria/prints/antes/app/pequeno-02-lista.png" width="180"> | <img src=".auditoria/prints/depois/app/pequeno-02-lista.png" width="180"> | Faixa PRÓXIMO e avisos rolam com a lista e sobra espaço para os cartões (UX1-05, UX1-09, UX1-10). |
| 03-lista-desligou-recorrente (pt) | <img src=".auditoria/prints/antes/app/pt-03-lista-desligou-recorrente.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-03-lista-desligou-recorrente.png" width="180"> | PRÓXIMO com a hora; PULAR e volume em grafite, NOVO é o único laranja (UX1-09, VIS-04). |
| 04-lista-pulando-um-dia (pt) | <img src=".auditoria/prints/antes/app/pt-04-lista-pulando-um-dia.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-04-lista-pulando-um-dia.png" width="180"> | Cartão pulando um dia; o texto segue o vocabulário "pular" (UX2-11). |
| 05-permissoes (pt) | <img src=".auditoria/prints/antes/app/pt-05-permissoes.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-05-permissoes.png" width="180"> | Sem sobretítulo, só a obrigatória fica laranja, descrição da bateria diz o que fazer (UX1-02, UX1-08, VIS-06). |
| 06-permissoes-aviso (pt) | <img src=".auditoria/prints/antes/app/pt-06-permissoes-aviso.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-06-permissoes-aviso.png" width="180"> | Continuar continua em um toque, risco visível, seta e "Voltar para a lista" (UX1-02, VIS-06). |
| 07-novo-alarme (pequeno) | <img src=".auditoria/prints/antes/app/pequeno-07-novo-alarme.png" width="180"> | <img src=".auditoria/prints/depois/app/pequeno-07-novo-alarme.png" width="180"> | Missão e Som antes do Nome, barra de baixo menor, dica com o dia (UX1-06, UX1-13). |
| 07b-digitar-hora (pt) | tela nova, sem antes | <img src=".auditoria/prints/depois/app/pt-07b-digitar-hora.png" width="180"> | Diálogo novo "Digite a hora", 4 dígitos, tecla laranja única (UX1-01). |
| 08-editando-excluir-armado (pequeno) | <img src=".auditoria/prints/antes/app/pequeno-08-editando-excluir-armado.png" width="180"> | <img src=".auditoria/prints/depois/app/pequeno-08-editando-excluir-armado.png" width="180"> | Ordem Missão, Som, Nome; "EXCLUIR MESMO?" (UX1-06, UX1-07, UX1-11). |
| 09-tarefa-matematica (pt) | <img src=".auditoria/prints/antes/app/pt-09-tarefa-matematica.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-09-tarefa-matematica.png" width="180"> | Configuração da tarefa de matemática. |
| 10-tarefa-frase (pt) | <img src=".auditoria/prints/antes/app/pt-10-tarefa-frase.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-10-tarefa-frase.png" width="180"> | Igual ao antes (conferido por pixel nas 4 variantes). |
| 11-som (pt) | <img src=".auditoria/prints/antes/app/pt-11-som.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-11-som.png" width="180"> | "Aumentar aos poucos" no topo, grupos "sons do app" e "do celular", prévia com tecla de parar, topo sem linha fatiada (UX1-03, UX1-04, VIS-05). |
| 12-radio (pt) | <img src=".auditoria/prints/antes/app/pt-12-radio.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-12-radio.png" width="180"> | Igual ao antes neste estado (conferido por pixel). A mudança do UX1-12 (Pronto e tecla de limpar a busca) não aparece aqui. |
| 13-tocando (pt) | <img src=".auditoria/prints/antes/app/pt-13-tocando.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-13-tocando.png" width="180"> | Rótulos do Espere maiores, "Toque para adiar", etiqueta escura no Começar missão, conteúdo centrado (UX2-05, UX2-06, UX2-12). |
| 14-espere (pt) | <img src=".auditoria/prints/antes/app/pt-14-espere.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-14-espere.png" width="180"> | Mesmos rótulos e centralização do toque (UX2-05, UX2-06, UX2-12). |
| 15-missao (pequeno) | <img src=".auditoria/prints/antes/app/pequeno-15-missao.png" width="180"> | <img src=".auditoria/prints/depois/app/pequeno-15-missao.png" width="180"> | Teclado usa a altura e fica colado no visor, sem "−", "TEMPO 1:14", luzes de progresso (UX2-01, UX2-04, UX2-08, UX2-10). |
| 16-missao-errou (pt) | <img src=".auditoria/prints/antes/app/pt-16-missao-errou.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-16-missao-errou.png" width="180"> | Dica e erro em 14 sp (UX2-05). |
| 17-missao-frase (pt) | <img src=".auditoria/prints/antes/app/pt-17-missao-frase.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-17-missao-frase.png" width="180"> | Erro da frase diz onde está o problema (UX2-13). |
| 18-bom-dia (pequeno) | <img src=".auditoria/prints/antes/app/pequeno-18-bom-dia.png" width="180"> | <img src=".auditoria/prints/depois/app/pequeno-18-bom-dia.png" width="180"> | Linha de comparação com a média da semana, em etiqueta escura (UX2-07, VIS-01). |
| 19-estatisticas (pequeno) | <img src=".auditoria/prints/antes/app/pequeno-19-estatisticas.png" width="180"> | <img src=".auditoria/prints/depois/app/pequeno-19-estatisticas.png" width="180"> | Estado vazio orienta no topo; só desenha depois de ler o banco (UX2-09, VIS-02). |
| 20-lista-oferta-pular (pt) | <img src=".auditoria/prints/antes/app/pt-20-lista-oferta-pular.png" width="180"> | <img src=".auditoria/prints/depois/app/pt-20-lista-oferta-pular.png" width="180"> | Oferta de pular, um laranja só na tela (VIS-04, UX2-11). |
| 21-confirmar-desativar (pequeno) | <img src=".auditoria/prints/antes/app/pequeno-21-confirmar-desativar.png" width="180"> | <img src=".auditoria/prints/depois/app/pequeno-21-confirmar-desativar.png" width="180"> | Teclado cabe inteiro, um confirmar só, "Pular só desta vez?" (UX2-03, VIS-03). |

### 5.2 Site

Mesmos nomes, viewports (390x844 no celular, 1440x900 no desktop) e estados dos dois lados. Os prints "antes" do início só mostram o topo e o fim da página, porque as seções entravam por animação e saíam pretas; hoje elas não dependem mais disso.

| página | antes | depois | o que mudou |
|---|---|---|---|
| Início PT, celular | <img src=".auditoria/prints/antes/site/celular-inicio-pt.png" width="180"> | <img src=".auditoria/prints/depois/site/celular-inicio-pt.png" width="180"> | Botão em etiqueta escura, versão e tamanho, "Como instalar", faixa de fatos, botão fixo de baixar (SITE-03, 04, 05, 08). |
| Início EN, celular | <img src=".auditoria/prints/antes/site/celular-inicio-en.png" width="180"> | <img src=".auditoria/prints/depois/site/celular-inicio-en.png" width="180"> | Mesmas mudanças em inglês; SEO e título novos (SITE-09). |
| Privacidade PT, celular | <img src=".auditoria/prints/antes/site/celular-privacidade-pt.png" width="180"> | <img src=".auditoria/prints/depois/site/celular-privacidade-pt.png" width="180"> | Data atualizada, item da cópia mínima dos horários, link de voltar (PLAY-03, SITE-10). |
| Privacidade EN, celular | <img src=".auditoria/prints/antes/site/celular-privacidade-en.png" width="180"> | <img src=".auditoria/prints/depois/site/celular-privacidade-en.png" width="180"> | Igual à versão em português. |
| 404, celular | <img src=".auditoria/prints/antes/site/celular-404-pt.png" width="180"> | <img src=".auditoria/prints/depois/site/celular-404-pt.png" width="180"> | Sem mudança (idêntico ao antes). |
| Início PT, desktop, topo | <img src=".auditoria/prints/antes/site/desktop-inicio-pt-topo.png" width="180"> | <img src=".auditoria/prints/depois/site/desktop-inicio-pt-topo.png" width="180"> | Rótulo do botão, versão e instalação legíveis sobre o degradê (SITE-03, SITE-04). |
| Início PT, desktop, página inteira | <img src=".auditoria/prints/antes/site/desktop-inicio-pt.png" width="180"> | <img src=".auditoria/prints/depois/site/desktop-inicio-pt.png" width="180"> | Sem sobretítulos, sem entrada igual em todas as seções, fim da página com passos e cartão de teste (SITE-12, VIS-07). |
| Início PT, desktop, demos clicadas | <img src=".auditoria/prints/antes/site/desktop-inicio-pt-demos-clicadas.png" width="180"> | <img src=".auditoria/prints/depois/site/desktop-inicio-pt-demos-clicadas.png" width="180"> | Demonstrações iguais ao app: sem tecla "−", "Toque para adiar" (VIS-08). |
| Início EN, desktop, topo | <img src=".auditoria/prints/antes/site/desktop-inicio-en-topo.png" width="180"> | <img src=".auditoria/prints/depois/site/desktop-inicio-en-topo.png" width="180"> | Mesmas mudanças do topo em inglês. |
| Início EN, desktop, página inteira | <img src=".auditoria/prints/antes/site/desktop-inicio-en.png" width="180"> | <img src=".auditoria/prints/depois/site/desktop-inicio-en.png" width="180"> | Mesmas mudanças em inglês (SITE-07, SITE-09). |
| Privacidade PT, desktop | <img src=".auditoria/prints/antes/site/desktop-privacidade-pt.png" width="180"> | <img src=".auditoria/prints/depois/site/desktop-privacidade-pt.png" width="180"> | Data e item novo, layout igual (PLAY-03, VIS-07). |
| Privacidade EN, desktop | <img src=".auditoria/prints/antes/site/desktop-privacidade-en.png" width="180"> | <img src=".auditoria/prints/depois/site/desktop-privacidade-en.png" width="180"> | Igual à versão em português. |
| 404, desktop | <img src=".auditoria/prints/antes/site/desktop-404.png" width="180"> | <img src=".auditoria/prints/depois/site/desktop-404.png" width="180"> | Sem mudança (idêntico ao antes). |

## 6. Performance

Limites de `.auditoria/orcamento.md`. "Antes" é de `performance-antes.md`. "Depois" é de `performance-depois.md` e da medição final (build, testes e lint rodados de novo no fim). **Nenhum limite estourou.**

| item | antes | depois | limite | dentro? |
|---|---|---|---|---|
| APK release sem assinatura | 4.066.524 B | 4.132.133 B (+1,61%) | até +5% (4.269.850 B) e 12 MB | sim |
| `classes.dex` | 2.479.656 B | 2.635.860 B (+6,3%, medido antes das correções da revisão) | conta no total do APK | vale acompanhar |
| `res/raw` (7 sons) total | 2.105.236 B | 2.105.236 B | até 2,5 MB | sim |
| `res/raw` maior arquivo | 300.717 B | 300.717 B | até 400 KB | sim |
| `res/font` | 419.212 B, 5 arquivos | igual | até 450 KB e 5 arquivos | sim (no limite de arquivos) |
| Ícone (`drawable-nodpi`) | PNG, 117.515 B | WebP, 90.774 B (-22,8%) | bitmap só em WebP | sim (antes não) |
| Testes JVM | não concluíam (`ClassNotFoundException`) | 385 testes, 0 falhas, 0 erros, 28 ignorados, 127,4 s somados | suíte inteira passa | sim |
| Lint | 1 erro, 50 avisos | 0 erros, 57 avisos | sem limite (erro novo é proibido) | sim |
| Lighthouse celular PT (Perf./Acess./Boas práticas/SEO) | 97 / 100 / 100 / 100 | 97 / 100 / 100 / 100 | 90 ou mais | sim |
| Lighthouse celular EN | 97 / 100 / 100 / 100 | 97 / 100 / 100 / 100 | 90 ou mais | sim |
| Página inicial PT sem vídeo e sem APK | 225.960 B | 214.759 B | até 500 KB | sim |
| Página inicial EN sem vídeo e sem APK | não medido | 214.742 B | até 500 KB | sim |
| JavaScript próprio do site | 19.361 B | 22.379 B | até 30 KB | sim |
| Fontes woff2 | 5 (104.988 B) | 4 (84.352 B) | no máximo 4 | sim (antes não) |
| `font-display` | swap | swap nas 4 | swap | sim |
| Vídeo | `preload="none"` com pôster | igual | nunca baixado antes do pedido | sim |
| Imagens acima de 50 KB na página | nenhuma | nenhuma (as `og-image` passam de 50 KB mas só entram em `og:image`) | WebP/AVIF acima de 50 KB | sim |

Notas:

- Os números de lint não são comparáveis linha a linha: a linha de base veio do XML e a medição seguinte, do texto. Na medição final foram 57 avisos (eram 60 antes das correções da revisão).
- O Lighthouse foi medido antes das correções finais do site (REV-10 a REV-12, VIS-07 e VIS-08). Depois delas só os prints do site foram refeitos (13 PNG, 0 erro de console), e o Lighthouse não foi rodado de novo.
- Os 28 testes ignorados são os do `ScreenshotTour`, que só gravam prints. Não há mais nenhum teste de bug ignorado.
- Das 7 suspeitas de performance do app (medição feita em e737427): a 2 (`rescheduleAll` na thread principal) e a 5 (polling de 250 ms) foram resolvidas, a 1 (`onStart`) ficou parcial, e a 3 (som só depois de ler o banco), a 4 (`prepare()` síncrono) e as 6 e 7 (grade de furos e riscas desenhadas a cada draw, custo não medido) continuam no código. A 6 já tinha sido avaliada e descartada pela auditoria técnica.

**O que não deu para medir sem celular:** partida a frio (limite 1,5 s), memória ao tocar (limite 150 MB PSS), FPS na rolagem e nos dígitos flip (limite 60), tempo para abrir a tela do toque sobre o bloqueio (limite 1 s), bateria e o tempo de `assembleRelease` depois das mudanças. Para medir: `adb shell am start -W com.wakewakeup/.MainActivity` e `adb shell dumpsys meminfo com.wakewakeup`.

## 7. Assets

### O que foi gerado

| peça | onde está | como foi feita |
|---|---|---|
| Ícone da loja 512x512 | `assets/marketing/play-store/play-icone-512.png` | ffmpeg, recorte do ícone atual (sai idêntico; sem Canva) |
| Imagem de compartilhamento em inglês | `site/og-image-en.png` | HTML `ferramentas/og-image.html`, igual à portuguesa |
| Ficha da Play em PT-BR e EN-US | `assets/marketing/play-store/ficha-pt-BR.md` e `ficha-en-US.md` | texto, com contagem de caracteres |
| Respostas de "Segurança dos dados" | `assets/marketing/play-store/seguranca-dos-dados.md` | tiradas da política de privacidade |
| README das peças de divulgação | `assets/marketing/README.md` | uma linha por peça, com ferramenta e plano |
| Ícone do app em WebP | `app/src/main/res/drawable-nodpi/ic_launcher_foreground_art.webp` | conversão sem perda (TEC-08) |

### O que não foi gerado

**Música:** o app não usa. Suno fora.

**Gráfico de destaque da Play (PT e EN):** o conector do Canva não deixa escolher a fonte Instrument Sans do guia visual, e o guia manda parar se a fonte não estiver garantida. A tentativa saiu em 1264x1264, com fundo e cores diferentes das do guia. Há um rascunho descartável no Canva (`https://www.canva.com/d/3hACPL6Kr0GQ7-s`) que não deve ser usado.

Briefing para montar a peça (no Canva escolhendo Instrument Sans à mão, ou local com ffmpeg ou HTML, se a fonte estiver instalada):

| item | valor |
|---|---|
| Tamanho | 1024x500 px, PNG, até 1 MB, sem transparência |
| Fundo | `#141017` |
| Nascer do sol | só na base, baixo: `#6B2433` para `#E8663C` para `#F7D46B` |
| Relógio | à direita; recorte real do relógio flip de `site/og-image.png` (faixa do relógio, sem os textos): `ffmpeg -i site/og-image.png -vf "crop=612:226:294:90"`. Não redesenhar |
| Frase principal (esquerda) | Instrument Sans SemiBold, creme `#F6ECE0`. PT: "Acorde de verdade." EN: "Wake up for real." |
| Linha de apoio | caixa alta, peso 600, espaçamento largo, cor `#B49E88`. PT: "O DESPERTADOR QUE SÓ DESLIGA COM UMA MISSÃO". EN: "THE ALARM THAT ONLY TURNS OFF AFTER A MISSION" |
| Composição | alinhado à esquerda, não centralizado; margem de segurança de 15% em toda a volta; legível em 25% do tamanho |
| Proibido | trocar a família da fonte; geração de imagem por IA do Canva; sobretítulo em cima de título (ver `GUIA-VISUAL.md`, seção 11) |
| Arquivos | `assets/marketing/play-store/play-destaque-pt.png` e `assets/marketing/play-store/play-destaque-en.png` |
| Destino | Play Console, ficha da loja, "Gráfico de destaque" (um por idioma da ficha) |

**Sons (ElevenLabs):** o servidor `elevenlabs` não estava autenticado. Nenhum som foi gerado, e a origem e a licença dos 7 sons de `res/raw` ainda não estão registradas (PLAY-04). Se você não conseguir confirmar a origem, autorize o ElevenLabs em `/mcp` e peça a rodada.

Regras da rodada: 7 sons x 3 variações = 21 gerações (abaixo do limite de 30); candidatos em `assets/audio/_candidatos/`; 12 s com loop ligado; o arquivo final mantém **exatamente** o nome atual em `app/src/main/res/raw/` (os alarmes salvos guardam o som pelo nome); MP3 de 192 kbps, até 400 KB cada e 2,5 MB no total; pico em -1 dB (hoje cinco deles estão em 0,0 dB); registrar no manifesto o serviço e o plano. Identidade sonora: rádio-relógio dos anos 70, sons mecânicos e eletrônicos simples, secos e altos.

| Arquivo final (não renomear) | Onde entra | Alvo de volume | Prompt |
|---|---|---|---|
| `campainha_mecanica.mp3` | som do app e reserva quando qualquer outro falha | -9 LUFS | Vintage 1970s mechanical twin-bell alarm clock ringing nonstop, two small steel hammers hitting brass bells very fast, close dry recording, no reverb, no room tone, very loud and harsh, constant ring with no pause, seamless loop |
| `telefone_de_disco.mp3` | som do app | -8 LUFS | Old rotary dial telephone ringing, classic electromechanical double ring then a short pause, repeating, metal bell inside a bakelite body, close dry recording, no reverb, loud, seamless loop |
| `bipe_digital_1.mp3` | som do app | -9 LUFS | 1970s digital alarm clock beep, short insistent square-wave beeps around 2 kHz, four fast beeps then a brief gap, repeating, completely dry, no reverb, loud, seamless loop |
| `bipe_digital_2.mp3` | som do app | -8 LUFS | Vintage clock radio piezo buzzer alarm, steady fast two-tone electronic beep alternating high and low, bright and harsh, completely dry, loud, seamless loop |
| `sirene_1.mp3` | som do app | -6 LUFS | Loud mechanical hand-crank style siren wailing slowly up and down in pitch, harsh and urgent, close dry recording, no reverb, no background noise, seamless loop |
| `sirene_2.mp3` | som do app | -5 LUFS | Loud two-tone electronic alarm siren alternating high and low every half second, 1970s horn speaker burglar alarm, harsh, completely dry, seamless loop |
| `passaros.mp3` | som do app (ambiente) | -15 LUFS | Dawn chorus of songbirds outdoors, several birds singing brightly and close, lively and dense with no quiet gaps, no wind, no traffic, no water, natural dry recording, seamless loop |

Pós-processo de cada escolhido, com `<alvo>` da tabela:

`ffmpeg -i candidato.mp3 -af "silenceremove=start_periods=1:start_threshold=-50dB,loudnorm=I=<alvo>:TP=-1.0:LRA=7" -ar 44100 -b:a 192k app/src/main/res/raw/<nome>.mp3`

Ouça no celular antes de trocar: alarme baixo demais é defeito. Observação do medidor: os 7 mp3 têm exatamente 300.717 B, mas md5 diferentes. Vale conferir se são arquivos de verdade.

## 8. Skills

Instaladas só no projeto, em `.claude/skills/` (nada global). Todos os `SKILL.md` e scripts foram lidos antes; nenhum script foi executado. A skill de design principal é a **impeccable**.

| skill | origem | licença | para quê |
|---|---|---|---|
| impeccable (v4.11.1) | github.com/pbakaus/impeccable @ 2fc5e14 | Apache-2.0 | design, crítica, polimento, animação, anti-padrões de "cara de IA", referência para Android |
| ux-writing | github.com/content-designer/ux-writing-skill @ 98cacde | MIT | textos de interface: botões, erros, estados vazios, acessibilidade do texto |
| jetpack-compose-performance | github.com/android/skills @ 7f7b217 | Apache-2.0 | recomposição, listas, animação |
| edge-to-edge | android/skills @ 7f7b217 | Apache-2.0 | insets, barras do sistema, teclado |
| testing-setup | android/skills @ 7f7b217 | Apache-2.0 | estratégia de testes (Robolectric, Compose, prints) |
| android-permissions-security | android/skills @ 7f7b217 | Apache-2.0 | permissões, componentes exportados, manifesto |
| android-intent-security | android/skills @ 7f7b217 | Apache-2.0 | intents e PendingIntent de alarmes e notificações |
| anr-audit-and-fix | android/skills @ 7f7b217 | Apache-2.0 | riscos de ANR em receivers, services e thread principal |
| seo-audit | github.com/coreyhaines31/marketingskills @ 8fd2fca | MIT | SEO técnico do site, hreflang PT/EN |
| aso | coreyhaines31/marketingskills @ 8fd2fca | MIT | ficha da Google Play |

O pedido de "instalar todas as skills que evoluam o projeto" levou o limite de 8 para até 12. Foram instaladas 10.

**Recusadas:**

- `anthropics/skills` frontend-design e `nextlevelbuilder/ui-ux-pro-max-skill`: duplicam a impeccable. Só uma skill de design.
- `play-policy-insights` (android/skills): scripts Python que fazem scraping da Play Store. Não instalada (rede e execução). A checagem de políticas fica manual.
- `r8-analyzer`: scripts Python/protobuf e ganho pequeno. `navigation-3`, `agp-9-upgrade`, `adaptive`, `camera`, `wear`, `xr`, `tv`: fora do escopo (app só de celular, e já está no AGP 9.3.1).
- `Kotlin/kotlin-agent-skills`: só migração, backend e KMP.
- Skills Compose da comunidade (aldefy, Meet-Miyani, skydoves, awesome-android-agent-skills): sobrepõem as do Google, e uma delas tem licença NOASSERTION. Ficaram as oficiais.
- Skills de Material 3 Expressive da comunidade: poucas estrelas e conflitam com a estética da impeccable.
- Resto do `marketingskills` (copywriting, cro, ads): fora do foco.

**Cuidados com a impeccable:** o launcher `scripts/impeccable` baixa um binário do GitHub Releases na primeira execução (com SHA-256). Não foi executado. Confirme antes de rodar; sem ele a skill funciona só com a leitura dos arquivos. Não use `hooks on` sem avisar. Já existe uma versão global diferente em `~/.claude/skills/impeccable`; a do projeto tem prioridade.

**Valeriam para todos os projetos** (instalação global, decisão sua; são comandos do instalador `skills`, que não foram usados, só a cópia manual):

- impeccable (a global é de outra versão): `npx skills add pbakaus/impeccable -g`, ou copiar `.claude/skills/impeccable` para `~/.claude/skills/impeccable`
- ux-writing: `npx skills add content-designer/ux-writing-skill -g`
- seo-audit (qualquer site): `npx skills add coreyhaines31/marketingskills --skill seo-audit -g`
- Outros apps Android: `npx skills add android/skills --skill jetpack-compose-performance -g` (idem `edge-to-edge` e `testing-setup`)

**Créditos de licença:** impeccable, jetpack-compose-performance, edge-to-edge, testing-setup, android-permissions-security, android-intent-security e anr-audit-and-fix são Apache-2.0: o `LICENSE` e o `NOTICE` foram copiados em cada pasta e devem ficar lá. ux-writing, seo-audit e aso são MIT. Nenhuma exige crédito público.

## 9. O que ficou pendente e por quê

1. **Validação em aparelho.** Não há emulador neste PC; tudo foi provado na JVM (Robolectric). Direct Boot, volume, canal, restauração de backup, relógio e partida a frio só se confirmam no celular. Roteiro na seção 10.
2. **APK assinado (SITE-01, crítico).** O `site/wake-wake-up.apk` ainda é o build de debug 1.0. Falta a sua chave de upload. Há script que confere o APK antes de ir para o site.
3. **Publicação do site (SITE-02).** A produção ainda é o site de 01/10 (`/en/` e a privacidade em inglês dão 404). Só publicar junto com o APK novo.
4. **E-mail (SITE-06, SITE-13).** Rodapé e política usam `cyberhat.tech@gmail.com`; o cartão de teste usa `vinicostamaga@outlook.com`. Falta você escolher um.
5. **Gráfico de destaque da Play (PLAY-05).** Não gerado, ver seção 7.
6. **Origem e licença dos 7 sons (PLAY-04).** Entraram no commit `a405a0b` em 01/10/2026, sem registro. Sem isso não dá para conferir o uso comercial.
7. **Vídeo do site.** O vídeo de demonstração ainda mostra "SEM LIMITE" (o app agora diz "Toque para adiar") e é em português na página em inglês. Não foi refeito.
8. **Avisos de lint que ficaram de fora** (57 avisos, nenhum erro): `UseKtx`, versões novas de Kotlin, Gradle e core-ktx, `ModifierParameter` e `UnusedResources` fora das órfãs. Atualizar dependências agora arrisca a base logo antes do lançamento.
9. **Fora do plano, por decisão:** mover o banco para o armazenamento protegido (LOG-01), cópia completa do alarme antes do desbloqueio (TEC-01), copiar a música escolhida para o app (TEC-03), notificação "não tocou" depois do boot (LOG-13), texto na notificação quando o tempo da missão acaba (UX2-01), contar o Espere com `elapsedRealtime` (LOG-08), minificar CSS e tirar o `lang.js` do `<head>` (Lighthouse já em 97), testes automáticos do JavaScript do site (F81 a F93) e limite para o Espere ou mais de um dia pulado (decisões de produto).
10. **Testes intermitentes:** nenhum. O agente final não achou falha estável nem intermitente. Os 28 ignorados são só os do tour de prints.
11. **Suspeitas de performance 3, 4, 6 e 7** do app seguem como estavam na última medição (seção 6). Não foram medidas.
12. **Arquivos fora do git:** na hora deste relatório, `.auditoria/PROGRESSO.md`, `.auditoria/performance-depois.md`, `CLAUDE.md` e a pasta `.claude/agents/` estavam modificados ou novos e sem commit. Este relatório também não foi commitado.
13. **Nada enviado ao GitHub** e sem merge no `main`. O arquivo `wake-wake-up-site.zip` da raiz (26,5 MB) tem o site e o APK antigos.

## 10. Como testar as mudanças, passo a passo

Todos os comandos rodam na raiz do projeto, no Git Bash. Use sempre `ferramentas/gradle.sh`: o caminho tem acento e o script contorna isso montando a pasta numa letra de unidade.

1. **Testes:** `bash ferramentas/gradle.sh :app:testDebugUnitTest`. Esperado: 385 testes, 0 falhas, 28 ignorados (cerca de 2 minutos de testes; o build completo levou 7m27s).
2. **Lint:** `bash ferramentas/gradle.sh :app:lintDebug`. Esperado: 0 erros.
3. **APK de release e tamanho:** `bash ferramentas/gradle.sh :app:assembleRelease`. O arquivo fica em `app/build/outputs/apk/release/`. Esperado: até 4.269.850 B.
4. **Prints do app:** `WWU_SCREENSHOTS=1 bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"`. Os PNG saem em `app/build/screenshots/`. Rode sozinho, sem outros testes.
5. **Instalar no celular:** ative a depuração USB, plugue o cabo e rode `bash ferramentas/gradle.sh :app:assembleDebug`. Depois `adb install -r app/build/outputs/apk/debug/app-debug.apk`. O `adb` fica em `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.
6. **Validação em aparelho** (checklist do `MELHORIAS.md`):
   - **Direct Boot (LOG-01, TEC-01):** crie um alarme para daqui a 10 min, reinicie o celular e **não desbloqueie**. Esperado: toca a campainha do app com a missão padrão, por cima da tela de bloqueio; concluir a missão fecha a tela sem erro. Depois de desbloquear, a lista está igual. Repita com um alarme de uma vez: depois de desbloquear ele aparece desligado.
   - **Restauração (TEC-06):** `adb shell bmgr backupnow com.wakewakeup`, desinstale, reinstale, `adb shell bmgr restore com.wakewakeup` e, sem abrir o app, `adb shell dumpsys alarm | findstr wakewakeup` mostra os alarmes.
   - **Volume (LOG-02):** com o volume de alarme no mínimo o alarme toca audível; a tecla de volume para baixo não faz nada durante o toque; ao concluir, o volume volta ao que era.
   - **Som escolhido (LOG-04, TEC-03):** use uma música do celular como som e, com o alarme tocando, apague o arquivo: outro som assume.
   - **Canal (TEC-07):** desligue só o canal "Alarmes" nas notificações do app: a lista avisa.
   - **Relógio (LOG-08):** mude a data para o futuro e volte; `dumpsys alarm` mostra o toque no instante certo.
   - **Partida (TEC-04):** `adb shell am start -W com.wakewakeup/.MainActivity` abre a frio em até 1,5 s.
   - **Sons:** ouça os 7 e veja se algum estoura.
7. **Site local:** `python -m http.server 8765 --directory site` e abra `http://localhost:8765/` (PT) e `http://localhost:8765/en/` (EN). Confira: botão com versão e tamanho, passos de instalação, botão fixo de baixar no celular, seletor de idioma, privacidade e 404. Para ver no celular, siga o procedimento do Tailscale do seu `CLAUDE.md` global. Feche o servidor ao terminar.
8. **APK do site:** `bash ferramentas/conferir-apk-site.sh app/build/outputs/apk/release/app-release.apk` (só depois de assinar). Ele falha se o APK for de debug, sem assinatura, com versão diferente da do site ou acima de 12 MB.

## 11. Sugestões que precisam do Vinicius

1. **Assinar o APK de release (SITE-01, crítico).** Ache ou crie sua chave de upload (`.jks`). Na raiz do projeto, crie `keystore.properties` com `storeFile=`, `storePassword=`, `keyAlias=` e `keyPassword=` (o arquivo já está no `.gitignore`). Rode `bash ferramentas/gradle.sh :app:assembleRelease`, depois `bash ferramentas/conferir-apk-site.sh app/build/outputs/apk/release/app-release.apk`. Se passar, copie para `site/wake-wake-up.apk` e confira `apkSize` em `site/site-config.js`. Para a Play, o envio é em AAB: `bash ferramentas/gradle.sh :app:bundleRelease`. Guarde a chave fora do OneDrive e com cópia: sem ela não há atualização.
2. **Publicar o site (SITE-02).** Só junto com o APK novo. Siga `site/README.md`, "Publicar na Hostinger" (enviar `site/` inteiro com `en/` e `.htaccess`, limpar o cache do CDN), rode os seis testes de "Conferir depois de publicar" e reenvie o `sitemap.xml` no Search Console. Quem tem o APK antigo (debug 1.0) precisa desinstalar antes; a página avisa.
3. **Escolher o e-mail (SITE-06).** Para ficar com um só (recomendado: o da política), apague a linha `testContact` de `site/site-config.js` e a menção ao outro endereço em `site/README.md`. Se preferir o outro, troque `contact` e as três menções na política (PT e EN).
4. **Teste fechado na Play (SITE-06, passo 3).** Se estiver ativo, avise e o cartão passa a dizer "Prefere instalar pela loja?". Se não, o cartão fica como está.
5. **Validar no celular.** Faça o roteiro do passo 6 da seção 10. Nada disso se prova na JVM.
6. **Sons (PLAY-04).** Confirme de onde vieram os 7 arquivos de `res/raw` (commit `a405a0b`, 01/10/2026) e anote serviço, plano e licença em `assets/audio/audio_manifest.md`. Se não der, autorize o ElevenLabs em `/mcp` e peça a rodada de 21 gerações da seção 7.
7. **Gráfico de destaque da Play (novo).** Monte as duas peças (PT e EN) pelo briefing da seção 7, escolhendo Instrument Sans à mão no Canva, ou peça para montar localmente por HTML se a fonte estiver instalada neste PC.
8. **Play Console.** Com a conta de desenvolvedor: criar o app; colar a ficha de `assets/marketing/play-store/ficha-*.md`; enviar o destaque, o ícone e os prints (de 4 a 6 prints `pt` e `en` do tour, copiados para `assets/marketing/play-store/prints/`); preencher "Segurança dos dados" com `seguranca-dos-dados.md`; apontar a política para `https://wakewakeup.cyberhat.com.br/privacidade.html` (o site novo precisa estar no ar); declarar alarme exato, notificação de tela cheia e serviço em primeiro plano `systemExempted` (o app é um despertador, o caso de uso previsto); responder a classificação de conteúdo. Contas pessoais novas precisam de teste fechado com 12 testadores por 14 dias antes da produção. Quando o app estiver na loja, preencha `storeUrl` em `site/site-config.js`.
9. **Versão.** O plano usa `versionCode 3` e `versionName "1.2"`. Para outro número, troque em `app/build.gradle.kts`, em `site/site-config.js` (`apkVersion`) e no JSON-LD das duas páginas iniciais.
10. **Pequenas decisões.** Grafia do nome da empresa ("CyberHat" no site, "Cyberhat" no `README.md`): escolha uma. O `wake-wake-up-site.zip` da raiz tem o site e o APK antigos (26,5 MB): posso apagar depois da publicação nova?
11. **Vídeo do site.** Decida se refaz o vídeo de demonstração (hoje mostra "SEM LIMITE" e é em português na página em inglês) ou se deixa assim por ora.
12. **Guardar o trabalho.** Faça o commit dos arquivos que ficaram sem commit (`.auditoria/PROGRESSO.md`, `.auditoria/performance-depois.md`, `CLAUDE.md`, `.claude/agents/` e este relatório). Depois, `git push` e o merge de `melhorias-claude` no `main` são seus; nada foi enviado.
13. **Impeccable.** Se quiser o detector da skill, confirme antes de rodar `scripts/impeccable`, que baixa um binário do GitHub.

## 12. Modelos usados em cada fase

| fase | frente | modelo |
|---|---|---|
| 0 | Preparação (orquestrador) | Opus 5.5 |
| 1 | Mapa, Ferramentas, Inventário, Medidor, Prints | haiku (effort low) |
| 1 | Testador, Skills | sonnet (medium) |
| 2 | Auditores de Lógica, UI/UX 1, UI/UX 2 e Site | claude-fable-5-2 (high) |
| 2 | Auditor Técnico | opus (high) |
| 2 | Manutenção de testes e prints do editor | sonnet |
| 3 | 5 confirmadores | sonnet |
| 4 | Planejador | claude-fable-5-2 (high) |
| 5 | 9 executores (um por lote) | sonnet (medium) |
| 6 | Revisor de Código | opus (high) |
| 6 | Prints depois e Medidor depois | haiku |
| 6 | Testador depois | sonnet |
| 6 | Revisor Visual | claude-fable-5-2 (high) |
| 6 | 4 corretores | sonnet |
| 6 | Verificação final (build, testes, prints) | haiku |
| 7 | Instalação do fluxo de agentes | orquestrador |
| 8 | Redator deste relatório | sonnet |

O Fable (claude-fable-5-2) esteve disponível o tempo todo: nenhuma troca por Opus.

Tokens de subagentes, aproximados:

| etapa | tokens |
|---|---|
| Fase 1 | 0,97 M |
| Fases 2 e 3 | 2,24 M |
| Fase 4 | 0,38 M |
| Fase 5 | 1,56 M |
| Fase 6, revisão | 0,98 M |
| Fase 6, correções | 0,46 M |
| **Total** | **cerca de 6,59 M** (soma das linhas acima; não inclui o orquestrador, a Fase 7 nem a Fase 8) |

Limites que o PC impôs: com 6 a 8 GB de memória livre (Chrome, Discord e outras sessões abertas), os builds Gradle foram limitados a 3 ao mesmo tempo, com `-Xmx1536m` e Kotlin dentro do daemon. Os worktrees automáticos dos workflows partiam do `main` antigo (sem as mudanças sem commit), então os prints "antes" foram refeitos e todos os worktrees seguintes foram criados à mão a partir de `melhorias-claude`, numa pasta sem acento (já apagada).

## 13. O que foi instalado na Fase 7 e como usar

**Seis agentes** em `.claude/agents/`, cada um com uma linha extra sobre este projeto:

| agente | modelo | para quê |
|---|---|---|
| explorador | haiku | ler, buscar, rodar build, lint e testes, medir e tirar prints; nunca altera código |
| planejador | claude-fable-5-2 | planejar em lotes antes de qualquer código; só escreve em `.auditoria/` |
| auditor-ux | claude-fable-5-2 | analisar prints e código de telas e apontar problemas visuais e de UX |
| confirmador | sonnet | reproduzir um bug com teste antes de alguém corrigir |
| executor | sonnet | implementar um lote, corrigir bug confirmado, rodar build e testes, um commit por item |
| revisor | opus | revisar só o diff, com atenção redobrada em `session/` e no `AndroidManifest.xml` |

**Configuração:** `.claude/settings.json` com `"model": "sonnet"` e `"effortLevel": "medium"`, criado no fim da missão (de propósito, para não trocar o modelo da sessão que estava orquestrando). O seu `.claude/settings.local.json` (permissão de WebSearch) continua igual.

**CLAUDE.md:** nova seção "Fluxo de trabalho com agentes", com uma regra extra sobre a memória deste PC (no máximo 3 builds Gradle ao mesmo tempo, cada um em seu worktree).

Nenhum desses arquivos existia antes, então nada foi sobrescrito.

**Como usar no dia a dia.** Abra uma **sessão nova** para a configuração valer. A sessão principal (sonnet) coordena e delega. Exemplos de pedido:

- "Planeja um modo de alarme com sons que mudam a cada dia." (planejador, depois executores e revisor)
- "Corrige o bug Y: o Espere continua tocando depois da missão." (explorador roda as ferramentas, confirmador reproduz, executor corrige, revisor confere)
- "Revisa a tela Z." (explorador tira os prints, auditor-ux revisa, executor ajusta)
- "Procura onde o app lê o volume do alarme." (explorador, nunca a sessão principal)
- "Muda o texto do botão de salvar." (mudança pequena: faz direto, sem planejador)
- Problema que o Sonnet travou: escreva "ultrathink" no pedido antes de chamar um modelo maior.

Regras que valem sempre: nada pode estourar o `.auditoria/orcamento.md`; todo visual segue o `GUIA-VISUAL.md`; use `/clear` entre tarefas diferentes; várias frentes independentes rodam em paralelo (até 6 com Haiku e Sonnet, até 3 com Fable e Opus), com nome claro e aviso de começo e fim de cada uma.
