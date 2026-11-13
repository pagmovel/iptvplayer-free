# SDD ledger — plan: docs/superpowers/plans/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo.md

## Setup

- Dono pediu para continuar "de onde parou" com abordagem subagent-driven (2026-11-12). Mesma
  exigência do Plano 1 continua valendo: todo subagente despachado deve ser instruído, no
  próprio prompt, a (a) usar os tools do plugin context-mode (`ctx_execute`, `ctx_execute_file`,
  `ctx_batch_execute`, `ctx_search`) em vez de `Read`/`Bash`/`Grep` diretos — `Read` nativo só no
  arquivo que vai receber `Edit` na sequência imediata — e (b) se comunicar em pt-BR.
- Branch: **Ruling** — continuar direto na `main`, sem worktree/branch isolada, seguindo o
  precedente do Plano 1 (ver `.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md`,
  seção Setup: "dono autorizou continuar direto na main... já que design e plano também foram
  commitados lá"). Mesmo padrão aqui: design e os dois planos já estão commitados na `main`,
  projeto solo-dev, sem colaboração remota concorrente. Custo se errado: teria que mover commits
  para uma branch depois — reversível via `git branch`/`git rebase`, baixo custo.
- Workspace resolvido com `sdd-workspace` em
  `.superpowers/sdd/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo/`. Verificado: nenhum
  `.superpowers/sdd/.gitignore` apareceu desta vez (pegadinha do Plano 1 não se repetiu).
  `git check-ignore -v .superpowers/sdd/2026-11-12-iptv-player-free-plano-2-tv-ao-vivo` → saída
  vazia (correto).
- Autoria dos commits: variáveis de ambiente por commit (nunca `git config`), autor/committer
  `Marcos Ronaldo <marcos.ronaldo@gmail.com>` (regra deste projeto, CLAUDE.md, a partir do
  commit `90e61b7`).

## Varredura de conflitos (pré-execução)

| Par de tarefas | O que uma produz / a outra consome | Achado |
|---|---|---|
| T1 → T2 | T1 cria `chaveCategoria`; T2 usa como parte da chave de `CanalFavorito` | Sequencial, sem conflito. |
| T1 → T5, T6, T7, T9 | T1 cria `CanalAgrupado`/`VarianteQualidade`/`Qualidade`; consumidos por todas | Sequencial, T1 é a base de tudo depois — ordem do plano já reflete isso. |
| T2 ↔ T3 | Ambas modificam `dados/CatalogoDao.kt` e `dados/CatalogoDaoTest.kt` (T2 adiciona favoritar/desfavoritar/favoritos; T3 adiciona categorias/canais/programacao) | Mesmo arquivo, métodos diferentes, sequencial (T2 antes de T3 na ordem do plano) — sem conflito real, cada implementador parte do HEAD atual. |
| T2 ↔ T6 | Ambas modificam `gradle/libs.versions.toml` e `app/build.gradle.kts` (T2 adiciona `room-testing`; T6 adiciona `media3-exoplayer`/`media3-ui`) | Mesmo arquivo, dependências diferentes, sequencial — sem conflito. |
| T4 → T5 | T5 recebe `tempoBufferMs` por parâmetro, não lê `ConfiguracoesRepositorio` direto (decisão do plano, pra não acoplar Android aos testes puros) | Sem conflito — interface já desenhada pra evitar acoplamento. |
| T5 → T6 → T7 → T9 | Cadeia de consumo de `CascataQualidade`/`EventoCascata` | Sequencial, sem conflito. |
| T7 cria → T8, T9, T10 modificam | `ui/aovivo/AoVivoTela.kt` e `ui/aovivo/AoVivoViewModel.kt` são criados em T7 e modificados em T8, T9 e T10 (cadeia de 4 tarefas no mesmo par de arquivos) | Sequencial conforme ordem do plano — cada implementador lê o estado atual do arquivo antes de editar. Ponto de atenção pro review: T9 e T10 não podem pisar em código que T8/T9 acabaram de adicionar. |
| T7 → T10 | Ambas modificam `ui/Navegacao.kt` (T7 adiciona `Tela.AO_VIVO`; T10 adiciona `Tela.CONFIGURACOES`) | Sequencial, sem conflito. |
| T10 (Passo 5) vs T11 | T10 Passo 5 manda documentar na skill nova "qualquer achado real do passo de validação (T11)" — mas T11 roda **depois** de T10 (T10 publica a tag v0.2.0 no Passo 6; T11 valida essa mesma versão no celular) | **Ruling**: T10 documenta o algoritmo/máquina de estados com o que já está confirmado nesta sessão (marcando os itens ainda abertos como `[A VERIFICAR]`, ex. variantes de qualidade não vistas tipo `UHD` — já é o que T11 Passo 2 assume). Achados reais de T11 entram em um commit de documentação **separado, depois da validação** — mesmo padrão usado no Plano 1 (commit `04bd7f7`, doc pós-release feito depois da tag, não dentro da tarefa que gerou a tag). Custo se errado: doc incompleta por um commit, corrigida no pós-validação — sem impacto em código. |
| Global Constraints vs texto das tarefas | 2 conexões simultâneas / timer 8s reinicia / erro definitivo na hora / falha na pior qualidade para / chave nome+categoria / troca manual não desliga automática / `epg_channel_id` igual entre variantes / segundo plano para completamente / `configChanges` na Activity / testes só com dados sintéticos | Todas batem com os testes e passos de T1-T11 (nenhuma tarefa contradiz uma constraint). Nenhum achado. |

Varredura limpa — nenhum conflito bloqueante. A única observação (T10×T11) foi resolvida por
ruling acima, sem impacto em código ou em ordem de execução.

## Execução

- Task 1 (agrupamento de canais por qualidade): brief extraído manualmente em
  `task-1-brief.md` (o script `task-brief` só casa cabeçalho `Task N` em inglês; este plano usa
  `### T1:` em pt-BR — extraído com `sed` entre `### T1:` e `### T2:`, linhas 114-170).
  BASE = `984448a`. Implementador despachado (modelo haiku, tarefa mecânica com spec completo:
  regex e os 5 testes dados literalmente no brief), agentId `ab980b89372102206`.
- Task 1: implementador reportou DONE. Commit `e537a08` "feat: agrupa canais por qualidade
  SD/HD/FHD/4K". 5/5 testes novos passando, suíte completa 51/51, 0 falhas. Sem preocupações.
  Relatório em `task-1-report.md`. Review package gerado:
  `review-984448a..e537a08.diff` (1 commit, 7792 bytes). Revisor despachado (modelo sonnet),
  agentId `a53f6646bab88bcff`.
- Task 1: revisor retornou ✅ Conforme / Aprovado. 0 Crítico, 0 Importante.
  Minor (deferred): (1) regex de sufixo duplicada em `qualidadeDoNome`/`nomeBase`
  (`CanalAgrupado.kt:28,42`) — extrair pra constante; (2) `grupoCanais.first()` pra
  icone/epgId/numero sem teste cobrindo variantes com valores diferentes entre si.
- Task 1: complete (commits 984448a..e537a08, review clean, 2 minor deferred).
- Task 2 (favoritos — migração Room + DAO): brief extraído manualmente em `task-2-brief.md`
  (linhas 171-211, entre `### T2:` e `### T3:`). Verificado antes do dispatch:
  `BancoApp.kt` atual é `version = 1, exportSchema = true` com 9 entidades (sem
  `CanalFavorito` ainda); schema exportado em
  `app/schemas/io.github.pagmovel.iptvplayer.dados.BancoApp/1.json`; Room já em `2.8.5`
  (`gradle/libs.versions.toml:14`). BASE = `e537a08`. Implementador despachado (modelo
  sonnet — migração Room + DAO, múltiplos arquivos), agentId `aaf7f886a4dc9de50`.
- Task 2: implementador reportou DONE_WITH_CONCERNS. Commit `d8e1b82`
  "feat: tabela de favoritos (migracao Room 1 -> 2)". Suíte completa 53/53. Duas
  preocupações: (1) não usou `MigrationTestHelper` do brief (dependeria de
  `AssetManager`/`InstrumentationRegistry`, indisponível em Robolectric puro deste projeto) —
  pivotou pra recriar o schema v1 com SQL e abrir com `Room.databaseBuilder` real + a
  `Migration(1,2)` real; (2) adicionou `.addMigrations(BancoApp.MIGRACAO_1_2)` em
  `AppIptv.kt`, fora do brief explícito. **Verificação do controlador antes da revisão**: li
  `AppIptv.kt` eu mesmo — a chamada `addMigrations` é necessária (sem ela o Room lançaria
  exceção na v2 sem `fallbackToDestructiveMigration`) e está correta. A preocupação (1) foi
  passada ao revisor como foco específico de verificação (se a migração testada é a mesma
  que a de produção, não uma reimplementação paralela). Review package:
  `review-e537a08..d8e1b82.diff` (43995 bytes). Revisor despachado (modelo sonnet),
  agentId `a76f51f71105ebcdd`.
- Task 2: revisor retornou ✅ Conforme / Aprovado. 0 Crítico, 0 Importante. Confirmou os 3
  pontos de verificação pedidos: `MigracaoBancoTest` usa o mesmo `BancoApp.MIGRACAO_1_2` de
  produção (não paralelo); schema v1 recriado programaticamente a partir do `1.json` real
  exportado (não SQL à mão); `Conta` pré-existente sobrevive à migração. SQL da migração
  idêntico ao `createSql` gerado pelo Room pra mesma entidade em `2.json`. Nenhum resíduo das
  tentativas com `MigrationTestHelper`. Minor (deferred): (1) `CatalogoDao.favoritos()`
  sem `ORDER BY` (`CatalogoDao.kt:96`) — se a tela de favoritos precisar de ordem, considerar
  `ORDER BY criadoEm DESC`; (2) comentário de doc do `MigracaoBancoTest.kt` longo/redundante.
  **Padrão validado para reaproveitar** (achado real, útil se alguma tarefa futura deste
  plano precisar testar outra migração Room): neste projeto, testes de migração Room não
  usam `MigrationTestHelper` (incompatível com Robolectric puro, sem assets
  mesclados/instrumentação) — o padrão é recriar o schema anterior programaticamente a partir
  do `.json` exportado em `app/schemas/`, abrir com o `Room.databaseBuilder` real + o objeto
  `Migration` real de produção, e deixar o `onValidateSchema` do próprio Room pegar
  divergência de schema.
- Task 2: complete (commits e537a08..d8e1b82, review clean, 2 minor deferred).
- Task 3 (consultas do catálogo pra TV ao vivo): brief extraído em `task-3-brief.md`
  (linhas 212-240). Verificado antes do dispatch: `CatalogoDao.kt` atual (já com os métodos
  de favoritos da T2), `TipoConteudo`/`Categoria`/`Programa` em `Entidades.kt`. BASE =
  `d8e1b82`. Implementador despachado (modelo haiku — 3 SELECTs mecânicos, spec completo),
  agentId `a38a4f99a24bb3402`.
- Task 3: implementador reportou DONE. Commit `b35cf38`
  "feat: consultas de categoria/canais/programacao pra TV ao vivo". **Observação do
  controlador antes da revisão**: li `task-3-report.md` eu mesmo — a seção "Suíte completa"
  mostra `testDebugUnitTest: BUILD SUCCESSFUL in 59s` descrito como "Todos os 11 testes da
  classe CatalogoDaoTest passaram", igual à evidência GREEN anterior (mesma contagem, 11).
  Isso é a classe de teste focada, não a suíte completa do módulo (que devia ter ~56 testes:
  53 da T2 + 3 novos) — a instrução de dispatch pedia suíte completa antes do commit. O
  relatório não contém evidência de um `testDebugUnitTest` sem filtro `--tests`. Repassado ao
  revisor como ponto específico de verificação.
- Task 3: revisor confirmou de forma independente o achado do controlador (comparou
  `task-3-report.md` com `task-2-report.md`: a T2 soma `TEST-*.xml` reais do módulo, 53/53;
  a T3 só repete a contagem da classe focada, 11, sob o rótulo "suíte completa"). Veredito:
  Precisa de correções. 1 Importante (suíte completa não rodada de fato — instrução de
  dispatch violada, risco baixo pelo diff mas sem evidência real), 1 Minor (teste de
  `programacao` não cobre o limite exato superior da janela, `inicio == fimMs`). Entrando no
  loop de correção, rodada 1/5 — retomando o implementador original (agentId
  `a38a4f99a24bb3402`).
- Task 3: fix round 1/5 — implementador reportou DONE. Commit `930412d` "fix: suite
  completa real e caso de borda da janela de programacao". Suíte completa real (sem
  `--tests`, XMLs somados): 56 testes, 0 falhas. Caso de borda `inicio == fimMs` adicionado
  e excluído corretamente. Review package: `review-b35cf38..930412d.diff`. Re-revisão
  escopada despachada (modelo haiku — diff pequeno, verificação mecânica), agentId
  `a274ade12f3604d9f`.
