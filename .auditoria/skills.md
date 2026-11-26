# Skills instaladas no projeto (Wake Wake Up)

Data: 2026-11-26. Instaladas em `.claude/skills/` (nada global). Nenhum script foi executado. Todos os SKILL.md e scripts foram lidos/varridos (sem envio de dados, chaves, instrucoes escondidas).

## SKILL DE DESIGN PRINCIPAL: impeccable
Criterio de "cara de IA": `impeccable/reference/craft-floor.md` (piso de qualidade, banimentos absolutos) + `critique-rubric.md` + detector. Tem referencia Android propria (`android.md`, `audit.native.md`, `adapt.native.md`). Serve para telas Compose e para o site.

## Instaladas (10)
| Skill | Origem / commit | Licenca | Para que serve | Quem usa |
|---|---|---|---|---|
| impeccable (v4.11.1) | github.com/pbakaus/impeccable @ 2fc5e14 | Apache-2.0 | Design/critica/polimento/animacao/tipografia, anti-padroes de IA, nativo Android | auditor de UI/UX, revisor visual, executores de telas, redator (clarify) |
| ux-writing | github.com/content-designer/ux-writing-skill @ 98cacde | MIT | Textos de interface: botoes, erros, vazios, onboarding, acessibilidade do texto | redator, auditor de UI/UX |
| jetpack-compose-performance | github.com/android/skills @ 7f7b217 | Apache-2.0 | Recomposicao, estabilidade, listas lazy, animacao | revisor de codigo, executores de telas |
| edge-to-edge | android/skills @ 7f7b217 | Apache-2.0 | Insets, barras do sistema, IME | executores de telas, revisor visual |
| testing-setup | android/skills @ 7f7b217 | Apache-2.0 | Estrategia de testes (Robolectric, Compose UI, screenshot) | testador |
| android-permissions-security | android/skills @ 7f7b217 | Apache-2.0 | Permissoes, componentes exportados, manifesto | revisor de codigo |
| android-intent-security | android/skills @ 7f7b217 | Apache-2.0 | Intents, PendingIntent (alarmes/notificacoes) | revisor de codigo |
| anr-audit-and-fix | android/skills @ 7f7b217 | Apache-2.0 | Riscos de ANR em receivers/services/main thread | revisor de codigo, testador |
| seo-audit | github.com/coreyhaines31/marketingskills @ 8fd2fca | MIT | SEO tecnico do site, hreflang PT/EN, titulos, CWV | auditor do site, redator |
| aso | coreyhaines31/marketingskills @ 8fd2fca | MIT | Ficha da Google Play (titulo, descricao, prints) | redator |

Observacoes:
- impeccable: o launcher (`scripts/impeccable`, `.cmd`) baixa na primeira execucao um binario do GitHub Releases do proprio repo, com checagem SHA-256 (`engine-checksums.txt`). Nao executei. Confirme antes de rodar; sem ele a skill funciona lendo PRODUCT.md/DESIGN.md. Ha tambem versao global diferente em `~/.claude/skills/impeccable`; a do projeto tem prioridade.
- impeccable `live`/`hooks` mexem em arquivos/servidor local so quando o usuario invoca; nao usar `hooks on` sem avisar.
- Removi de seo-audit/aso a pasta `evals` e, do ux-writing, videos/docs/exemplos (so SKILL.md, references, templates).
- Creditos: Apache-2.0 exige manter LICENSE/NOTICE (copiados em cada pasta). Sem creditos publicos obrigatorios.

## Recusadas / nao instaladas
- anthropics/skills frontend-design: duplica o impeccable e puxa para estetica propria; so um de design.
- nextlevelbuilder/ui-ux-pro-max-skill: tambem design geral (paletas/estilos prontos); conflitaria com o impeccable. Nao avaliada a fundo por isso.
- android/skills play-policy-insights: scripts Python que fazem scraping da Play Store e rodam orquestrador; nao instalado (rede + execucao). Fazer a checagem de politicas manualmente.
- android/skills r8-analyzer: scripts Python/protobuf, ganho pequeno aqui. navigation-3 (app usa Navigation 2.9.7), agp-9-upgrade (ja no AGP 9.3.1), adaptive, camera, wear, xr, tv, etc.: fora do escopo (app so de celular).
- Kotlin/kotlin-agent-skills: so migracoes/backend/KMP, nada util aqui.
- Skills Compose da comunidade (aldefy, Meet-Miyani, skydoves, awesome-android-agent-skills): sobrepoem as do Google; licenca NOASSERTION em uma; preferi as oficiais.
- Skills de Material 3 Expressive da comunidade: poucas estrelas e conflitam com a estetica do impeccable.
- Resto de marketingskills (copywriting, cro, ads...): fora do foco.

## Valeriam para todos os projetos (instalacao global, decisao do Vinicius)
- Ja existe global: impeccable. Para atualizar a mesma versao do projeto: copiar `.claude/skills/impeccable` para `~/.claude/skills/impeccable` (ou `npx skills add pbakaus/impeccable -g`).
- ux-writing: `npx skills add content-designer/ux-writing-skill -g`
- seo-audit (qualquer site): `npx skills add coreyhaines31/marketingskills --skill seo-audit -g`
- Android (outros apps): `npx skills add android/skills --skill jetpack-compose-performance -g` (idem edge-to-edge, testing-setup)
(Comandos do instalador "skills"; eu nao os usei, so copia manual.)
