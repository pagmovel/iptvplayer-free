### T11: Telas de contas e resumo, worker e Release v0.1.0

**Arquivos:**
- Criar: `carga/CargaWorker.kt`, `ui/Navegacao.kt`, `ui/contas/ContasTela.kt`,
  `ui/contas/ContasViewModel.kt`, `ui/resumo/ResumoTela.kt`, `ui/resumo/ResumoViewModel.kt`,
  `ui/Tema.kt`, `AppIptv.kt` (Application, montagem manual das dependências, sem framework de DI)
- Modificar: `ui/MainActivity.kt`, `AndroidManifest.xml`
- Teste: `carga/CargaWorkerTest.kt` (`TestListenableWorkerBuilder`, Robolectric)

**Interfaces:**
- Consome: `ContasRepositorio` (T10), `CargaCatalogo` (T9), `lerLinkXtream` (T4).
- Produz: `CargaWorker` (`CoroutineWorker`), com entrada `contaId`, progresso `etapa` (nome do
  `EtapaCarga`) e falha com `mensagem` em pt-BR. Ele é enfileirado como trabalho único por conta
  (`ExistingWorkPolicy.KEEP`) e exige rede conectada. No sucesso, chama `marcarCarga`.

- [ ] **Passo 1: Escrever os testes que falham.**

```kotlin
@Test fun `worker grava o catalogo e marca a carga`()      // Result.success(); ultimaCargaEm != null
@Test fun `worker traduz falha em mensagem`()               // auth 0 → Result.failure com mensagem "Usuário ou senha incorretos"
```

  Mensagens *(decisão do plano)*: `CredenciaisInvalidas` → "Usuário ou senha incorretos";
  `ContaInativa` → "Conta do provedor inativa ou vencida (<status>)"; `ServidorIndisponivel` →
  "Servidor não respondeu. Tente outro endereço do seu provedor."; `RespostaInvalida` → "O
  servidor respondeu algo inesperado."
- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar o worker.**
- [ ] **Passo 4: Implementar as telas.**
  - **Contas:** lista das contas salvas, com a ativa marcada. Botão "Adicionar conta" abre um
    formulário com Nome, Servidor, Usuário e Senha, e com o botão **"Colar link do provedor"**,
    que lê a área de transferência com `lerLinkXtream` e preenche os 3 campos; se o link não
    servir, mostra "Link não reconhecido". "Salvar" chama `testarESalvar` com um indicador de
    carregamento e mostra o erro traduzido. Ações por conta: ativar, editar (trocar servidor) e
    apagar, com confirmação.
  - **Resumo** (tela inicial provisória, que o Plano 3 troca pela vitrine): nome da conta ativa;
    as contagens de canais, filmes, séries e programas; a etapa atual da carga; a data da última
    carga; e o botão "Atualizar agora". Sem conta, abre direto o formulário.
  - Ao abrir o app, se `precisaAtualizar` for verdadeiro, enfileira o `CargaWorker`.
  - Use o tema escuro do Material 3, coerente com os esboços (fundo `#0f1115`, destaque
    `#4fc3f7`), com textos de pelo menos 14 sp.
- [ ] **Passo 5: Rodar tudo.** `./scripts/gradle.ps1 testDebugUnitTest assembleDebug`. Esperado:
  `BUILD SUCCESSFUL`.
- [ ] **Passo 6: Commit.** `feat: telas de contas e resumo com carga em segundo plano`.
- [ ] **Passo 7: Com o "sim" do dono,** faça push e publique a tag `v0.1.0`.
- [ ] **Passo 8: Validação pelo dono no celular,** atualizando por cima da `v0.0.2`:
  1. Colar um dos links de `listas.txt`: os campos são preenchidos.
  2. Salvar: a conta é aceita.
  3. Esperar a carga: ela termina sem travar a tela, e o resumo mostra números próximos de 946
     canais, 16.122 filmes e 8.714 séries (§3.1; o provedor muda isso com o tempo).
  4. Fechar e abrir: nada recarrega antes de 12 h.
  5. "Atualizar agora" com o Wi-Fi desligado: aparece mensagem, e os números continuam.
- [ ] **Passo 9: Documentar.** Crie `.claude/skills/iptv-xtream-carga/SKILL.md` com o formato
  real da API, as pegadinhas (temporadas vazias, `get.php` redirecionando, tipos inconsistentes,
  ids de EPG repetidos), os acertos e como validar. Atualize as tabelas de acompanhamento deste
  plano e a §6 do design. Commit `docs: skill da API Xtream e da carga`.