- Task 3: re-revisor retornou — os 2 achados ADDRESSED (contagem real 56/0, caso de borda
  `inicio==fimMs` excluído corretamente). 1 quebra nova Minor na própria correção: comentário
  em `CatalogoDaoTest.kt:159` diz "inclusive limit" quando a semântica real é "exclusive
  limit" (a query usa `<`, exclusivo) — cosmético, não afeta comportamento, deferred.
- Task 3: complete (commits d8e1b82..930412d, fix round 1/5, 1 minor deferred: comentário
  "inclusive"→"exclusive" invertido em `CatalogoDaoTest.kt:159`).
- Task 4 (configurações — tempo de buffer): brief extraído em `task-4-brief.md` (linhas
  241-265). BASE = `930412d`. Instrução explícita no dispatch pra rodar a suíte completa
  real (lição da T3). Implementador despachado (modelo haiku — wrapper simples de
  SharedPreferences, spec completo), agentId `a791e4a6b893d53e4`.
- Task 4: implementador reportou DONE. Commit `c76c57c`. Suíte completa real: 58 testes
  (56+2), 0 falhas. Review package: `review-930412d..c76c57c.diff`. Revisor despachado
  (modelo haiku), agentId `ad6195f89dde04cfe`.
- Task 4: revisor retornou ✅ Conforme / Aprovado. 0 Crítico, 0 Importante, 0 Minor.
  Confirmou evidência real de suíte completa (58 testes, 12 arquivos XML, sem filtro).
