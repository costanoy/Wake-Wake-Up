# Revisão de código (Fase 6): diff dos lotes 1 a 9

Base `006d028`, HEAD `e737427`. Lido só o diff de `app/src/main`, `app/build.gradle.kts`, `site`, `ferramentas`, `README.md` e `app/src/test`.

**Resumo:** 12 problemas: 1 alto, 3 médios e 8 baixos. Nenhum é crítico.

O que foi conferido e está em ordem:

- **Testes:** nenhum foi apagado nem enfraquecido. As asserções que mudaram seguem o comportamento novo pedido no plano: `waits` igual a 1 no LOG-10, desfazer o pulo até o instante do toque (LOG-11) e `phrase_wrong_here`.
- **Orçamento:** dentro do limite. APK com 4.134.361 B (limite 4.269.850 B), 4 fontes woff2 e 22,4 KB de JavaScript no site.
- **Segurança:**
  - O token do pulo é validado antes do `cancelOffer`.
  - O `RECEIVER_NOT_EXPORTED` está certo.
  - O único componente exportado novo é o filtro do `BootReceiver`, com ações protegidas do sistema.
  - As `PendingIntent` continuam `FLAG_IMMUTABLE`.
- **Direct Boot:** nenhum caminho lê Room ou SharedPreferences de credencial antes do desbloqueio sem guarda.

| ID | Gravidade | Onde | Problema |
|----|-----------|------|----------|
| REV-01 | alta | `session/BootReceiver.kt`, `session/AlarmRingService.kt` | Alarme de uma vez que tocou com o celular bloqueado depois de um reinício é rearmado para o dia seguinte |
| REV-02 | média | `session/BootReceiver.kt` | `switchOffMissedOneTimeAlarms` roda antes do `rescheduleAll()` sem proteção: uma exceção nele deixa o celular reiniciado sem nenhum alarme armado |
| REV-03 | média | `session/PreAlarmCheck.kt` | `waitUntilInUse` pode chamar `resume` duas vezes (`IllegalStateException`, o processo cai) |
| REV-04 | média | `ui/mission/MissionScreen.kt` | Nos últimos 20 s, o texto em `liveRegion` muda a cada segundo e o TalkBack anuncia "Faltam N s" sem parar |
| REV-05 | baixa | `session/RingWatchdog.kt` | `moveSharedPreferencesFrom` pode sobrescrever a sessão gravada no armazenamento do aparelho com o arquivo velho da 1.1 |
| REV-06 | baixa | `session/AlarmRingService.kt` | O volume original do alarme só fica em memória: se o processo morrer no toque, o piso de 60% vira o volume permanente |
| REV-07 | baixa | `session/AlarmReceiver.kt` | Toque sem leitura do banco (bloqueado ou banco com erro) não arma o próximo toque de alarme que se repete |
| REV-08 | baixa | `MainActivity.kt` | `Handler.postDelayed` de 125 s segura a `MainActivity` (vazamento) |
| REV-09 | baixa | `session/BootReceiver.kt` | O caminho bloqueado faz leitura e escrita de preferências e AlarmManager na thread principal, sem `goAsync` |
| REV-10 | baixa | `site/index.html`, `site/en/index.html` | "Fechou o app?" promete mais do que o vigia garante |
| REV-11 | baixa | `site/site-config.js` | O site anuncia "v1.2 · 4 MB", mas `site/wake-wake-up.apk` ainda é o 1.0 de debug |
| REV-12 | baixa | `site/index.html`, `site/en/index.html` | "Acordou antes do alarme?" ainda diz "desativar" / "turn off", e o app agora diz "pular" / "skip" |

---

## REV-01 (alta): alarme de uma vez toca de novo no dia seguinte depois de um toque com o celular bloqueado

**Arquivos:** `app/src/main/java/com/wakewakeup/session/BootReceiver.kt` (`switchOffMissedOneTimeAlarms`) e `app/src/main/java/com/wakewakeup/session/AlarmRingService.kt` (`cleanUpAfterRing`).

**O que acontece:** antes do desbloqueio, `cleanUpAfterRing` sai sem chamar `setEnabled(alarm, false)` (Lote 1, item 5). O alarme de uma vez continua ligado no banco. Depois do desbloqueio, o `BOOT_COMPLETED` deveria desligá-lo, mas `switchOffMissedOneTimeAlarms` pula a entrada em dois casos:

