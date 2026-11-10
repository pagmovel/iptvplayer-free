# IPTV Player Free

Player de IPTV para celular Android (Kotlin), com login Xtream. O APK é gerado pelo GitHub Actions.

- Design aprovado: [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](docs/superpowers/specs/2026-11-10-iptv-player-free-design.md)
- Esboços das telas: `.superpowers/brainstorm/`
- Plano atual: [docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md](docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md)

## Onde paramos (2026-11-10, pausado a pedido do dono)

- Execução do Plano 1 via **superpowers:subagent-driven-development**. **Concluídas e
  testadas: T1, T4, T5, T6, T7, T8** (7 de 11). Faltam T9, T10, T11.
- **T9 travada por um bloqueio externo real**, não por bug: o Maven Central devolve HTTP 429
  para um artefato do Robolectric (`org.robolectric:nativeruntime-dist-compat:1.0.19`), que
  T9/T10/T11 precisam de verdade (não só de passagem, como T6/T8 driblaram). Libera por volta
  de **2026-11-11 18:07 GMT (~15:07 em Brasília)** — é um limite fixo no tempo, não piora
  tentando de novo. Tentei 2 mirrors públicos que respondem 200 (`maven.aliyun.com`,
  `maven-central.storage-download.googleapis.com` — este último já adicionado em
  `settings.gradle.kts`), mas o Gradle não trocou de repositório para esse artefato
  específico. **Próxima sessão:** tentar de novo depois do horário acima; se continuar
  bloqueado, configurar `exclusiveContent`/`content { includeGroup("org.robolectric") }` no
  mirror em vez de só declará-lo como extra. Detalhe completo no ledger do SDD
  (`.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md`).
- Código da T9 (`carga/CargaCatalogo.kt`/`CargaCatalogoTest.kt`) está escrito e compila, mas
  **não foi comitado** — a suíte nunca rodou de fato (nem "ver falhar" nem "ver passar"), e a
  regra do projeto é não comitar código sem verificação real.
- Só há commits locais; **nada foi enviado ao GitHub**. Push e tag dependem do "sim" do dono na
  hora (T3 e T11 do plano).
- Fora do git: `listas.txt` (credenciais) e `temp/` (`coletar_amostras.py` e
  `temp/amostras/`, base de formato para os testes; nunca copiar os dados para testes
  versionados).

## Regras do projeto

- `listas.txt` (credenciais do dono) e `temp/` (amostras do provedor) **nunca** vão para o
  repositório, que é público.
- Testes versionados usam só dados sintéticos.
- A chave de assinatura fica fora do repositório e nos GitHub Secrets (ver design, §4).
- **Autoria dos commits (decisão do dono em 2026-11-10, só para este projeto):** a partir do
  commit `90e61b7`, autor e committer são `Marcos Ronaldo <marcos.ronaldo@gmail.com>` — não
  `marcossilva@rms.adv.br` (regra global) nem o e-mail do git config global desta máquina.
  Commits anteriores a esse não foram reescritos. Repositório sem `user.name`/`user.email`
  local: sempre setar as 4 variáveis de ambiente (`GIT_AUTHOR_NAME`/`_EMAIL`,
  `GIT_COMMITTER_NAME`/`_EMAIL`) no comando do commit, nunca mexer no `git config`.

## Plugins e skills usados

- **superpowers** (plugin): brainstorming, writing-plans e execução do plano.
- **context-mode** (plugin): leitura e execução com saída filtrada.
- **impeccable** (plugin): hook de qualidade visual. As exceções ficam em `.impeccable/config.json`.