- Task 4: complete (commits 930412d..c76c57c, review clean).
- Task 5 (cascata de qualidade — máquina de estados pura): brief extraído em
  `task-5-brief.md` (linhas 266-332). Confirmado `kotlinx-coroutines-test` 1.11.0 já é
  dependência do projeto (Plano 1). BASE = `c76c57c`. Tarefa mais sensível a timing do plano
  até aqui — implementador despachado (modelo sonnet), agentId `a4ad6cb63bbe3d17d`.
- Task 5: implementador reportou DONE. Commit `ef1720b`. Suíte completa real: 66 testes
  (58+8), 0 falhas. **Correção da minha própria instrução**: o brief tem 8 `@Test fun`
  literais, não 9 como eu tinha dito no dispatch (contei errado) — implementador confirmou
  contando no brief, não inventou teste. Dois pontos levantados pelo implementador, repassados
  ao revisor pra verificação: (1) `advanceTimeBy()` sozinho não disparava o timer no
  kotlinx-coroutines-test 1.11.0, precisou de `runCurrent()` depois em 4 testes — corrigido no
  teste, não na implementação; (2) `escolherManualmente()` zera a flag de `FalhaTerminal` pra
  honrar "automática continua após escolha manual", mas nenhum dos 8 testes do brief exercita
  escolha manual DEPOIS de FalhaTerminal — gap de cobertura não coberto pelo brief literal.
  Review package: `review-c76c57c..ef1720b.diff`. Revisor despachado (modelo sonnet — tarefa
  mais sensível a timing do plano), agentId `ac39b08a5f2207245`.
- Task 5: revisor confirmou os 8 testes (não 9) e validou que `advanceTimeBy`+`runCurrent()`
  é uso correto da API 1.11.0, sem mascarar timing errado (teste 4, erro definitivo, nem
  chama `advanceTimeBy`, prova que a troca é síncrona). Veredito: Precisa de correções.
  1 Importante: `escolherManualmente()` zera `falhouDeFormaTerminal` (linha 76) — leitura que
  resolve uma tensão entre duas Global Constraints ("nunca insiste sozinho sem parar" vs.
  "troca manual não desliga a automática"), sem teste cobrindo a transição real
  `FalhaTerminal → escolherManualmente() → aoTravar() volta a agir`. 3 Minor: guards triviais
  de `tentarNovamente()`/`aoErroDefinitivo()` sem teste dedicado; `MutableSharedFlow` sem
  replay perde o evento `Tocando` inicial se o coletor assinar depois de `iniciar()` —
  pegadinha real pra T7, merece KDoc em `eventos`.
  **Ruling do controlador**: a leitura do implementador está correta, não é uma falha de
  design. "Nunca insiste sozinho sem parar" é sobre a cascata se auto-reiniciar; uma escolha
  manual é uma ação explícita do usuário, da mesma natureza de `tentarNovamente()` (que já
  reinicia a cascata depois de `FalhaTerminal` sem violar a constraint) — `escolherManualmente`
  depois de `FalhaTerminal` é só outra forma explícita de reiniciar, não a cascata "insistindo
  sozinha". A implementação fica como está; só falta o teste que comprova essa transição.
  Custo se a ruling estiver errada: 1 teste a ajustar depois, sem retrabalho de design.
  Entrando no loop de correção, rodada 1/5 — retomando o implementador original (agentId
  `a4ad6cb63bbe3d17d`), pedindo só o teste que falta (não mudança de implementação). Os 3
  Minor ficam deferred; o aviso de `MutableSharedFlow` será repassado no dispatch da T7.
- Task 5: fix round 1/5 — implementador reportou DONE. Commit `9510a67` "test: cobre
  transicao FalhaTerminal -> escolha manual na cascata". `CascataQualidade.kt` NÃO foi
  alterado, só o teste, como instruído. Suíte completa real: 67 testes, 0 falhas.
  Re-revisão escopada despachada (modelo sonnet), agentId `a5b436445bce6f94d`.
- Task 5: re-revisor retornou ADDRESSED — teste novo força `FalhaTerminal` real (com
  asserção explícita antes de `escolherManualmente`), depois confirma que `aoTravar()`
  subsequente degrada de novo (`Trocou(HD,SD)`). `CascataQualidade.kt` não tocado. Sem
  quebra nova. Suíte completa real 67/0.
- Task 5: complete (commits c76c57c..9510a67, fix round 1/5, 3 minor deferred: guards de
  `tentarNovamente()`/`aoErroDefinitivo()` sem teste dedicado; `MutableSharedFlow` sem
  replay — repassar aviso no dispatch da T7, quem assinar `eventos` precisa estar inscrito
  ANTES de chamar `iniciar()`).
- Task 6 (player Media3/ExoPlayer): brief extraído em `task-6-brief.md` (linhas 333-366).
  Verificado antes do dispatch: `AndroidManifest.xml` atual (sem `configChanges` ainda),
  `app/build.gradle.kts` sem Media3. Pegadinha do `MutableSharedFlow` sem replay (achado da
  T5) repassada no dispatch. BASE = `9510a67`. Implementador despachado (modelo sonnet —
  integração Android/ExoPlayer real, sem suíte unitária própria de peso), agentId
  `a3006f1236c71c1b0`.
