# T4 Report: Configurações — tempo de buffer

## Implementação

### Arquivos criados:
- `app/src/main/java/io/github/pagmovel/iptvplayer/configuracoes/ConfiguracoesRepositorio.kt`
  - Classe wrapper de `SharedPreferences`
  - `tempoBufferMs(): Long` — padrão 8000L
  - `definirTempoBufferSegundos(segundos: Int)` — converte para ms

- `app/src/test/java/io/github/pagmovel/iptvplayer/configuracoes/ConfiguracoesRepositorioTest.kt`
  - 2 testes com Robolectric real (não mock)
  - Setup com `RuntimeEnvironment.getApplication()` e `getSharedPreferences("config", 0)`

### Arquivo modificado:
- `app/src/main/java/io/github/pagmovel/iptvplayer/AppIptv.kt`
  - Adicionada importação de `ConfiguracoesRepositorio`
  - Adicionada propriedade: `val configuracoes: ConfiguracoesRepositorio by lazy { ConfiguracoesRepositorio(getSharedPreferences("config", MODE_PRIVATE)) }`
  - Segue padrão de DI manual já existente no projeto

## Testes

### Fase RED:
- Testes falharam com `Unresolved reference 'ConfiguracoesRepositorio'` (esperado)

### Fase GREEN:
- `tempoBufferMs comeca em 8000 (8 segundos) por padrao` ✓ (7.174s)
- `definirTempoBufferSegundos muda o valor lido depois` ✓ (0.1s)

### Suíte completa:
Comando: `./scripts/gradle.ps1 testDebugUnitTest`
Resultado: BUILD SUCCESSFUL in 2m 58s

**Contagem real de testes (soma dos XML):**
- TEST-io.github.pagmovel.iptvplayer.aovivo.CanalAgrupadoTest.xml: 5
- TEST-io.github.pagmovel.iptvplayer.carga.CargaCatalogoTest.xml: 4
- TEST-io.github.pagmovel.iptvplayer.carga.CargaWorkerTest.xml: 2
- TEST-io.github.pagmovel.iptvplayer.configuracoes.ConfiguracoesRepositorioTest.xml: 2
- TEST-io.github.pagmovel.iptvplayer.conta.ContasRepositorioTest.xml: 5
- TEST-io.github.pagmovel.iptvplayer.conta.LinkXtreamTest.xml: 4
- TEST-io.github.pagmovel.iptvplayer.dados.CatalogoDaoTest.xml: 10
- TEST-io.github.pagmovel.iptvplayer.dados.MigracaoBancoTest.xml: 1
- TEST-io.github.pagmovel.iptvplayer.EsqueletoTest.xml: 1
- TEST-io.github.pagmovel.iptvplayer.programacao.LeitorXmltvTest.xml: 7
- TEST-io.github.pagmovel.iptvplayer.xtream.MapeamentoTest.xml: 7
- TEST-io.github.pagmovel.iptvplayer.xtream.XtreamApiTest.xml: 10

**TOTAL: 58 testes, 0 falhas, 0 erros**

## Autorrevisão

- ✓ Implementação exata do brief
- ✓ Padrão de DI manual mantido (lazy)
- ✓ Testes com Robolectric real (não mock)
- ✓ Conversão de segundos → ms funcionando em ambos os testes
- ✓ Padrão de teste limpo (setup limpa SharedPreferences antes de cada teste)
- ✓ Suíte completa rodada sem filtro `--tests`
- ✓ Commit com autoria correta

## Arquivos alterados
- Modified: `app/src/main/java/io/github/pagmovel/iptvplayer/AppIptv.kt` (1 propriedade + 1 import)
- Created: `app/src/main/java/io/github/pagmovel/iptvplayer/configuracoes/ConfiguracoesRepositorio.kt` (15 linhas)
- Created: `app/src/test/java/io/github/pagmovel/iptvplayer/configuracoes/ConfiguracoesRepositorioTest.kt` (30 linhas)

## Commit
SHA: c76c57c
Autoria: Marcos Ronaldo <marcos.ronaldo@gmail.com>
Mensagem: `feat: configuracao do tempo de buffer antes da troca de qualidade`

## Status
✓ Testes passando
✓ Suíte completa: 58 tests, 0 failures, 0 errors
✓ Commitado
