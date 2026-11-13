### T1: Agrupamento de canais por qualidade (SD/HD/FHD/4K)

**Arquivos:**
- Criar: `aovivo/CanalAgrupado.kt`
- Teste: `aovivo/CanalAgrupadoTest.kt`

**Interfaces:**
- Consome: `Canal` (`dados/Entidades.kt`, já existe: `contaId`, `streamId`, `nome`,
  `categoriaId: Long?`, `icone`, `epgId`, `numero`).
- Produz:
  - `enum class Qualidade(val rotulo: String) { SD("SD"), HD("HD"), FHD("FHD"), QUATRO_K("4K") }`
    — ordem de declaração é da pior pra melhor (`Qualidade.entries` ordenado assim).
  - `fun chaveCategoria(categoriaId: Long?): Long` — normaliza `null` para `-1L` (Review
    Focus: chave de agrupamento nunca usa `Long?` cru).
  - `data class VarianteQualidade(val qualidade: Qualidade, val streamId: Long)`
  - `data class CanalAgrupado(val nomeBase: String, val categoriaId: Long, val icone: String?, val epgId: String?, val numero: Int?, val variantes: List<VarianteQualidade>)`
    — `variantes` sempre ordenada da **maior pra menor** qualidade.
  - `fun qualidadeDoNome(nome: String): Qualidade?`
  - `fun nomeBase(nome: String): String`
  - `fun agruparPorQualidade(canais: List<Canal>): List<CanalAgrupado>`

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `agrupa variantes do mesmo canal pelo nome-sem-sufixo e categoria`()
// 3 Canal sintéticos: "CANAL TESTE FHD", "CANAL TESTE HD", "CANAL TESTE SD", mesma categoriaId.
// agruparPorQualidade(...) -> 1 CanalAgrupado, nomeBase == "CANAL TESTE",
// variantes == [FHD, HD, SD] nessa ordem (maior pra menor).

@Test fun `canais com nome-base igual em categorias diferentes nao se misturam`()
// "CANAL X HD" categoria 1, "CANAL X HD" categoria 2 -> 2 CanalAgrupado distintos.

@Test fun `categoriaId nulo normaliza para -1 sem colidir com categoria real -1`()
// "CANAL Y HD" categoriaId = null e "CANAL Y HD" categoriaId = -1L (hipotético) ->
// terminam na mesma chave normalizada -1L -> 1 CanalAgrupado só (comportamento esperado e
// documentado, já que -1L é o sentinela).

@Test fun `canal sem sufixo de qualidade reconhecido vira grupo de 1 variante`()
// "CANAL SEM SUFIXO" (nenhum SD/HD/FHD/4K no fim) -> qualidadeDoNome(...) == null;
// agruparPorQualidade ainda produz 1 CanalAgrupado com nomeBase == nome inteiro e
// 1 variante (decisão: variante sem qualidade reconhecida entra como Qualidade.SD, pior
// caso, nunca quebra a função).

@Test fun `qualidadeDoNome reconhece os 4 sufixos, case-insensitive`()
// "X SD", "x hd", "X Fhd", "X 4K" -> SD, HD, FHD, QUATRO_K.
```

- [ ] **Passo 2: Rodar e ver falhar.** `./scripts/gradle.ps1 testDebugUnitTest --tests '*CanalAgrupadoTest'`
- [ ] **Passo 3: Implementar.** Regex para o sufixo:
  `Regex("""(?i)\s+(4K|FHD|HD|SD)$""")` — casa só o último token, sem ambiguidade entre "HD"
  e "FHD" porque a alternância precisa bater com o token inteiro até o fim da string.
  `agruparPorQualidade` agrupa por `Pair(nomeBase(c.nome), chaveCategoria(c.categoriaId))`,
  usa `icone`/`epgId`/`numero` de qualquer uma das variantes do grupo (são do mesmo canal
  lógico), e ordena `variantes` por `qualidade.ordinal` decrescente.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: agrupa canais por qualidade SD/HD/FHD/4K`