- Task 6: implementador reportou DONE. Commit `9ce147f`. `assembleDebug` BUILD SUCCESSFUL,
  suíte completa 67/0 (sem teste novo, esperado). Usou `Dispatchers.Main.immediate` pra
  tentar garantir inscrição no `cascata.eventos` antes de `iniciar()` — repassado ao revisor
  como ponto específico de verificação (se a ordem de código realmente garante isso, ou se há
  corrida). Review package: `review-9510a67..9ce147f.diff`. Revisor despachado (modelo
  sonnet), agentId `a24a968a86cdabc7f`.
- Task 6: revisor confirmou a resolução da pegadinha do replay (verificou a ordem real de
  código, não só a alegação: `collect` aloca o slot de inscrição antes de qualquer suspensão
  real, então quando `escopo.launch{...}` retorna, o collector já está inscrito antes de
  `cascata.iniciar()` na linha seguinte). Veredito: Precisa de correções. 1 Importante real
  (não é gap de teste, é bug de implementação): `abrir()` registra um NOVO
  `Player.Listener` e lança uma NOVA coroutine de coleta a cada chamada, sem remover/cancelar
  os anteriores — e o próprio plano (T7) já define que `abrir()` será chamado de novo na
  MESMA instância a cada troca de canal. Consequência real: um timer de buffering pendente do
  canal anterior pode trocar o `MediaItem` de volta pra uma variante antiga; cada troca também
  vaza 1 listener + 1 coroutine (só liberados em `liberar()`, fim de sessão, não a cada
  troca). 3 Minor: `first{}` sem doc de invariante (canal/cascata precisam ser do mesmo
  canal); `context` deveria ser `applicationContext` (evita reter Activity); contrato
  "main thread" só em comentário interno, não no KDoc público de `abrir()`.
  Entrando no loop de correção, rodada 1/5 — retomando o implementador original (agentId
  `a3006f1236c71c1b0`).
- Task 6: fix round 1/5 — implementador reportou DONE. Commit `0e7a767` "fix: abrir() limpa
  listener e coleta da cascata anterior antes de trocar de canal". `Player.Listener`
  registrado 1x no `init`, delega pra campo `cascataAtual`; novo campo `jobColeta: Job?`
  cancelado no início de `abrir()` antes do novo coletor. Garantia da T5 (inscrição antes de
  `iniciar()`) preservada. `assembleDebug` OK, suíte 67/0. Review package:
  `review-9ce147f..0e7a767.diff`. Re-revisão escopada despachada (modelo sonnet), agentId
  `aed3a941f01988d33`.
- Task 6: re-revisor retornou ADDRESSED — listener único no `init` delegando pra
  `cascataAtual`; `jobColeta` cancelado no início de `abrir()` antes do novo coletor;
  garantia de inscrição antes de `iniciar()` preservada. Sem quebra nova. Observação do
  re-revisor sobre o diff parecer "truncado" — **verifiquei eu mesmo**: `git diff -U10` tem
  exatamente as mesmas 100 linhas que o `review-package` gerou; não é truncamento do script,
  é só o diff unificado parando de mostrar contexto depois de 10 linhas sem mudança
  seguinte (comportamento normal do `-U10`, `.play()`/chaves/`iniciar()` ficam fora da janela
  de contexto por não haver mudança próxima). Não é um defeito do tooling do SDD.
- Task 6: complete (commits 9510a67..0e7a767, fix round 1/5, 3 minor deferred: invariante do
  `first{}` sem doc de que `canal` e `cascata` precisam ser do mesmo canal lógico; `context`
  deveria ser `applicationContext` pra não reter Activity; contrato "main thread" só em
  comentário interno, não no KDoc público de `abrir()`).
- Task 7 (tela Ao Vivo — aba Canais, maior integração do plano): brief extraído em
  `task-7-brief.md` (linhas 367-422). Verificado antes do dispatch: padrão de `Navegacao.kt`
  (enum `Tela` privado + `when`), `ResumoTela.kt` (botão "Gerenciar contas", sem
  `androidx.lifecycle.ViewModel`), como montar a URL de streaming
  (`contasRepositorio.credenciais(conta)` → `app.criarApi(cred)` → `api.urlAoVivo(streamId)`
  — não estava explícito no brief, descoberto lendo o código real). BASE = `0e7a767`.
  Implementador despachado (modelo sonnet), agentId `a7596dc74789694ff`.
- Task 7: implementador reportou DONE_WITH_CONCERNS. Commit `c7a9050`. 74/74 testes (67+7),
  `assembleDebug` OK. TDD pegou bug real em `programaNoInstante`, corrigido antes do commit.
  4 decisões próprias sinalizadas: (1) ícone sempre fallback "nome sobre fundo neutro",
  nunca carrega `canal.icone` real — **verifiquei eu mesmo**: não existe NENHUMA lib de
  imagem no projeto (nem do Plano 1), `ponytail:` documentado com teto e upgrade path em
  `CanaisAba.kt:140-142`; (2) chips Todos/Favoritos/categoria com seleção única, não
  explícito no brief; (3) botão "Voltar" acrescentado, não pedido mas necessário; (4) selo
  "Mudou para X" não implementado — **verifiquei eu mesmo**: é escopo explícito da T9
  (OSD), o brief da T9 já diz isso literalmente, não é lacuna da T7. Também notou (não agiu,
  corretamente) arquivos não commitados de outras tarefas/do controlador no repo (ledger,
  briefs, reports, diffs do SDD) — são artefatos meus desta sessão, não do usuário; ficam
  sem commit por ora, seguem a convenção do Plano 1 (commit de documentação consolidado
  depois, não a cada tarefa). Review package: `review-0e7a767..c7a9050.diff` (39KB, o maior
  do plano). Revisor despachado (modelo sonnet), agentId `ae3a956a639e164f3`.
- Task 7: revisor retornou ✅ Conforme / Aprovado, verificação linha a linha (não só o
  relatório) dos 7 passos + 4 restrições de integração + os 4 pontos sinalizados (confirmou
  minhas duas verificações prévias — ícone sem lib é razoável, selo "Mudou para X" é escopo
  da T9 — e validou os outros 2 pontos: chips de seleção única batem com o padrão já usado
  na seção "Busca" do design; botão Voltar é necessário). 0 Crítico, 0 Importante. 3 Minor
  (deferred): `categoriaSelecionada`/`somenteFavoritos` duplicados entre `remember` da UI e
  `MutableStateFlow` do ViewModel (2 fontes de verdade sincronizadas por convenção — se T9/OSD
  precisar ler o filtro de fora da `CanaisAba`, expor como estado público do ViewModel);
  `liberar()` cancela `jobEventos` manualmente além do `escopo.cancel()` (redundante,
  inofensivo); "Sem programacao" pode aparecer por uma fração de segundo antes do
  `produceState` resolver, em canal que tem programação.
