<p align="center">
  <a href="https://wakewakeup.cyberhat.com.br"><img src="site/og-image.png" alt="Wake Wake Up: acorde de verdade" width="720"></a>
</p>

<p align="center">
  <a href="https://wakewakeup.cyberhat.com.br"><b>Baixar no site</b></a> · Android 8 ou superior
</p>

# Wake Wake Up

Despertador para Android feito para quem dorme pesado demais para a soneca funcionar: o alarme só desliga depois que você cumpre uma missão.

## Como funciona

1. **Toca até você levantar.** O volume pode subir do baixo ao máximo em 60 segundos. No lugar da soneca existe o *Espere*: uma pausa de 5 minutos, sem limite de usos.
2. **Só desliga com a missão.** Resolva contas de matemática (3 dificuldades, de 1 a 10 contas) ou digite uma frase.
3. **Bom dia.** Missão cumprida, alarme desligado, e o app mostra quanto tempo você levou para acordar de verdade.

E também:

- **Sons e rádio**: sons do próprio app, os toques do celular, uma música do aparelho ou uma rádio da internet (busca pelo [Radio Browser](https://www.radio-browser.info/)). Com rádio, o alarme comum assume se a estação não tocar ou se você não reagir em 3 minutos.
- **Estatísticas**: o tempo entre o primeiro toque e a missão cumprida, nos últimos 7 dias.
- Visual de rádio-relógio dos anos 70, com dígitos flip, em português e inglês.

## Como foi feito

| Parte | Tecnologia |
|---|---|
| Interface | Kotlin + Jetpack Compose + Material 3 + Navigation Compose |
| Arquitetura | MVVM com ViewModels e um `AppContainer` simples para as dependências |
| Dados | Room (alarmes e histórico de despertares) + Coroutines |
| Alarme | `AlarmManager.setAlarmClock` (horário exato), serviço em primeiro plano para tocar e reagendamento quando o aparelho reinicia |
| Testes | JUnit + Robolectric + testes de interface do Compose, rodando na JVM |

O código fica em `app/src/main/java/com/wakewakeup/`: `data/` (banco e rádios), `session/` (agendar, tocar e gerar as missões) e `ui/` (as telas). O site de download está em `site/`.

## Rodando

Abra a pasta no [Android Studio](https://developer.android.com/studio) e rode o módulo `app` num aparelho ou emulador com Android 8 ou superior. Os testes ficam em `app/src/test/` e rodam na JVM, sem emulador (no Android Studio: botão direito na pasta `test` e *Run Tests*).

---

Feito por [Vinícius Costa Magalhães](https://portfolio.cyberhat.com.br) · [Cyberhat](https://cyberhat.com.br)
