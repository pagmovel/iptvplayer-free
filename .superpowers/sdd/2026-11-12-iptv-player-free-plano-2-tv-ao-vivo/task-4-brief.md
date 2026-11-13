### T4: Configurações — tempo de buffer

**Arquivos:**
- Criar: `configuracoes/ConfiguracoesRepositorio.kt`
- Teste: `configuracoes/ConfiguracoesRepositorioTest.kt`

**Interfaces:**
- Produz: `class ConfiguracoesRepositorio(private val prefs: SharedPreferences)` com
  `fun tempoBufferMs(): Long` (padrão **8000L**) e `fun definirTempoBufferSegundos(segundos: Int)`.
  *(Decisão minha: `SharedPreferences`, não Room — é 1 valor só, sem necessidade de migração
  nem de consulta reativa entre telas; `AppIptv` ganha `val configuracoes: ConfiguracoesRepositorio by lazy { ConfiguracoesRepositorio(getSharedPreferences("config", MODE_PRIVATE)) }`.)*

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `tempoBufferMs comeca em 8000 (8 segundos) por padrao`()
@Test fun `definirTempoBufferSegundos muda o valor lido depois`()
```

- [ ] **Passo 2: Rodar e ver falhar.** (Robolectric: `SharedPreferences` real do Android, roda
  igual aos outros testes deste projeto.)
- [ ] **Passo 3: Implementar.**
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: configuracao do tempo de buffer antes da troca de qualidade`