- Task 7: complete (commits 0e7a767..c7a9050, review clean, 3 minor deferred).
- Task 8 (tela Ao Vivo — aba Programação): brief extraído em `task-8-brief.md` (linhas
  423-441). Verificado antes do dispatch: `AoVivoTela.kt` já tem o `TabRow`/`AbaAoVivo` com
  placeholder exato pra trocar; `AoVivoViewModel.kt` já tem `canalAtual` privado e
  `programaAtual(canal)` (T7) pra reaproveitar o padrão. BASE = `c7a9050`. Implementador
  despachado (modelo sonnet), agentId `ab0685e1b73a0c733`.
- Task 8: implementador reportou DONE. Commit `e87e1a0`. Suíte 78/0 (74+4). RED/GREEN via
  bug injetado/revertido. Review package: `review-c7a9050..e87e1a0.diff`. Revisor
  despachado (modelo sonnet), agentId `a4d94618b7bb706db`.
- Task 8: revisor retornou ✅ Conforme / Aprovado, verificado linha a linha. 0 Crítico, 0
  Importante. 3 Minor (deferred): `janelaDoDia` sem teste de virada de mês/ano; `programacaoDoDia`
  sem teste direto pros casos canal nulo/sem epgId (consistente com a convenção já existente
  de não testar isoladamente métodos que só repassam pro DAO); "Sem programacao." com ponto
  em `ProgramacaoAba` vs "Sem programacao" sem ponto em `CanaisAba` (cosmético).
- Task 8: complete (commits c7a9050..e87e1a0, review clean, 3 minor deferred).
- Task 9 (OSD do player): brief extraído em `task-9-brief.md` (linhas 442-468). Duas
  lacunas reais identificadas antes do dispatch e repassadas ao implementador: (1)
  "Configurações" é escopo da T10 (depois desta) — instruído a propagar
  `onAbrirConfiguracoes` até `Navegacao.kt` como no-op/TODO, não inventar a tela; (2)
  "canal anterior/próximo" precisa do canal atual público em `AoVivoViewModel`, hoje privado
  — instruído a expor e adicionar `canalAnterior`/`canalProximo` navegando a lista
  `canaisFiltrados` (resolve também o Minor deferred da T7 sobre o filtro não ser acessível
  de fora da `CanaisAba`). BASE = `e87e1a0`. Implementador despachado (modelo sonnet),
  agentId `a3f1bbe2dff64c13e`.
- Task 9: implementador reportou DONE. Commit `ba38bbd`. 85/85 testes (78+7), build OK.
  **Achado cross-task real**: descobriu que `CascataQualidade.escolherManualmente()` (T5, já
  aprovada) nunca emitia `EventoCascata.Trocou` — o seletor manual do OSD não teria efeito
  nenhum no vídeo. Corrigiu em `CascataQualidade.kt` (+9/-?) com 2 testes novos em
  `CascataQualidadeTest.kt`, alegando que os 9 testes da T5 continuam passando. Confirmado no
  diff: `CascataQualidade.kt` e `CascataQualidadeTest.kt` realmente estão no diff da T9
  (`git diff --stat`). Review package: `review-e87e1a0..ba38bbd.diff` (7 arquivos,
  334+/33-). Revisor despachado com foco dedicado nesse fix retroativo (modelo sonnet),
  agentId `a16e47b70bf44c323`.
- Task 9: revisor validou o fix retroativo da T5 como **correto, mínimo e bem testado**
  (respondeu às 5 perguntas: bug real confirmado; `Trocou(de,para)` com valores certos, `de`
  lido antes de atualizar o índice; não toca no timer; 2 testes provam valor exato, não só
  ausência de exceção; nenhum teste de T5-T8 quebrado — `escolherManualmente` só aparece em 3
  arquivos, nenhum teste pré-existente olha a lista inteira de eventos). Sem risco pras
  tarefas já aprovadas. Veredito da T9: Precisa de correções. 1 Importante (não-conformidade
  direta com o Passo 2 do brief): selo "Mudou para X" pode ficar preso na tela — `Osd.kt:60-65`,
  `LaunchedEffect(eventoAtual)` cancela a corrotina do delay quando `eventoAtual` muda, mas
  não limpa `seloEvento` se a nova chave não for `Trocou` (ex. trocar de canal zera
  `eventoAtual` e emite `Tocando` do canal novo com o selo do canal antigo ainda visível, pra
  sempre). Fix sugerido: `else { seloEvento = null }`. 3 Minor: botão Voltar duplicado (topo
  fixo + dentro do OSD); `canalAnterior/Proximo` resubscrevem `canaisFiltrados.first()` a
  cada clique (funciona, não é o mais barato); cores do OSD hardcoded (já reconhecido no
  relatório). Entrando no loop de correção, rodada 1/5 — retomando o implementador original
  (agentId `a3f1bbe2dff64c13e`).
- Task 9: fix round 1/5 — implementador reportou DONE. Commit `352d3da` "fix: selo de troca
  de qualidade nao fica preso no OSD". Diff real de código: 6 linhas em `Osd.kt` (resto do
  diff é o relatório de correção anexado). Suíte 85/0, `assembleDebug` OK. Re-revisão
  escopada despachada (modelo haiku — diff pequeno, verificação mecânica), agentId
  `ac7e4e71ddd6b0b93`.
- Task 9: re-revisor retornou ADDRESSED — `else { seloEvento = null }` em `Osd.kt:208-209`
  limpa o selo no mesmo `LaunchedEffect` quando `eventoAtual` deixa de ser `Trocou` antes do
  delay completar. Sequência válida (Trocou completo, 3s) continua funcionando. Sem quebra
  nova.
- Task 9: complete (commits e87e1a0..352d3da, fix round 1/5, 3 minor deferred: botão Voltar
  duplicado — topo fixo + dentro do OSD; `canalAnterior/Proximo` resubscrevem
  `canaisFiltrados.first()` a cada clique; cores do OSD hardcoded em vez de
  `MaterialTheme.colorScheme`). **Nota permanente**: T5 (`CascataQualidade`) recebeu fix
  retroativo validado nesta tarefa — `escolherManualmente()` agora emite `Trocou(de,para)`
  de verdade (antes não emitia nada, o seletor manual do OSD não teria efeito no vídeo).
