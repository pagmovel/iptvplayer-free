### T8: Tela Ao Vivo — aba Programação

**Arquivos:**
- Criar: `ui/aovivo/ProgramacaoAba.kt`
- Modificar: `ui/aovivo/AoVivoViewModel.kt`, `ui/aovivo/AoVivoTela.kt`

**Interfaces:**
- Consome: `CatalogoDao.programacao` (T3), `epgId` do `CanalAgrupado` atual (T1).
- Produz: `AoVivoViewModel.programacaoDoDia(dia: DiaPrograma): List<Programa>` com
  `enum class DiaPrograma { ONTEM, HOJE, AMANHA }`.

- [ ] **Passo 1: Implementar o cálculo da janela do dia** (início/fim em epoch ms a partir de
  `DiaPrograma` + fuso do aparelho) **em `AoVivoViewModel`**, chamando `programacao` (T3).
- [ ] **Passo 2: Implementar `ProgramacaoAba`** — seletor Ontem/Hoje/Amanhã (design §4), lista
  de programas com horário, e **"Sem programação"** quando a lista vier vazia (Review Focus) —
  seja porque o canal não tem `epgId`, seja porque a janela não tem nenhum programa.
- [ ] **Passo 3: Rodar `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`.**
- [ ] **Passo 4: Commit.** `feat: tela de TV ao vivo -- aba Programacao`