- `id == ringing || id == interrupted`: a sessão ainda está aberta, por exemplo no toque, na missão ou no Bom dia;
- `armedAt >= now - MISSED_AFTER_MS`: o desbloqueio veio menos de 2 min depois do horário.

Em seguida, `rescheduleAll()` arma esse alarme (ligado, sem dias) para amanhã, no mesmo horário.

**Cenário:**

1. O celular reinicia de madrugada, por exemplo numa atualização automática.
2. O alarme de uma vez das 07:00 toca na tela bloqueada, e o usuário resolve a missão sem desbloquear.
3. Ainda no Bom dia, o usuário desbloqueia o celular. O mesmo vale para quem toca em Concluir antes de 07:02, porque a `MainActivity` pede o desbloqueio.
4. No dia seguinte, às 07:00, o alarme toca de novo.

**Correção:** em `switchOffMissedOneTimeAlarms`:

- trocar a condição para `entry.armedAt <= now`. Na hora do boot, o AlarmManager não tem nenhum toque nosso pendente, então não há toque em andamento a poupar;
- tirar a exclusão `id == ringing || id == interrupted`. Desligar no banco não afeta a sessão, que usa `currentAlarm`, e é o que `cleanUpAfterRing` teria feito se estivesse desbloqueado.

Alternativa: no `AlarmRingService`, guardar `pendingCleanUp = alarm` quando bloqueado e rodar `cleanUpAfterRing` ao receber `Intent.ACTION_USER_UNLOCKED` (receiver registrado no serviço).

Teste novo em `BootRearmTest`:

1. Usuário bloqueado.
2. Entregar o toque de um alarme de uma vez.
3. Desbloquear (`setUserUnlocked(true)`) com a sessão ainda aberta.
4. Entregar `BOOT_COMPLETED`.
5. Conferir que o alarme está desligado e que nada está armado para ele.

## REV-02 (média): falha em `switchOffMissedOneTimeAlarms` impede o rearme depois do reinício

**Arquivo:** `app/src/main/java/com/wakewakeup/session/BootReceiver.kt`

**O que acontece:** antes dos lotes, `rescheduleAll()` era a primeira coisa do `BOOT_COMPLETED`. Agora `switchOffMissedOneTimeAlarms` roda antes dele e não está protegido. Uma exceção em `repository.getById`, ou em `setEnabled` (que chama `onChanged`, depois `preAlarm.reschedule`, depois o AlarmManager), sai do `launch` e pula o `rescheduleAll()`. Resultado: o celular reiniciado fica sem nenhum alarme até o app ser aberto.

**Correção:**

- envolver a chamada em `try { switchOffMissedOneTimeAlarms(context, app) } catch (e: Exception) { currentCoroutineContext().ensureActive(); Log.w(...) }`;
- dentro do laço, proteger cada alarme também, para um com problema não impedir os outros.

## REV-03 (média): `resume` duplo em `waitUntilInUse`

**Arquivo:** `app/src/main/java/com/wakewakeup/session/PreAlarmCheck.kt` (`waitUntilInUse`)

**O que acontece:** depois do `registerReceiver`, a thread de IO faz `if (!power.isInteractive || !keyguard.isKeyguardLocked) continuation.resume(Unit)` sem conferir `isActive`.

Se o `ACTION_USER_PRESENT` chegar na thread principal entre o registro e essa checagem, o receiver retoma primeiro, a condição já é verdadeira e a segunda chamada lança `IllegalStateException: Already resumed`. A ordem inversa também pode acontecer: a thread de IO retoma, e o receiver passou do `isActive` antes. Nos dois casos a exceção sai sem tratamento no `CoroutineScope(Dispatchers.IO)` do `PreAlarmReceiver`, ou no `onReceive` da thread principal, e o processo cai.

**Correção:** trocar a continuação por um `CompletableDeferred<Unit>`. `complete()` pode ser chamado mais de uma vez sem erro:

```kotlin
val done = CompletableDeferred<Unit>()
// no receiver:
done.complete(Unit)
// depois do registro:
if (...) done.complete(Unit)
// para esperar:
withTimeoutOrNull(unlockWaitMs) { done.await() }
```