- Task 10 (tela cheia + Configurações + navegação — **só Passos 1-5**; Passo 6, tag
  `v0.2.0`, fica com o controlador, não é dispatched): brief extraído em `task-10-brief.md`
  (linhas 469-499). Lido `AoVivoTela.kt` completo pós-T9 pra planejar a restruturação
  condicional por orientação sem recriar `Osd`/overlay de grade. BASE = `352d3da`.
  Implementador despachado com instrução explícita de não tocar em tag/release/git push
  (modelo sonnet), agentId `afcc25a4a6ab87da3`.
- Task 10 (passos 1-5): implementador reportou DONE. Commits `e9068c4` (feat) e `936e5fb`
  (docs: skill nova + Fase 2 testada no design). 87/87 testes (85+2), build OK. Decisão de
  julgamento sinalizada: `ConfiguracoesTela` usa botão "Salvar" explícito em vez de
  auto-save a cada tecla. Nova skill `.claude/skills/iptv-tv-ao-vivo/SKILL.md` já aparece
  listada nas skills do projeto. Review package: `review-352d3da..936e5fb.diff` (2 commits,
  30KB). Revisor despachado com instrução de confirmar que nenhum dos 2 commits toca em
  tag/release (Passo 6 era fora do escopo do implementador) e de avaliar se a skill nova
  documenta de fato o achado real da T9 (fix do `escolherManualmente`) como pegadinha
  (modelo sonnet), agentId `a744c6a23c96ea244`.
- Task 10 (passos 1-5): revisor retornou ✅ Conforme / Aprovado. Confirmou escopo (nenhum
  dos 2 commits toca `.github/workflows/`, tag ou script de release). Confirmou que a skill
  nova documenta de fato o achado real da T9 (não é só "como fazer") — também cita o
  fallback `Qualidade.SD` sem sufixo reconhecido, o timer reiniciando por tentativa, e por
  que `FalhaTerminal` não tem retry automático (limite de 2 conexões). 0 Crítico, 0
  Importante bloqueante — o único ⚠️ (tabela de acompanhamento do plano não tocada pelos
  commits) é a convenção já estabelecida: eu (controlador) mantenho essa tabela, não o
  implementador. 2 Minor (deferred): sem mensagem de erro quando o valor digitado em
  Configurações não é inteiro > 0 (só feedback positivo "Salvo."); botão "Salvar" explícito
  vs. auto-save — avaliado como decisão razoável, não é problema.
- Task 10 (passos 1-5): complete (commits 352d3da..936e5fb, review clean, 2 minor
  deferred). **Passo 6 (publicar tag v0.2.0) ainda pendente** — ver seção Finish/Release.

## Finish/Release

