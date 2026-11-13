# T8: Tela Ao Vivo — aba Programacao

## O que foi implementado

- **`AoVivoViewModel.kt`**: `enum class DiaPrograma { ONTEM, HOJE, AMANHA }` (top-level,
  pública — usada também por `ProgramacaoAba`); função pura top-level
  `janelaDoDia(dia, agora = System.currentTimeMillis()): Pair<Long, Long>` que calcula a
  janela `[inicio, fim)` em epoch ms do dia pedido, no fuso do aparelho; método
  `suspend fun programacaoDoDia(dia: DiaPrograma): List<Programa>` que usa o **canal
  atualmente selecionado** (`canalAtual`, campo já existente da T7) — devolve lista vazia sem
  canal selecionado ou sem `epgId` (o `?:` do safe-call cobre os dois casos numa linha só),
  senão chama `banco.catalogoDao().programacao(contaId, epgId, inicio, fim)` (T3).
- **`AoVivoTela.kt`**: placeholder da T7 trocado por `ProgramacaoAba(viewModel)`; comentário de
  topo do arquivo atualizado (não cita mais "T8 ainda não existe").
- **`ui/aovivo/ProgramacaoAba.kt`** (novo): seletor Ontem/Hoje/Amanhã em `FilterChip`s (mesmo
  padrão do `CanaisAba`), `LazyColumn` com título + horário (`HH:mm - HH:mm`) de cada
  `Programa`, e texto "Sem programacao." quando a lista vem vazia — cobre os dois motivos
  (canal sem EPG, janela sem programa) porque `programacaoDoDia` já devolve lista vazia nos
  dois casos; a UI não distingue a causa, só o efeito (igual ao `LinhaCanal` do `CanaisAba`,
  que já faz o mesmo com "Sem programacao" por programa).
- **`AoVivoViewModelTest.kt`**: 4 testes novos para `janelaDoDia` (helper `calendarEm` local).

## Decisão de API de data/hora: `java.util.Calendar`

Escolhido **`Calendar`**, não `java.time`. Motivo: `ResumoTela.kt` (`formatarData`) já usa
`java.text.SimpleDateFormat`/`java.util.Date` — não há nenhum uso de `java.time` no projeto.
`Calendar` fica no mesmo estilo e não exige checar `minSdk`/core library desugaring (que
`java.time.LocalDate`/`ZoneId` precisaria em minSdk < 26) — menor risco, zero configuração
nova. `ProgramacaoAba.kt` formata o horário com `SimpleDateFormat("HH:mm", ...)`, mesma
família de API do `formatarData`.

Semântica da janela: `HOJE` = `[00:00:00.000 de hoje, 00:00:00.000 de amanhã)` no fuso padrão
do `Calendar.getInstance()` (fuso do aparelho); `ONTEM`/`AMANHA` deslocam essa janela em -1/+1
dia inteiro via `Calendar.add(DAY_OF_MONTH, ...)`, que já lida com DST/virada de mês/ano
corretamente (responsabilidade do `Calendar`, não reimplementada). O limite superior é
exclusivo, batendo com o operador `<` de `CatalogoDao.programacao` (`inicio < :fimMs`).

## Evidência RED/GREEN do teste da janela do dia

**RED** (bug injetado de propósito em `janelaDoDia` — `ONTEM` virou `Unit`, ou seja, não
deslocava o dia):

```
AoVivoViewModelTest > janelaDoDia ONTEM desloca a janela um dia pra tras FAILED
    java.lang.AssertionError at AoVivoViewModelTest.kt:99
> Task :app:testDebugUnitTest FAILED
```

**GREEN** (bug revertido, suíte completa):

```
BUILD SUCCESSFUL in 1m 56s
47 actionable tasks: 7 executed, 40 up-to-date
```

## Contagem real da suíte

`app/build/test-results/testDebugUnitTest/*.xml` somado: **78 testes, 0 falhas, 0 skipped**
(14 arquivos de resultado). Antes da T8: 74 (confirmado no brief). `AoVivoViewModelTest.kt`
isolado: 11 testes (7 da T7 + 4 novos da T8), 0 falhas.

Comando final: `./scripts/gradle.ps1 testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`.

## Arquivos alterados

- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoViewModel.kt` (modificado)
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoTela.kt` (modificado)
- `app/src/main/java/io/github/pagmovel/iptvplayer/ui/aovivo/ProgramacaoAba.kt` (novo)
- `app/src/test/java/io/github/pagmovel/iptvplayer/ui/aovivo/AoVivoViewModelTest.kt` (modificado)

Commit: `e87e1a0` — `feat: tela de TV ao vivo -- aba Programacao` (autor/committer
`Marcos Ronaldo <marcos.ronaldo@gmail.com>`, via variáveis de ambiente, sem tocar
`git config`). Fora do commit, de propósito: `docs/.../plano-2...md` e os artefatos de
`.superpowers/sdd/.../` (briefs/reports/diffs/progress.md de outras tarefas) e o
`fwcd.kotlin-0.2.36.vsix` — pertencem à bookkeeping do orquestrador do plano, não ao escopo
de código da T8.

## Autorrevisão

- `programacaoDoDia` usa o canal atualmente selecionado (`canalAtual`, sem parâmetro extra)?
  **Sim.**
- Retorna lista vazia (não trava) sem canal/epgId? **Sim** — `canalAtual?.epgId ?: return
  emptyList()`.
- "Sem programação" aparece tanto por falta de EPG quanto por janela vazia? **Sim** — a
  mesma lista vazia cobre as duas causas, e a UI só olha pro tamanho da lista.
- O placeholder da T7 foi substituído sem quebrar o resto de `AoVivoTela.kt`? **Sim** — só a
  linha do `when` e o comentário de topo mudaram; `BUILD SUCCESSFUL` com o arquivo inteiro
  compilando.

## Dúvidas

Nenhuma — semântica de fuso/janela do dia já estava clara no brief e no design (Sec. "TV ao
vivo"); nenhum ponto exigiu escalar.
