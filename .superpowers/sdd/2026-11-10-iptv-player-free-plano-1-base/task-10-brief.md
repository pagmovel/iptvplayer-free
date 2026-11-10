### T10: Contas: cofre de senha e repositório

**Arquivos:**
- Criar: `conta/Cofre.kt`, `conta/ContaDao.kt`, `conta/ContasRepositorio.kt`
- Teste: `conta/ContasRepositorioTest.kt` (Robolectric, com `Cofre` falso)

**Interfaces:**
- Produz: `interface Cofre { fun cifrar(texto: String): ByteArray; fun decifrar(dados: ByteArray): String }`
  e `class CofreKeystore : Cofre` (AES/GCM/NoPadding, chave `iptv-player-free-senhas` no
  `AndroidKeyStore`, IV de 12 bytes prefixado ao resultado). A interface existe porque o
  Robolectric não emula o `AndroidKeyStore`. O falso dos testes é a segunda implementação.
- Produz: `ContaDao` com `inserir`, `atualizar`, `apagar`, `listar(): Flow<List<Conta>>`,
  `ativa(): Conta?` e `marcarCarga(id, quando)`.
- Produz: `class ContasRepositorio(private val banco: BancoApp, private val cofre: Cofre, private val criarApi: (CredenciaisXtream) -> XtreamApi)`, com:
  - `suspend fun testarESalvar(nome: String, cred: CredenciaisXtream): Result<Conta>`, que chama
    `autenticar()` e só salva se der certo. A primeira conta salva fica ativa;
  - `suspend fun ativar(id: Long)`, que deixa exatamente uma ativa;
  - `suspend fun apagar(id: Long)`, que apaga catálogo e programação da conta;
  - `suspend fun credenciais(conta: Conta): CredenciaisXtream`;
  - `suspend fun precisaAtualizar(conta: Conta, agoraMs: Long): Boolean`, verdadeiro se
    `ultimaCargaEm` for `null` ou tiver mais de 12 h.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `senha nunca e gravada em texto puro`()     // bytes de conta.senhaCifrada não contêm "s3nh4"
@Test fun `credencial invalida nao salva conta`()       // MockWebServer auth 0 → Result.failure; listar() vazio
@Test fun `primeira conta fica ativa e ativar troca`()  // depois de ativar(b): só b.ativa
@Test fun `apagar conta apaga o catalogo dela`()
@Test fun `precisa atualizar depois de 12 horas`()       // null → true; 11h59 → false; 12h01 → true
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.** Coloque o `ContaDao` no `BancoApp` (ainda na versão 1, porque
  nada foi publicado com dados).
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit.** `feat: contas com senha cifrada e troca de conta ativa`.