- **Verificado nesta sessão** (não suposto): `.claude/settings.local.json` atual tem
  `Bash(git push origin v*.*.*)` — autorização permanente real pra push de tags de versão,
  concedida pelo dono durante o Plano 1 (ledger
  `.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md:658-664`: "sempre
  que necessitar", ampliada depois de `v0.0.*` pra `v*.*.*` antes da tag `v0.1.0`). Cobre
  `v0.2.0`. Não preciso pedir "sim" de novo pra este push específico.
- **Ruling**: o Plano 1 não fez uma revisão final de toda a branch separada das revisões por
  tarefa (confirmado lendo o ledger até o fim — a sequência foi só: código → tag → validação
  no celular → plano completo). Mas o Plano 2 é mais integrado (10 tarefas alimentando uma
  única feature de UI, com 2 bugs cross-task já achados em revisão — T6 vazamento de
  listener/coroutine, T9 fix retroativo na T5) — decido fazer a revisão final de toda a
  branch (skill subagent-driven-development, modelo mais capaz) ANTES de publicar a tag,
  mesmo não sendo o que o Plano 1 fez, porque é mais barato achar um bug cross-cutting agora
  do que depois que o dono já instalou a tag no celular pra validar (T11). Custo se errado:
  um dispatch extra de revisão, sem repetir trabalho de código.
- Push/tag em si (quando chegar): feito por mim (controlador) diretamente, nunca por
  subagente — mesma decisão já registrada no Plano 1 (ações públicas no GitHub).
- **Observação de processo** (não é achado de código): os implementadores de T2, T5, T7 e T9
  cada um comitou seu próprio `task-N-report.md` junto com o commit de código da tarefa (em
  vez de deixar fora, como combinado) — confirmado via `git log -- <arquivo>`. Não é
  violação de regra (o projeto exige versionar `.superpowers/sdd/` mesmo), só comitagem mais
  misturada do que o planejado. Vou reforçar em dispatches futuros: `git add` só os arquivos
  do brief, nunca `git add -A`/`.`.
- Review package da branch inteira gerado: `review-984448a..936e5fb.diff` (15 commits, 34
  arquivos, ~185KB, 3246 inserções). Revisor final despachado (modelo opus, o mais capaz
  disponível), instruído a revisar em passos (dados/lógica pura primeiro, depois UI) e com
  foco em achados cross-cutting que as 10 revisões por tarefa não pegariam. agentId
  `a347c41af09e762dd`.
- **Revisão final retornou: NÃO pronto pra tag.** 3 Crítico + 6 Importante, todos achados
  cross-cutting reais que nenhuma revisão por tarefa isolada pegaria (validados com
  investigação real do bytecode do Media3 1.11.1, não suposição):
  1. **Crítico**: sem `keepScreenOn` — tela apaga durante a reprodução, dispara `ON_STOP`,
     vídeo para. Fix: `keepScreenOn = true` no `PlayerView` (`AoVivoTela.kt`).
  2. **Crítico**: timer de buffer não reinicia na qualidade nova — `aoTravar()` só é chamado
     na transição PRA `STATE_BUFFERING`; como o player já está em BUFFERING quando o timer
     vence, `setMediaItem`/`prepare()` não disparam novo callback (confirmado no bytecode:
     `prepare()` não faz nada fora de IDLE, `setMediaSourcesInternal` mascara pra BUFFERING,
     `onPlaybackStateChanged` só avisa se o estado mudou) — a cascata trava depois da
     primeira descida. Fix: chamar `cascata.aoTravar()` explicitamente no coletor de
     `PlayerAoVivo.abrir()` depois de `setMediaItem`/`prepare`/`play`.
  3. **Crítico**: vídeo pode voltar a tocar sozinho em segundo plano — `pausar()` só chama
     `exoPlayer.pause()`, mas listener/coleta/timer continuam vivos; se o timer vencer com o
     app minimizado, `Trocou` dispara `play()` de novo (áudio em segundo plano + conexão
     aberta, viola o limite de 2 conexões). Fix: `PlayerAoVivo.parar()` nova (cancela
     `jobColeta`, chama `aoEstabilizar()`, `exoPlayer.stop()`), chamada em `pausar()`.
  4. Importante: `FalhaTerminal` não para o player — conexão/stream pode continuar atrás do
     aviso de erro. Fix: `exoPlayer.stop()` no ramo `FalhaTerminal` do coletor.
  5. Importante: botão "Tentar de novo" fica coberto pelo `Osd` (desenhado depois, ocupa a
     área toda, engole o toque). Fix: desenhar o aviso de falha depois do `Osd`.
  6. Importante: `ProgramacaoAba` mostra a grade do canal anterior — `produceState(dia)` não
     tem `canalAtual` na chave. Fix: chave `dia, viewModel.canalAtual`.
  7. Importante: em paisagem sem canal selecionado (entrar direto deitado, ou voltar de
     Configurações deitado perde `canalAtual`), tela preta sem saída. Fix:
     `telaCheia = emPaisagem && canalAtual != null`.
  8. Importante (promovido de minor da T7): filtro duplicado UI/ViewModel gera estado
     divergente real (chips voltam pra "Todos" ao sair/voltar da aba, filtro do ViewModel
     continua valendo). Fix: UI passa a ler o `StateFlow` público do ViewModel.
  9. Importante: `Trocou` só carrega `Qualidade`, não a `VarianteQualidade` — grupo com 2
     variantes da mesma qualidade (provedor duplicando canal, ou sufixo ausente caindo em
     SD junto com um "X SD" explícito) repete a mesma variante que já falhou. Fix: `Trocou`
     passa a carregar a `VarianteQualidade` de destino.
  Triagem dos minors já deferred: 2 descartados com razão real (`favoritos()` sem ORDER BY —
  nunca exibido em ordem; `PlayerAoVivo` usa `context` — único chamador já passa `app`,
  `Application`, não retém Activity; virada de mês/ano — `Calendar.add` já trata; invariante
  canal/cascata — some com o fix #9); 1 promovido (item 8 acima); resto confirmado Minor.
  Achados Minor novos: inconsistência de acentos entre telas; "Gerenciar contas" do OSD
  sempre volta pro Resumo (não pra TV ao vivo); Configurações perde o canal ao voltar (sem
  BackHandler do sistema).
- **Ruling**: entrando no ÚNICO fix wave da revisão final (regra do skill: 1 dispatch pro
  conjunto completo + 1 re-revisão escopada, sem segunda rodada — achados residuais sobem
  pro dono). Despachando 1 implementador pros 3 Crítico + 6 Importante. BASE = `936e5fb`.
  Implementador despachado com os 9 fixes detalhados e já com a solução indicada pelo
  revisor (modelo sonnet), agentId `a30ee0bf01b934e27`.
- Fix wave: implementador reportou DONE_WITH_CONCERNS (9/9 addressed, nenhum parcial).
  Commit `7248a12`. 87/87 testes, `assembleDebug` OK. Preocupação reconhecida: Críticos 1-3
  (keepScreenOn, timer, parar em segundo plano) sem teste automatizado possível (dependem de
  ExoPlayer/ciclo de vida Android reais) — mesma categoria de limitação já documentada em T6,
  validação real fica pra T11. Review package: `review-936e5fb..7248a12.diff` (30KB, 1
  commit). Re-revisão escopada única despachada (modelo opus — 9 achados, #9 muda o tipo de
  `EventoCascata.Trocou`), agentId `a2f1a729a7327224e`.
- Fix wave: re-revisor retornou ADDRESSED pros 9/9, sem quebra nova Crítica/Importante.
  Verificou com cuidado a interação #2×#3 (timer novo vs. `parar()` — `aoEstabilizar()`
  cancela o `timerJob` antes de qualquer `descerQualidade()`, `stop()` leva a IDLE que o
  listener ignora, nada reemite depois de `parar()`) e #9 (emissores usam `varianteAtual()`
  pelo índice real, não `first{}` de novo; `PlayerAoVivo`/`Osd` usam `evento.para` direto;
  testes comparam variante exata via `streamId` distinto, não só "compila"). 2 Minor novos
  (deferred): falta teste de regressão pro cenário exato do achado 9 (`canalCom(HD,SD,SD)`,
  barato de escrever, JVM puro); parâmetro `canal` de `abrir()` ficou sem uso. **Observação
  fora de escopo (deferred, não estende o loop)**: a troca MANUAL de qualidade
  (`escolherManualmente`) tem a MESMA família de bug do achado 9, só que no caminho manual —
  resolve por `indexOfFirst { qualidade == X }`, então grupo com 2 variantes da mesma
  qualidade pode reselecionar a que já falhou ao escolher manualmente; os 2 botões da mesma
  qualidade no OSD também aparecem destacados juntos. Correção natural: o seletor manual
  passar `VarianteQualidade`/índice em vez de `Qualidade`. **Ressalva real**: a alegação de
  "sem teste possível" pros Críticos 1-3 é exagerada — Robolectric 4.17 já está no projeto, um
  teste de `parar()` com ExoPlayer real seria viável, mas provavelmente precisaria de
  `media3-test-utils-robolectric` [A VERIFICAR], fora do escopo deste fix wave.
- **Fix wave: complete** (commits 936e5fb..7248a12, re-revisão única clean, 2 minor
  deferred + 1 observação fora de escopo deferred + 1 ressalva sobre testabilidade). Skill
  `.claude/skills/iptv-tv-ao-vivo/SKILL.md` ainda NÃO documenta as 3 pegadinhas deste wave —
  controlador vai atualizar agora (regra global: documentar na hora, não acumular).
- Skill `iptv-tv-ao-vivo/SKILL.md` atualizada com as 3 pegadinhas cross-cutting (+1 achado
  residual da T9) e o `keepScreenOn` — commit `5f42ef1`. Workspace do SDD (ledger, briefs,
  reports, diffs) + tabela de acompanhamento do plano (T1-T10 `testada`) consolidados em
  commit único — `6452ea2`.
- **Push + tag v0.2.0 publicados** (eu, controlador, diretamente — nunca por subagente).
  `git push origin main` (18 commits) confirmado chegando em `origin/main` (6452ea2).
  `git tag v0.2.0` + `git push origin v0.2.0` confirmado em `git ls-remote --tags origin`.
  Acompanhado via API pública do GitHub (sem `gh` CLI nesta máquina, mas o repo é público —
  `curl` funcionou sem autenticação, run `43338532386`): job `testes` = success, job
  `release` = success. **Release real publicada**:
  https://github.com/pagmovel/iptvplayer-free/releases/tag/v0.2.0, anexo
  `iptv-player-free-0.2.0.apk` (12.110.118 bytes).
