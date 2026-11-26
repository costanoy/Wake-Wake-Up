# Segurança dos dados (Play Console)

Respostas do formulário, tiradas de `site/privacidade.html` (última atualização 28 de outubro de 2026) e do `AndroidManifest.xml`. Se a política ou o manifesto mudarem, refazer este arquivo.

## 1. Coleta e compartilhamento

| Pergunta do formulário | Resposta | Por quê |
|---|---|---|
| O app coleta ou compartilha algum dos tipos de dados exigidos? | Não coleta. Ver nota sobre a rádio abaixo | Alarmes, histórico e preferências ficam no aparelho; o desenvolvedor não recebe nada |
| Os dados são criptografados em trânsito? | Sim. A busca usa HTTPS e o app só toca emissoras com endereço HTTPS (o tráfego sem criptografia é bloqueado) | `RadioBrowser.kt`: `BASE` em https e filtro `startsWith("https://")` |
| Há como pedir a exclusão dos dados? | Não se aplica: nada é guardado fora do aparelho. Apagar os dados do app ou desinstalar remove tudo | Política: "Seus alarmes e seu histórico ficam guardados no seu aparelho" |
| Anúncios, SDK de análise, rastreamento? | Não | Política: "não tem anúncios e não usa ferramentas de rastreamento" |
| Compartilha dados com terceiros? | Não, no sentido da Play (ver nota) | Política: "Não vende nem compartilha dados" |
| Dados de crianças / público-alvo | Não é voltado a crianças | Sem cadastro, sem conta |

### Nota sobre a rádio (o único tráfego de rede)
- Ao abrir a tela de rádio, o app pede ao Radio Browser (radio-browser.info) as estações mais ouvidas e envia só o código do país (ex.: BR). Se a pessoa digita uma busca, o texto digitado vai no lugar.
- Esse texto e o código do país vão a um serviço de terceiros, sem nenhuma identificação (sem conta, e-mail, ID de anúncio ou de aparelho). O serviço recebe o endereço IP, como em qualquer conexão.
- Ao tocar a rádio, o aparelho se conecta direto ao servidor da emissora, que recebe o endereço IP.
- Sem abrir a tela de rádio nem escolher rádio como som, o app não faz nenhuma conexão.

Decisão para o formulário: a busca é uma consulta anônima, feita a pedido da pessoa e só para entregar o recurso. Se a Play Console exigir classificar, declarar como a tabela abaixo. Se quiser a leitura mais conservadora, marcar "Pesquisa de apps/ações no app" como **compartilhado**, finalidade "Funcionalidade do app", **não opcional para o recurso**, e não vinculado à identidade.

| Tipo de dado (se declarado) | Coletado | Compartilhado | Finalidade | Opcional | Vinculado à pessoa |
|---|---|---|---|---|---|
| Histórico de pesquisa no app (texto da busca de rádio) | Não (não chega ao desenvolvedor) | Sim, com o serviço Radio Browser (conservador) | Funcionalidade do app | Sim: só se a pessoa usar a rádio | Não |
| Informações aproximadas de país (código do país do aparelho) | Não | Sim, com o Radio Browser (conservador) | Funcionalidade do app | Sim | Não |

## 2. Permissões do manifesto e justificativa

| Permissão | Para que serve no app |
|---|---|
| `INTERNET` | Buscar e tocar rádios (Radio Browser e servidor da emissora). Só no recurso de rádio |
| `SCHEDULE_EXACT_ALARM` (até Android 12L, `maxSdkVersion=32`) | Agendar o alarme no minuto certo no Android 12 e 12L, onde é um acesso que a pessoa pode revogar |
| `USE_EXACT_ALARM` | Alarme exato no Android 13 ou superior, por ser app de despertador (ver declaração 3.1) |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar os alarmes depois que o celular reinicia, muda o fuso ou o app é atualizado |
| `WAKE_LOCK` | Manter o aparelho acordado enquanto o alarme toca, até a missão ser cumprida |
| `POST_NOTIFICATIONS` | Mostrar o alarme tocando, o Espere ativo e o aviso "Já acordou?" |
| `FOREGROUND_SERVICE` | Serviço que mantém o alarme tocando |
| `FOREGROUND_SERVICE_SYSTEM_EXEMPTED` | Tipo `systemExempted` do serviço, reservado a apps de alarme (ver declaração 3.3) |
| `USE_FULL_SCREEN_INTENT` | Abrir o alarme e a missão por cima da tela de bloqueio (ver declaração 3.2) |
| `VIBRATE` | Vibrar junto com o toque |

Não pede: contatos, localização, câmera, microfone, armazenamento, telefone, `QUERY_ALL_PACKAGES`. O app não vê quais outros apps a pessoa usa; o "Já acordou?" só confere se a tela está ligada e desbloqueada, no aparelho.

## 3. Declarações que a Play pede

### 3.1 Alarme exato (`USE_EXACT_ALARM`)
- Categoria: **despertador**. A função principal do app é um alarme: tocar na hora que a pessoa marcou, com missão para desligar.
- Uso: `AlarmManager.setAlarmClock`, o mesmo que o app Relógio do sistema usa; o relógio do celular mostra o próximo toque.
- Texto sugerido: "Wake Wake Up is an alarm clock. Its core function is ringing at the exact time the user sets, so it needs USE_EXACT_ALARM. A late alarm defeats its purpose."

### 3.2 Notificação de tela cheia (`USE_FULL_SCREEN_INTENT`)
- Categoria: **alarme**. Quando o alarme dispara com o celular bloqueado, a notificação abre a tela de toque (`RingActivity`, `showWhenLocked` e `turnScreenOn`), onde a pessoa começa a missão.
- Só dispara no horário de um alarme que a própria pessoa criou. Nenhuma outra notificação usa tela cheia.
- Texto sugerido: "The app is an alarm clock. The full-screen intent opens the ringing screen over the lock screen when a user-set alarm fires, so the user can complete the mission and turn it off."

### 3.3 Serviço em primeiro plano `systemExempted`
- Serviço: `AlarmRingService`, com `foregroundServiceType="systemExempted"` e a permissão `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, o tipo que o Android reserva para apps de alarme com `USE_EXACT_ALARM`.
- Uso: manter o som, a vibração e a notificação do alarme vivos até a missão ser cumprida, mesmo com o app em segundo plano ou a tela apagada.
- Texto sugerido: "An alarm clock app. The foreground service keeps the alarm sound and vibration running after the exact alarm fires, until the user completes the mission. It runs only while an alarm is ringing."
- Vídeo de demonstração (a Play pode pedir): gravar um alarme tocando com a tela bloqueada, a missão e o desligar.

## 4. Pendências para o Vinicius
- Conferir se o link da política (`https://wakewakeup.cyberhat.com.br/privacidade.html`) está no ar e igual ao arquivo `site/privacidade.html`.
- A política cita "O app não pede para ficar fora da otimização de bateria" e abre as configurações de bateria: não é uma permissão do manifesto (não há `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), então nada a declarar.