O `unregisterReceiver` continua no `finally`.

## REV-04 (média): TalkBack anuncia a contagem a cada segundo

**Arquivo:** `app/src/main/java/com/wakewakeup/ui/mission/MissionScreen.kt`

**O que acontece:**

- Em `MathBody`, a `message` tem `.semantics { liveRegion = LiveRegionMode.Polite }`.
- Em `PhraseBody`, o texto de apoio também é `liveRegion`.

Com `secondsLeft <= TIME_LOW_SECONDS`, os dois passam a mostrar `mission_time_low` com o número de segundos, que muda a cada segundo. Por 20 s, o TalkBack anuncia "Faltam 20 s…", "Faltam 19 s…" e assim por diante, por cima da digitação.

**Correção:** anunciar só a entrada no estado de tempo curto. Duas formas:

- usar um texto fixo na `liveRegion`, por exemplo uma string nova "Menos de 20 s. Depois o alarme volta a tocar.";
- ou tirar a `liveRegion` desse Text e anunciar uma vez com `LaunchedEffect(low) { if (low) view.announceForAccessibility(...) }`.

O número que muda pode continuar visível no `VfdText` do topo, que não é `liveRegion`. Teste de UI: com 15 s, o nó `liveRegion` não contém o número.

## REV-05 (baixa): mover as preferências do vigia pode apagar uma sessão gravada antes do desbloqueio

**Arquivo:** `app/src/main/java/com/wakewakeup/session/RingWatchdog.kt` (`prefs`)

**O que acontece:**

1. Na 1.1, todo toque deixava `ring_session.xml` no armazenamento de credencial. `clear().commit()` grava um arquivo vazio, mas não o apaga.
2. O usuário atualiza para a 1.2.
3. O primeiro toque acontece com o celular reiniciado e bloqueado. A sessão é gravada no armazenamento do aparelho.
4. No primeiro `prefs()` com o celular desbloqueado, `moveSharedPreferencesFrom` move o arquivo velho por cima do novo e limpa o cache.
5. A partir daí, `RingWatchdog.saved()` devolve `null`, por exemplo no `BOOT_COMPLETED` (`interrupted`). Se o processo morrer antes do próximo `save`, o toque não volta.

Acontece só uma vez por aparelho, logo depois da atualização.

**Correção:** só mover se o arquivo de destino ainda não existe. Por exemplo:

```kotlin
if (!store.getSharedPreferencesPath(PREFS).exists()) {
    runCatching { store.moveSharedPreferencesFrom(context, PREFS) }
}
```

`getSharedPreferencesPath` é API 24+, e o minSdk é 26. Se o destino já existir, apagar a origem com `context.deleteSharedPreferences(PREFS)`.

## REV-06 (baixa): volume original perdido se o processo morrer durante o toque

**Arquivo:** `app/src/main/java/com/wakewakeup/session/AlarmRingService.kt` (`ensureAudible` e `restoreAlarmVolume`)

**O que acontece:**

1. `originalAlarmVolume` é um campo do serviço.
2. O processo morre no toque e o vigia retoma.
3. O serviço novo vê o volume já no piso de 60%, então nunca guarda o original.
4. O volume de alarme do usuário fica em 60% para sempre.

**Correção:**

- gravar o volume original junto da sessão, no `RingWatchdog` (chave `orig_volume`, armazenamento do aparelho) ou em um campo de `RingSession`;
- ao retomar, ler esse valor e devolver em `finishSession` e em `stopSelfSafely`;
- apagar o valor no `RingWatchdog.clear`.

## REV-07 (baixa): toque sem leitura do banco não arma o próximo toque

**Arquivo:** `app/src/main/java/com/wakewakeup/session/AlarmReceiver.kt`

**O que acontece:** com `readFailed`, o `alarm` fica `null` e `armNext` não roda. O toque que estava no AlarmManager já foi consumido. Se o celular ficar bloqueado até o dia seguinte, ou se o banco estiver com erro (desbloqueado), o alarme que se repete não toca no outro dia, mesmo com a cópia em `ArmedRings` disponível.

**Correção:** com `readFailed && alarm == null`, procurar `ArmedRings.all(context).firstOrNull { it.alarm.id == alarmId }`. Se a entrada tiver dias, chamar `AlarmScheduler(context).schedule(entry.alarm)` em `runCatching`. `nextRingMillis` a partir de agora dá o próximo dia.