- **Task 10: complete por inteiro** (todos os 6 passos, commits 352d3da..6452ea2 + tag
  v0.2.0). **Plano 2 completo exceto T11** (validação do dono no celular, contra o provedor
  real — próximo passo, trabalho do dono, não dispatch).

## T11 — validação real do dono (2026-11-13)

- Dono começou a validar no celular, contra o provedor real, com prints reais (Warner
  Channel tocando, abas Canais/Programacao, chips Todos/Favoritos/categoria visíveis).
- **Achado real 1 (confirmado por print, não suposição)**: em paisagem, a barra de status
  (hora/notificações) e a barra de navegação do sistema continuam visíveis por cima do
  vídeo — a T10 só escondeu a NOSSA UI (Voltar/TabRow/abas), nunca implementou o modo
  imersivo real do Android. Dono esperava "sumir em tudo, só aparecer se deslizar o dedo,
  como em qualquer aplicativo" (modo imersivo "sticky" padrão). Confirmado no código: nenhum
  uso de `WindowInsetsController`/`systemBars`/`immersive` em lugar nenhum do projeto antes
  deste fix. Implementador despachado pra `DisposableEffect(telaCheia)` com
  `WindowInsetsControllerCompat` (modelo sonnet), agentId `afe14eff4bef70928`.
- **Achado real 2 (ainda sem diagnóstico — aguardando resposta do dono)**: "a programação
  ainda não exibe nada". Perguntei: mostra o texto "Sem programação" ou fica literalmente em
  branco? Em qual canal? Sem essa resposta ainda não investigo (pode ser canal sem EPG de
  verdade — comportamento esperado — ou bug real).
- **Feedback de design (não é bug, não entra nesta rodada de fix)**: dono achou a cor
  primária (`Destaque = #4FC3F7` em `ui/Tema.kt`, confirmado no código — é a cor aprovada no
  Plano 1, só que aplicada em muito mais lugares agora) ruim na prática, e citou "problemas
  sérios de design" de forma mais ampla. **Perguntei e o dono escolheu**: terminar a
  validação funcional da T11 primeiro, design em sessão separada depois. Registrado em
  memória do projeto (`.claude/memory/plano2-feedback-design-t11.md`), não no ledger do SDD
  (memória sobrevive a este plano, ledger não).

- **Achado real 1: complete.** Fix do modo imersivo (`926c4e4`, 27 linhas em `AoVivoTela.kt`)
  revisado e aprovado — 0 Crítico, 0 Importante. Revisor confirmou (não supôs): o cast
  `(view.context as Activity)` é seguro neste projeto (MainActivity é ComponentActivity pura,
  sem Fragment/Preview); os 4 caminhos de restauração das barras (retrato, canal null,
  trocar de tela, fechar o app) via `Navegacao.kt` real; `configChanges` da T6 intacto; sem
  efeito colateral no resto do app. 2 Minor cosméticos (referência imprecisa ao Accompanist
  no relatório do implementador; `systemBarsBehavior` não resetado no `onDispose`, inofensivo)
  — deferred. Validação visual real (barras somem/voltam com deslizar de verdade) ainda
  depende do celular do dono — próximo passo de T11.

- **Achado real 2: [INCONCLUSIVO], não é bug confirmado.** Dono não reproduziu de novo —
  "ficou em branco" (nem o texto "Sem programacao.", pelo relato) na primeira vez, mas
  "executei novamente e a programação apareceu" na segunda. Sem reprodução consistente, sem
  print da tela em branco, não investigo mais a fundo nem proponho fix (regra: teste que não
  decidiu é inconclusivo, não vira conclusão porque "faz sentido" supor). Se reaparecer,
  preciso de: print da tela exata, canal, e se os chips Ontem/Hoje/Amanhã apareceram ou não.
  Dono testou em outro canal também e funcionou — reforça que não é específico de um canal.
  Fecho como inconclusivo, não reproduzível; sigo sem fix.

- **Patch v0.2.1 publicado** (push + tag, eu/controlador diretamente) com o fix do modo
  imersivo (`926c4e4`). Run `43428075245`: job `testes` = success, job `release` = success.
  Release real: https://github.com/pagmovel/iptvplayer-free/releases/tag/v0.2.1, anexo
  `iptv-player-free-0.2.1.apk` (12.110.118 bytes).

- **Achado real 3 (causa raiz confirmada, print real)**: "sem botões de canal anterior/
  próximo" NÃO era o nosso `Osd` — era o controle NATIVO do Media3 `PlayerView` aparecendo
  por cima (retroceder/play/avançar 10s, item anterior/próximo de playlist, barra de
  progresso, engrenagem). `PlayerView(context).apply {...}` em `AoVivoTela.kt` nunca
  desativou `useController` (default `true`). Explica também a confusão: o controle nativo
  não tem noção de "canal", só item de playlist (que nem usamos — 1 `MediaItem` por vez).
- **Achado real 4 (gap confirmado no código)**: sem spinner durante buffering — só existe
  `CircularProgressIndicator` pro carregamento INICIAL da tela; nada reage a
  `STATE_BUFFERING` depois disso; `PlayerView` sem `setShowBuffering`.
- Implementador despachado pros 2 juntos: `useController = false` + spinner de buffering
  exposto via o listener já existente de `PlayerAoVivo` (cobrindo troca de canal E de
  qualidade, sem aparecer sobre `FalhaTerminal`) (modelo sonnet), agentId
  `a743af264c60ed162`.

- **Achados reais 3 e 4: complete.** Revisor aprovou (0 Crítico, 0 Importante) — confirmou
  o único `PlayerView` do projeto, rastreou os 3 únicos emissores de evento da cascata
  (`iniciar`/`escolherManualmente`/`descerQualidade`, todos cobertos pelo reset do spinner),
  e investigou a condição de corrida pedida (dois coletores do mesmo `SharedFlow`, ordem de
  inscrição segura, sem janela onde o spinner fica preso sobre `FalhaTerminal`). 2 Minor
  (deferred): sobreposição cosmética entre o scrim do `Osd` e o spinner quando os dois estão
  visíveis ao mesmo tempo (tocar a tela durante um buffering); imprecisão no relatório do
  implementador sobre a transição de estado (READY→BUFFERING direto, não
  READY→IDLE→BUFFERING — não afeta a correção).
- Commits `681bfc7`/`7d32801` prontos pra nova tag de patch.
