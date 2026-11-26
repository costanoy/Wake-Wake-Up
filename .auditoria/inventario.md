# Inventário – Wake Wake Up

Gerado pelo agente Inventário (somente leitura). Ignorados: build/, .gradle/, .kotlin/, .idea/, .git/, portfolio-preview/.

## 1. Maiores arquivos (top 20, fora das pastas ignoradas)

| # | Bytes | Arquivo | Situação no git |
|---|------:|---------|-----------------|
| 1 | 26.571.428 | wake-wake-up-site.zip | ignorado (.gitignore) |
| 2 | 22.556.142 | site/wake-wake-up.apk | ignorado (.gitignore) |
| 3 | 4.969.843 | wake-wake-up-video.mp4 (raiz) | ignorado (.gitignore) |
| 4 | 4.969.843 | site/wake-wake-up-video.mp4 | versionado |
| 5-11 | 300.717 cada | app/src/main/res/raw/*.mp3 (7 arquivos: telefone_de_disco, sirene_1/2, passaros, campainha_mecanica, bipe_digital_1/2) | versionados |
| 12 | 185.102 | site/og-image.png | versionado |
| 13 | 117.515 | app/src/main/res/drawable-nodpi/ic_launcher_foreground_art.png | versionado |
| 14 | 109.428 | res/font/barlow_condensed_semibold.ttf | versionado |
| 15 | 103.360 | res/font/barlow_condensed_medium.ttf | versionado |
| 16 | 68.904 | res/font/instrument_sans_medium.ttf | versionado |
| 17 | 68.872 | res/font/instrument_sans_semibold.ttf | versionado |
| 18 | 68.648 | res/font/instrument_sans_regular.ttf | versionado |
| 19 | 43.764 | gradle/wrapper/gradle-wrapper.jar | versionado |
| 20 | 41.010 | site/assets/video-poster.jpg | versionado |

Observações:
- Os três maiores (zip, APK e vídeo da raiz) são cópias/artefatos e não estão no git. Ocupam ~54 MB no disco.
- Os mp4 da raiz e de site/ têm o mesmo tamanho: provável duplicata.
- Maior arquivo de código: app/src/main/java/com/wakewakeup/ui/components/Hardware.kt, 975 linhas (sem contar build/).

## 2. Código duplicado ou quase duplicado

Verificação heurística (nomes de funções/constantes e padrões repetidos). Não é uma análise semântica completa de clones.
- Constante duplicada: `MAX_PHRASE_LENGTH = 80` em app/src/main/java/com/wakewakeup/session/AlarmRingService.kt:648 e `MAX_PHRASE_CHARS = 80` em app/src/main/java/com/wakewakeup/ui/mission/MissionScreen.kt:87. Mesmo limite, dois nomes. Convém centralizar.
- Nomes de composables privados parecidos em telas diferentes, sem duplicação de corpo confirmada: `LeaveDialog` (edit/EditAlarmScreen.kt:293), `ListHeader` (list/AlarmListScreen.kt:213), `NextStrip` (list/AlarmListScreen.kt:271), `CardDrawer` (list/AlarmListScreen.kt:569), `EmptyState` (list/AlarmListScreen.kt:596), `PermissionPlate` (permissions/PermissionsScreen.kt:176), `PhrasePlate` e `MathPlate` (task/TaskConfigScreen.kt:221 e :131), `StationRow` (sound/RadioScreen.kt:218) e `SoundRow` (sound/SoundPickerScreen.kt:310). Vale checar se `PermissionPlate`/`MathPlate`/`PhrasePlate` repetem o mesmo cartão.
- Site: site/index.html e site/en/index.html são versões bilíngues com estrutura duplicada (esperado, por ser PT/EN). Qualquer mudança de layout precisa ser feita nos dois.
- Nomes de funções repetidos entre arquivos (`onCreate`, `onReceive`, `save`, `insert`, `update` etc.) são, em geral, overrides ou métodos de DAO/repositório, não duplicação.

## 3. Arquivos mortos

Kotlin:
- `calcPill` em app/src/main/java/com/wakewakeup/ui/theme/Shape.kt:23 (`val calcPill = RoundedCornerShape(32.dp)`) não tem nenhuma referência no app nem nos testes. Candidato a remoção.
- Demais declarações com 1 ocorrência: nenhuma além de `calcPill`. As com 2 ocorrências são quase todas definição + um uso (não são mortas).

Recursos (app/src/main/res):
- `mipmap-anydpi-v26/ic_launcher_round.xml`: nenhuma referência em código ou manifesto. O AndroidManifest.xml (linhas 24 e 26) usa `@mipmap/ic_launcher` tanto para `android:icon` quanto para `android:roundIcon`, então é morto.
- Strings: as 207 chaves de strings.xml (203 string + 4 plurals) têm uso no código.
- Sons (res/raw): os 7 mp3 são usados por BundledSounds.kt (linhas 13-19). `keep.xml` é intencional (tools:keep="@raw/*"), não é morto.
- Fontes e drawables: todos com uso no código (`ic_launcher_background`, `ic_launcher_monochrome`, `ic_notification_sun`, a fonte barlow e instrument sans).

Site:
- Referenciados pelo HTML: todos os assets de site/assets/ e os arquivos CSS/JS.
- Sem referência nos HTML (esperado, são arquivos de servidor, crawler ou documentação): `.htaccess`, `favicon.ico`, `robots.txt`, `sitemap.xml`, `README.md`.
- site/main.js: todas as funções aparecem mais de uma vez, sem candidatas óbvias.
- Fora do site: `wake-wake-up-site.zip` e `wake-wake-up-video.mp4` da raiz não são referenciados por nada (cópias ignoradas).

## 4. TODO, FIXME, HACK, XXX

Nenhum encontrado em app/src, site/ nem README.md. (A única ocorrência fora disso está em .auditoria/PROGRESSO.md, que é a própria tabela de controle da auditoria.)

## 5. Strings pt x en

- values/strings.xml: 203 `<string>` e 4 `<plurals>`.
- values-pt/strings.xml: 203 `<string>` e 4 `<plurals>`.
- Chaves faltando em pt: nenhuma. Chaves sobrando em pt: nenhuma. Os nomes dos plurals batem entre os dois idiomas.
- Nenhuma string marcada `translatable="false"`.

## 6. Arquivos de teste (192 @Test ao todo, 21 classes de teste + 3 auxiliares)

Unitários (JVM, sem UI):
- AppSetupTest.kt (7): canais de notificação, permissões declaradas, fiação dos componentes, fuso horário e atualização do app.
- StringsTest.kt (4): paridade de strings pt/en, placeholders, arrays de dias.
- data/AlarmEntityTest.kt (3): ida e volta da entidade pelo banco, dias como texto, valores de tarefa desconhecidos.
- data/AlarmRepositoryTest.kt (15): salvar/editar/ligar/desligar/excluir alarme e agendamento, pular um dia.
- data/DatabaseMigrationTest.kt (2): migrações 4->5 e 5->6 do Room.
- data/RadioBrowserTest.kt (6): busca de rádios, filtros de HTTPS, erro sem rede.
- session/AlarmReceiverTest.kt (8): disparo do receiver, alarme desligado, alarme pulado.
- session/AlarmRingServiceTest.kt (36): rampa de volume, som escolhido, fallback de som, desligar alarme único.
- session/AlarmTimingTest.kt (13): cálculo do próximo disparo por dia da semana.
- session/MissionGeneratorTest.kt (9): contas por dificuldade, frase do dia, acentos.
- session/PreAlarmCheckTest.kt (11): checagem de "celular em uso" antes do alarme.
- session/PreAlarmPlannerTest.kt (11): quando perguntar antes do alarme.

Auxiliares (sem @Test): testing/AppEnv.kt, testing/FakeRadio.kt, ui/UiTesting.kt.

Compose UI na JVM (Robolectric):
- ui/AlarmListUiTest.kt (18): lista vazia, contagem regressiva, criar e editar alarme.
- ui/ListWarningsUiTest.kt (5): avisos de permissão e de volume baixo na lista.
- ui/PermissionsUiTest.kt (7): fluxo de permissões no primeiro uso.
- ui/PortugueseUiTest.kt (5): textos em português, lista, pular dia, tela de toque, link de privacidade.
- ui/RingFlowUiTest.kt (10): fluxo completo de toque, espera, estatísticas.
- ui/ScreenshotTour.kt (7): gera os prints das telas (lista, permissões, editor, toque, estatísticas, confirmação de pular).
- ui/SkipConfirmUiTest.kt (3): desligar com soma e desfazer.
- ui/SoundAndTaskUiTest.kt (7): escolha de som, toque de música, rádio, configuração da tarefa.
- ui/StatsUiTest.kt (5): estatísticas sem dados, semana, política de privacidade.

## 7. Resumo de contagens

- Arquivos Kotlin: 77 (app/src). Linhas: 13.159 no total; 8.777 em app/src/main/java.
- Arquivos de código de teste: 24 (21 de teste + 3 auxiliares).
- Entradas do .gitignore que cobrem artefatos grandes: 3 (zip, apk, mp4 da raiz).
- Pastas e arquivos fora do git: `.auditoria/` aparece como não rastreado (não está no .gitignore).

## 8. Limitações

- Dead code e duplicação foram checados por busca de texto (grep), não por análise da árvore de sintaxe. Pode haver falso positivo ou falso negativo, sobretudo com reflexão, nomes gerados e referências por string.
- Não foi executado build nem teste (não era o escopo deste agente).