## REV-08 (baixa): `MainActivity` presa por 125 s

**Arquivo:** `app/src/main/java/com/wakewakeup/MainActivity.kt` (`rearmAlarmsOnce`)

**O que acontece:** a lambda do `postDelayed` usa `app`, que é um getter da activity (`application as WakeWakeUpApplication`). Ela captura a `MainActivity` e a mantém viva por 2 min depois de uma rotação ou de fechar a tela.

**Correção:** capturar antes:

```kotlin
val container = app.container
Handler(Looper.getMainLooper()).postDelayed(
    { container.appScope.launch { container.alarmRepository.rescheduleAll() } },
    SECOND_PASS_MS,
)
```

## REV-09 (baixa): disco na thread principal no `BootReceiver` bloqueado

**Arquivo:** `app/src/main/java/com/wakewakeup/session/BootReceiver.kt` (`rearmFromArmedRings`)

**O que acontece:** o caminho bloqueado roda tudo dentro do `onReceive`, na thread principal:

- `ArmedRings.all` (carga síncrona do XML);
- `AlarmScheduler.schedule`, que inclui `ArmedRings.put`;
- `RingWatchdog.resumeIfInterrupted`, que lê preferências.

Isso contraria a nota de orçamento do Lote 2 ("sempre fora da thread principal"). É pouco hoje, mas cresce com o número de alarmes.

**Correção:** como no caminho desbloqueado, usar:

```kotlin
val pending = goAsync()
CoroutineScope(Dispatchers.IO).launch {
    try {
        rearmFromArmedRings(...)
    } finally {
        pending.finish()
    }
}
```

## REV-10 (baixa): "Fechou o app?" promete demais

**Arquivos:** `site/index.html` e `site/en/index.html` (item novo em `.extras`)

**O que acontece:** o texto diz "Fechar o app no meio do toque não desliga o alarme: ele volta a tocar em até 3 minutos". Mas "Forçar parada", e a limpeza agressiva de alguns fabricantes ao deslizar o app para fora, cancelam também os alarmes do AlarmManager, inclusive o do vigia. Nesse caso o toque não volta, e os próximos alarmes também somem até o app ser aberto. Deslizar o app nos recentes nem chega a parar o serviço, então "volta" também não descreve bem esse caso.

**Correção:** dizer só o que é garantido. Sugestão:

- PT: "Se o sistema fechar o app no meio do toque, o alarme volta a tocar em até 3 minutos. Só a missão encerra."
- EN: "If the system closes the app mid-ring, the alarm rings again within 3 minutes. Only the mission ends it."

Manter igual nas duas línguas.

## REV-11 (baixa): site anuncia uma versão que o arquivo ainda não é

**Arquivos:** `site/site-config.js` e `site/index.html` / `site/en/index.html` (`data-apk-meta` com "v1.2 · 4 MB · Android 8+")

**O que acontece:** o `site/wake-wake-up.apk` atual é o 1.0 de debug. O próprio `ferramentas/conferir-apk-site.sh` falha contra ele. Se `site/` for publicado antes da troca do APK, o botão promete a 1.2 e entrega a 1.0 de debug.

**Correção:**

- no `site/README.md`, na seção "Atualizar o APK", deixar explícito: "não publique `site/` enquanto o script não passar";
- opcional: fazer o `conferir-apk-site.sh` também comparar `apkVersion` de `site-config.js` com o `versionName` do APK e falhar se forem diferentes.

## REV-12 (baixa): vocabulário do site diferente do app

**Arquivos:** `site/index.html` e `site/en/index.html` (`.extras`, "Acordou antes do alarme?" / item equivalente em inglês)

**O que acontece:** o texto continua "pergunta se quer desativar esse alarme só desta vez", e em inglês "turn off". Depois do UX2-11 (Lote 7), o app diz "Já acordou?… Quer pular só desta vez?" e o botão "Pular".

**Correção:**

- PT: "Se você já estiver usando o celular até 1 hora antes de um alarme da manhã, o app pergunta se quer pular esse alarme só desta vez. Para confirmar, resolva uma conta rápida."
- EN: trocar "turn off" por "skip".
