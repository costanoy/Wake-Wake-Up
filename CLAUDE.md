# Wake Wake Up

Despertador para Android que só desliga depois de uma missão (contas ou frase). Visual de rádio-relógio dos anos 70 com dígitos flip, em português e inglês. Tem também um site estático de download em `site/`. Detalhes de produto no `README.md`.

## Stack

- Kotlin 2.2 + Jetpack Compose (BOM 2026.06) + Material 3 + Navigation Compose 2.9; MVVM com ViewModels e um `AppContainer` simples.
- Room 2.8 (banco v6, schemas em `app/schemas/`, migrações escritas à mão) + Coroutines.
- Alarme: `AlarmManager.setAlarmClock`, serviço em primeiro plano `AlarmRingService`, `RingWatchdog` a cada 3 min, `BootReceiver`.
- AGP 9.3, Gradle 9.5 (wrapper), compileSdk 37, targetSdk 36, minSdk 26, JDK 17.
- Testes: JUnit + Robolectric + Compose UI na JVM (`app/src/test`). Sem emulador neste PC.
- Site: HTML/CSS/JS puro, sem build (`site/`, PT na raiz e EN em `site/en/`).

## Rodar, compilar e testar (Git Bash)

O caminho tem acento ("Área de Trabalho") e os workers de teste do Gradle quebram com isso. Use sempre o script, que monta a pasta numa letra de unidade com `subst` quando precisa:

```bash
bash ferramentas/gradle.sh :app:assembleDebug
bash ferramentas/gradle.sh :app:testDebugUnitTest                 # suíte inteira (~270 testes)
bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "*AlarmTimingTest*"
bash ferramentas/gradle.sh :app:lintDebug
bash ferramentas/gradle.sh :app:assembleRelease                   # sem keystore.properties sai sem assinatura
WWU_SCREENSHOTS=1 bash ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"
```

- Prints saem em `app/build/screenshots/` com prefixos `pt`, `en`, `pequeno` (celular pequeno) e `fonte-grande` (1.3x).
- Dois Gradle na mesma pasta ao mesmo tempo brigam por trava: trabalho paralelo vai em worktrees separados.
- Worktrees: crie a partir de `melhorias-claude` (ou da branch atual), de preferência fora do OneDrive e num caminho sem acento, ex.: `git worktree add C:/Users/vinic/wwu-wt/<nome> -b <branch> HEAD`.
- Site: `python -m http.server 8765 --directory site` e abrir `http://localhost:8765/`. O `lang.js` redireciona pelo idioma do navegador (força com `localStorage wwu-lang=en|pt`).

## Estrutura (`app/src/main/java/com/wakewakeup/`)

- `data/`: entidades Room (`AlarmEntity`, `WakeHistoryEntity`), `AppDatabase`, `AlarmRepository`, `RadioBrowser` (busca de rádios).
- `session/`: agendar (`AlarmScheduler`, `nextRingMillis`), tocar (`AlarmRingService`, `RingActivity`, `RingWatchdog`), missões (`MissionGenerator`), "Já acordou?" (`PreAlarmPlanner`, `PreAlarmCheck`), receivers.
- `ui/`: uma pasta por tela, `components/` (Hardware, FlipDigit, Keypads, DayKeys, Labels), `theme/`, `navigation/` (`Destinations`, `WwuNavGraph`).
- `res/raw/`: 7 sons de alarme. Os nomes NÃO podem mudar: alarmes salvos guardam o som pelo nome do recurso.
- `res/values/strings.xml` (EN) e `res/values-pt/strings.xml` (PT): toda string nova entra nos dois (o `StringsTest` confere).

## Telas

Permissões, Lista de alarmes, Editar alarme, Tarefa, Som, Rádio, Estatísticas, Confirmar pulo (rotas do `WwuNavGraph`) e, em `RingActivity`, Toque (com Espere), Missão e Bom dia. Mapa completo com estados e as 93 funções em `.auditoria/mapa.md`; status de cada função em `.auditoria/funcoes.md`.

## Convenções

- Tema escuro fixo, paleta e tipografia em `ui/theme/` (Barlow Condensed + Instrument Sans). Componentes de "hardware" em `ui/components/Hardware.kt`.
- Textos de interface curtos e diretos, sem cara de marketing; PT-BR é o idioma principal.
- Comentários e mensagens de commit do código original em inglês; commits desta revisão em português.
- Sem dependências novas de runtime sem razão forte. Orçamento em `.auditoria/orcamento.md`.
- Áudio: manifesto em `assets/audio/audio_manifest.md` (o app não usa música).
- Nunca versionar `local.properties`, `keystore.properties`, `*.jks`, `.env`.

## Fluxo de trabalho com agentes

A sessão principal (Sonnet) coordena e delega. Siga estas regras:

- **Recurso novo ou mudança em mais de 2 arquivos:** planejador → executores em paralelo (um por lote, até 6) → revisor.
- **Mudança pequena e óbvia (1 ou 2 arquivos):** faça direto, sem planejador.
- **Bug relatado:** explorador roda as ferramentas → confirmador reproduz → executor corrige → revisor confere.
- **Tela ou modelo 3D criado ou alterado:** explorador tira os prints ou renderiza a prévia → auditor-ux revisa → executor ajusta.
- **Leitura ampla ou busca no projeto:** sempre o explorador, nunca a sessão principal.
- **Problema muito difícil em que o Sonnet travou:** use "ultrathink" no pedido antes de chamar um modelo maior.
- Use /clear entre tarefas diferentes para o contexto não crescer à toa.
- **Várias frentes ao mesmo tempo:** o Vinicius gosta de ver várias frentes trabalhando simultaneamente e de acompanhar cada uma. Divida todo trabalho independente em frentes paralelas (até 6 com Haiku e Sonnet, até 3 com Fable e Opus), dê um nome claro a cada frente, mantenha a lista de tarefas atualizada e avise em uma linha quando cada frente começar e terminar.
- Nada pode estourar o orçamento em .auditoria/orcamento.md. O projeto precisa continuar leve.
- Todo visual segue o GUIA-VISUAL.md, se existir.
- Skills do projeto ficam em .claude/skills/, com a lista em .auditoria/skills.md. Use as que valem para cada tarefa, principalmente a de design, para nada ficar com cara de IA. Ao procurar skills novas, não se limite às oficiais da Anthropic, mas leia o SKILL.md e todos os scripts antes de instalar.
- Assets, quando necessários: ElevenLabs gera tudo que não é música, Suno gera tudo que é música, Blender faz tudo que é 3D e Canva faz toda a arte 2D (logo, ícones, banners, arte da loja e divulgação).
- Não sugerir a estética Graffiti Core.
- Mantenha o ROADMAP.md atualizado quando um recurso for concluído.
- **Neste PC (memória apertada):** no máximo 3 builds Gradle ao mesmo tempo, cada um em seu worktree, com `-Dorg.gradle.jvmargs="-Xmx1536m -Dfile.encoding=UTF-8" -Pkotlin.compiler.execution.strategy=in-process`. Worktrees automáticos de workflow partem do `main`: crie os worktrees à mão a partir da branch atual.
