# SDD ledger — plan: docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md

## Setup

- Dono aprovou o plano e o método (subagent-driven-development). Exigência adicional do dono:
  todo subagente despachado deve ser instruído a (a) economizar tokens via context-mode e
  (b) se comunicar em pt-BR. Regras obrigatórias, repetir literalmente em todo dispatch.
- Branch: dono autorizou continuar direto na `main` (sem worktree/branch isolada), já que design
  e plano também foram commitados lá.
- Workspace resolvido com `sdd-workspace`. Pegadinha confirmada (ver memória global do dono):
  o script criou `.superpowers/sdd/.gitignore` com `*`. Removido. `git check-ignore -v
  .superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md` → saída vazia (correto).
- `.vscode/settings.json` removido do versionamento antes de iniciar (pedido do dono), commit
  `aced657`. `CLAUDE.md` atualizado (commit `cdb3938`).

## Varredura de conflitos (pré-execução)

| Par de tarefas | O que uma produz / a outra consome | Achado |
|---|---|---|
| T1 → T2 | T1 cria `app/build.gradle.kts`; T2 modifica (assinatura/versão) | Sequencial, sem conflito. |
| T1 → T2 | T1 cria `AndroidManifest.xml`; T11 modifica | Sequencial, sem conflito (T11 é bem depois). |
| T2 → T3 | T2 produz os 4 Secrets consumidos pela T3/workflow | Sequencial, sem conflito. |
| T4 → T5 | T5 consome `CredenciaisXtream` (T4) | Sem conflito. |
| **T5 → T6** | T5 passo 3 usa auxiliares `JsonObject.long/int` que a T6 produz (`JsonCampos.kt`). O próprio texto da T5 prevê isso: "Implemente a T6 primeiro se precisar, ou declare esses auxiliares aqui e mova-os depois." | **Dependência direta no sentido contrário à ordem numérica.** |
| **T6 → T7** | T6 consome as entidades `Categoria/Canal/Filme/Serie` (T7). O texto da T6 prevê: "Escreva a T7 antes, ou defina primeiro só as data class... e complete na T7." | **Mesma situação: T6 depende de algo que só a T7 produz.** |
| T7 → T8 | T8 consome `Programa` (T7) | Sem conflito (T7 já antes na ordem numérica). |
| T7 → T9, T10 | T9/T10 consomem `BancoApp`/`CatalogoDao` (T7) | Sem conflito. |
| **T7 → T10** | T7 produz `BancoApp` com `fun catalogoDao()` e `fun contaDao()`, com a nota "O ContaDao é preenchido na T10". Mas a T10 declara em "Arquivos" só **criar** `conta/ContaDao.kt` — não lista modificar `dados/BancoApp.kt` (T7) para adicionar o método abstrato `contaDao()`. | **Lacuna do plano**: falta listar que a T10 também *modifica* `dados/BancoApp.kt`. |
| T5,T6,T7 → T9 | T9 consome API (T5), mapeamentos (T6), DAO/transação (T7), leitor XMLTV (T8) | Sem conflito, desde que as 4 já estejam prontas. |
| T4,T5,T7 → T10 | T10 consome `CredenciaisXtream` (T4), tipo `XtreamApi` (T5), `BancoApp` (T7) | Sem conflito. |
| T4,T9,T10 → T11 | T11 consome `ContasRepositorio` (T10), `CargaCatalogo` (T9), `lerLinkXtream` (T4) | Sem conflito. |
| Autocoerência por tarefa | Testes especificados vs. "Produz" de cada tarefa (T4…T11) | Lidos lado a lado; nenhuma contradição encontrada (nomes, assinaturas e casos de teste batem com as interfaces descritas). |
| Atribuição de commit | O plano manda terminar commits com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` | **Desatualizado**: a instrução de atribuição vigente desta sessão (mais recente, substitui orientações anteriores) é `Claude Sonnet 5 <noreply@anthropic.com>`. |

### Rulings

- `Ruling:` reordenar a execução da Fase 1 para **T4, T7, T6, T5, T8, T9, T10, T11** em vez da
  ordem numérica — resolve as duas dependências T5→T6 e T6→T7 sem duplicar código nem precisar
  de "stub e completar depois" (as duas alternativas que o próprio texto do plano already previa). Não
  contraria nenhuma restrição do plano (a ordem numérica é só rótulo das tarefas, não uma
  sequência mandatória). Custo se errado: baixo — é só ordem de despacho, reversível antes de
  chegar na T11.
- `Ruling:` a T10 também modifica `dados/BancoApp.kt` (T7) para adicionar `fun contaDao(): ContaDao`,
  além de criar os 3 arquivos já listados. Vou incluir isso explicitamente no dispatch da T10.
  Custo se errado: baixo — um método abstrato a mais, fácil de ajustar na revisão.
- `Ruling:` todo commit desta execução termina com `Co-Authored-By: Claude Sonnet 5
  <noreply@anthropic.com>`, não `Claude Opus 5.5` como o texto do plano (desatualizado) diz —
  sigo a instrução de atribuição vigente desta sessão, que substitui orientações anteriores
  equivalentes. Custo se errado: nenhum (é só texto do rodapé do commit).
- `Ruling:` a T3 (chave de assinatura, Secrets, push/tag v0.0.1/v0.0.2) e os passos de
  push/tag da T11 (passos 7-8) são feitos pelo controlador diretamente, nunca por subagente —
  envolvem geração de chave real, interação com o dono e ações públicas no GitHub que exigem o
  "sim" específico daquele momento, o que não cabe no padrão "subagente implementa → relatório →
  revisão". Custo se errado: nenhum, é só escolha de quem executa.

## Execução

BASE antes da T1: `cdb3938`.

- Task 1: implementado (commit `8a47817`). Relatório: `task-1-report.md`. Descobertas reais
  (não presumidas, verificadas rodando o build): AGP 9.5.1 tem Kotlin embutido (plugin
  `org.jetbrains.kotlin.android` removido), `kotlinOptions{}` não existe mais, `compileSdk`
  usa a sintaxe nova (`compileSdk { version = release(37) { minorApiLevel = 2 } }`, API 37.2 —
  a mais recente estável da imagem `cimg/android:2026.10`), Gradle 9.9.0, cache do Docker exige
  `--user root`. Pendências que o implementador escalou corretamente (não resolveu por conta
  própria):
  1. Removeu de novo o `.superpowers/sdd/.gitignore` (a pegadinha já tinha sido removida por
     mim antes de despachar; o script `sdd-workspace` provavelmente recriou ao rodar de novo).
     `.superpowers/sdd/` ficou untracked, deixado para o controlador comitar.
  2. `docs/superpowers/plans/.../plano-1-base.md` já estava modificado (edição minha, do aviso
     de autorização) — implementador corretamente não tocou nem comitou.
- Controlador: comitou a atualização do aviso do plano (`d4e633d`, já era minha edição) e vai
  versionar este workspace (`.superpowers/sdd/`) no próximo commit, conforme a regra do dono.
- Task 1: pacote de revisão gerado (`review-cdb3938..8a47817.diff`), revisor despachado
  (modelo sonnet).
- Task 1: revisão: ✅ spec conforme, qualidade **Aprovado**, 0 crítico, 0 importante. ⚠️ itens
  (versões/build real da imagem Docker, sintaxe do AGP, corpo do commit, ausência de
  push/tag) verificados por mim diretamente: corpo do commit `8a47817` confirmado com
  `Co-Authored-By: Claude Sonnet 5`; `git tag`/`git status -b` confirmam nenhum push nem tag;
  as descobertas de build (AGP/compileSdk) já vieram de execução real relatada pelo
  implementador. Nada ficou como lacuna real.
- Task 1: minor (deferred): `ui/MainActivity.kt:36` usa string hardcoded em vez de
  `stringResource(R.string.app_name)` (duplica `strings.xml:3`).
- Task 1: minor (deferred): `scripts/gradle.ps1:11` roda o build inteiro com `--user root`
  para contornar permissão do volume `iptv-gradle-cache`; um ajuste de dono do volume (fora
  do script) seria mais contido. Risco baixo (container local efêmero).
- Task 1: complete (commits cdb3938..8a47817, review clean, 2 minors deferred).

BASE antes da T4: `5dd7d0b`.

- Task 4: implementado (commit `0fe2a79`, modelo haiku). 4/4 testes passando. Relatório:
  `task-4-report.md`.
- Task 4: minor (deferred): o commit terminou com `Co-Authored-By: Claude Haiku 4.5
  <noreply@anthropic.com>` em vez de `Claude Sonnet 5`, apesar da instrução explícita no
  dispatch (não é só o texto desatualizado do plano — o controlador já tinha corrigido isso
  na instrução). Não vou reescrever o commit (a regra do dono é nunca dar `--amend` sem pedido
  explícito, e nada foi publicado ainda, então o risco de deixar como está é zero). Vou
  reforçar a instrução nos próximos dispatches e registrar isto para a revisão final decidir
  se corrige em lote.
- Task 4: pacote de revisão gerado (`review-5dd7d0b..0fe2a79.diff`), revisor despachado
  (modelo haiku).
- Task 4: revisão: ✅ spec conforme, qualidade **Aprovado**, 0 crítico, 0 importante. Nota
  técnica (não é defeito): usou `java.net.URL` em vez de `java.net.URI` como o brief sugeria —
  decisão válida, sem impacto.
- Task 4: complete (commits 5dd7d0b..0fe2a79, review clean).

BASE antes da T7: `6c1a8b9`.

- Task 7: despachado (implementador, modelo sonnet). Brief: `task-7-brief.md`. Relatório
  esperado em `task-7-report.md`. Aviso dado sobre a reordenação (T7 antes de T5/T6).
- Task 7: implementado (commit `6876c13`). 5/5 testes. Atribuição correta desta vez
  (`Co-Authored-By: Claude Sonnet 5`). Descoberta real registrada pelo implementador: a
  sintaxe do brief `"palavra"*` (asterisco fora das aspas) não aplica prefixo no FTS4 —
  a forma correta é `"palavra*"` (asterisco dentro), confirmado em sqlite.org/fts3.html e
  pelo teste de busca por prefixo falhando/passando nas duas formas.
- Task 7: pacote de revisão gerado (`review-6c1a8b9..6876c13.diff`), revisor despachado
  (modelo sonnet).
- Task 7: revisão: ✅ spec conforme (entidades, FTS, DAO, testes literais, isolamento por
  conta todos corretos). ❌ 1 achado **Importante**: `Fts.kt`/`CatalogoDao.kt` chamam
  `reconstruirIndice*` (FTS `rebuild`) manualmente depois de inserir/apagar, com comentário
  dizendo que "o Room não sincroniza a FTS sozinho" — **falso**: o próprio schema exportado
  no diff mostra os triggers `room_fts_content_sync_*` que o Room gera para
  `@Fts4(contentEntity=...)`, e o revisor confirmou rodando em SQLite isolado que a FTS já
  fica correta sem o rebuild manual. Custo real: rebuild completo dos 3 índices (de todas as
  contas, não só a alterada) a cada carga de catálogo — e a T9 chama esse DAO dentro de
  `withTransaction`, então o custo se repete a cada carga. 2 achados menores (rebuild rodando
  em lista vazia; escape de aspas no termo FTS, sem problema real).
- Task 7: fix round 1/5 — implementador original corrigiu (commit `9862d02`): removeu
  `reconstruirIndiceCanais/Filmes/Series()` e os métodos `*Bruto`, voltou a `@Insert` direto,
  corrigiu o comentário de `Fts.kt`. Verificou de forma independente antes de aplicar (abriu
  o próprio schema exportado e confirmou os triggers `contentSyncTriggers` do Room). 5/5
  testes continuam passando. Re-revisão focada despachada (modelo haiku,
  `review-6876c13..9862d02.diff`).
- Task 7: re-revisão — todos os 3 achados ADDRESSED, nenhuma quebra nova. Fix round 1/5
  fechou o loop.
- Task 7: complete (commits 6c1a8b9..9862d02, fix round 1/5, review clean).

BASE antes da T6: `6334d39`.

- Task 6: despachado (implementador, modelo sonnet). Brief: `task-6-brief.md`. Relatório
  esperado em `task-6-report.md`. Aviso dado sobre a reordenação (T6 antes de T5, depois T7).
