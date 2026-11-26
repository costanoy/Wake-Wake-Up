# Painel de progresso

Projeto: Wake Wake Up (app Android + site). Branch: `melhorias-claude`. TIPO: PRONTO. MODO: AUTOMÁTICO. LIBERDADE: ALTA.

| Fase | Frente | Modelo | Status | Resultado |
|---|---|---|---|---|
| 0 | Preparação (orquestrador) | opus | concluída | branch `melhorias-claude`, commit de base `e57a996`, Gradle wrapper adicionado, `orcamento.md` criado |
| 1 | Mapa: estrutura, telas e fluxos | haiku | concluída | 11 telas, 93 funções (80 app, 13 site), `mapa.md` |
| 1 | Ferramentas: build, lint e testes | haiku | concluída | lint 1 erro e 50 warnings; testes não rodavam no caminho com acento (resolvido com `ferramentas/gradle.sh`) |
| 1 | Inventário: arquivos, duplicação, TODOs | haiku | concluída | 0 TODOs, 2 itens mortos, constantes duplicadas, strings PT/EN completas |
| 1 | Medidor: APK, assets e site (antes) | haiku | concluída | APK release 4,07 MB; Lighthouse 97/100/100/100; 6 suspeitas de performance |
| 1 | Testador: todas as funções | sonnet | concluída | 81 testes novos; 79 FUNCIONA, 2 QUEBRADO, 13 NÃO TESTÁVEL (JS do site) |
| 1 | Prints: app e site (antes) | haiku | concluída | 13 do site; os do app foram refeitos pelo orquestrador no estado atual (64; o passo do editor falha) |
| 1 | Skills: buscar, avaliar e instalar | sonnet | concluída | 10 skills (impeccable como design principal), commit `28157c8` |
| 1 | Linha de base da suíte (orquestrador) | opus | concluída | 294 testes, 3 falhas (2 testes novos contra tela antiga, `SkipConfirmUiTest`) |
| 2 | Manutenção de testes e prints do editor | sonnet | concluída | suíte 294/0 falhas; 84 prints (21 telas x 4 variantes); SkipConfirmUiTest era corrida do teste |
| 2 | Auditor de Lógica | fable | concluída | 13 itens, 0 críticos, 2 altos |
| 2 | Auditor UI/UX 2: toque, missão, bom dia, stats, pulo | fable | concluída | 13 itens, 0 críticos, 3 altos |
| 2 | Auditor Técnico | opus | concluída | 12 itens, 0 críticos, 3 altos |
| 2 | Auditor UI/UX 1: permissões, lista, editor, tarefa, som, rádio | fable | concluída | 13 itens, 0 críticos, 1 alto |
| 2 | Auditor do Site | fable | concluída | 13 itens, 1 crítico (APK de debug 1.0 no site), 3 altos |
| 3 | Confirmador: lógica | sonnet | concluída | 13 confirmados, 0 descartados; testes em AppLogicBugTest e ServiceLogicBugTest |
| 3 | Confirmador: técnica | sonnet | concluída | 12 confirmados, 0 descartados; 10 classes *BugTest |
| 3 | Confirmador: UI/UX 1 | sonnet | concluída | 13 confirmados, 0 descartados |
| 3 | Confirmador: UI/UX 2 | sonnet | concluída | 13 confirmados, 0 descartados (ressalva no UX2-09) |
| 3 | Confirmador: site | sonnet | concluída | 13 confirmados, 0 descartados (SITE-01/02/06 dependem do Vinicius) |
| 3 | Junção e suíte (orquestrador) | opus | concluída | 319 testes, 0 falhas, 55 ignorados (testes de bug aguardando correção) |
| 4 | Planejador: MELHORIAS.md e GUIA-VISUAL.md | fable | em andamento | |
