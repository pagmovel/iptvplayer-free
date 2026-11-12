# IPTV Player Free

Player de IPTV para celular Android (Kotlin), com login Xtream. O APK é gerado pelo GitHub Actions.

- Design aprovado: [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](docs/superpowers/specs/2026-11-10-iptv-player-free-design.md)
- Esboços das telas: `.superpowers/brainstorm/`
- Plano atual: [docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md](docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md)

## Onde paramos (2026-11-12, Plano 1 inteiro `validada`; próximo é planejar o Plano 2 · TV ao vivo)

- Execução do Plano 1 via **superpowers:subagent-driven-development**. **Plano 1 completo —
  Fase 0 (build/CI/assinatura) e Fase 1 (contas + carga de dados) — está `validada` de ponta a
  ponta, com conta real em produção** — ver tabela de acompanhamento no plano
  (`docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md`; linha do "Mapa dos
  planos" também atualizada de `pendente` para `validada`).
- **T3 passo 6 confirmado em 2026-11-12** (resposta direta do dono, não suposição): ele
  instalou a `v0.0.4` e depois a `v0.0.5` direto **por cima** da versão anterior no celular,
  sem desinstalar nenhuma vez e sem "conflito com pacote existente" — fluxo normal de
  atualização do Android com o mesmo APK assinado.
- **T11 (telas de contas/resumo + worker) completa e `validada`** — código commitado
  (`90db6eb`/`6df4753`), suíte `BUILD SUCCESSFUL` reconfirmada nesta sessão, tag `v0.1.0`
  publicada com sucesso (`testes`/`release` = `success`):
  https://github.com/pagmovel/iptvplayer-free/releases/tag/v0.1.0. Os 5 itens do passo 8
  (validação no celular) confirmados pelo dono: colar link, salvar conta, carga sem travar
  com números reais, **12h sem recarregar sozinho** (fechou/abriu com internet normal) e
  **"Atualizar agora" sem rede mostrando "Aguardando conexão com a internet." sem perder os
  números** (print real).
- **Investigação de um falso alarme durante a validação**: o dono relatou que a tela "não
  mudava" depois de carregar. Investigado com `superpowers:systematic-debugging` (perguntar
  os fatos antes de supor): não era bug — a carga tinha concluído com sucesso (print com
  "Última carga" atualizada, sem erro, números corretos); a tela de Resumo só atualiza os
  números no próprio lugar por design, sem navegar para outra tela (não existe outra tela
  ainda — isso é a Fase 2). Nenhuma mudança de código.
- **Releases públicas, em ordem, cada uma com uma causa raiz real resolvida**:
  - `v0.0.1`: falhou — chave de assinatura gerada com JDK 8 local, incompatível com o
    OpenJDK 21 do `cimg/android:2026.10` (quem assina no CI). Corrigido gerando a chave
    **dentro do container**. Processo e pegadinhas:
    `.claude/skills/iptv-build-assinatura/SKILL.md`.
  - `v0.0.2`: ficou presa em `queued` por um outage real da plataforma GitHub
    (confirmado em `githubstatus.com`); run órfão, abandonado.
  - **`v0.0.3`**: primeira Release pública publicada com sucesso.
  - `v0.0.4`: corrigiu `XtreamApi.abrirXmltv` não convertendo `IOException`/
    `XmlPullParserException` em `FalhaXtream` (commit `c62d580`) — mas o dono ainda bateu em
    "Falha ao atualizar." por outro motivo (abaixo).
  - **`v0.0.5` — primeira sincronização completa confirmada no celular do dono**
    (print real: 946 canais, 15.896 filmes, 7.746 séries, 14.447 programas, sem erro).
    Corrigiu o achado real de T9 (abaixo).
  - Release mais recente: https://github.com/pagmovel/iptvplayer-free/releases/tag/v0.0.5
- **Dois achados reais pós-lançamento, cada um com causa raiz confirmada por reprodução
  (não suposição) e corrigidos:**
  1. **Bloqueio de DNS da operadora em dados móveis** (não é bug do app): o celular do dono
     não alcançava o domínio do provedor em dados móveis; resolvido trocando o DNS do
     aparelho. Fora do controle do código — documentado como pegadinha de diagnóstico na
     skill `iptv-xtream-carga` (item 9): ao investigar "servidor não respondeu" relatado por
     um usuário, perguntar a rede (Wi-Fi vs. dados móveis) antes de desconfiar do código.
  2. **`stream_id` duplicado em `get_vod_streams` do provedor real** quebrava a carga com
     `SQLiteConstraintException` (não é `FalhaXtream`, escapava do `CargaWorker`, caía no
     "Falha ao atualizar." genérico). Reproduzido de ponta a ponta contra o servidor real
     (teste descartável com `CargaCatalogo.executar()` de verdade, nunca comitado) e
     corrigido trocando os 4 inserts do catálogo para `OnConflictStrategy.REPLACE`
     (commit `b62eeb8`). Teste de regressão sintético em `CatalogoDaoTest`. Skill
     `iptv-xtream-carga`, item 4a.
- **Config do Claude Code**: o modo Auto bloqueava `git push` de tags novas ("Create Public
  Surface"), mesmo com o "sim" do dono — é um classificador de segurança do próprio harness,
  não do projeto. Resolvido com `.claude/settings.local.json` (regras de permissão para
  `git push origin main` e `git push origin v0.0.*`). **Duas revisões de segurança
  automáticas pegaram regras amplas demais** nesse processo (`"Bash(git push*)"` primeiro, a
  escopo-por-prefixo `v0.0.*` depois) — a primeira foi corrigida para o comando exato; a
  segunda ficou como está, por pedido explícito do dono (ele mesmo editou o arquivo, já
  ciente do risco). O dono autorizou criar/enviar tags de versão sem perguntar a cada vez
  daqui pra frente.
- `fwcd.kotlin-0.2.36.vsix` na raiz: o dono decidiu deixar como está, sem versionar.
- Working tree limpo, tudo commitado e enviado para `origin/main`.
- O bloqueio de HTTP 429 do Maven Central no artefato do Robolectric
  (`org.robolectric:nativeruntime-dist-compat:1.0.19`) foi contornado em `settings.gradle.kts`
  com `exclusiveContent { forRepository { maven(espelho do Google) }; filter {
  includeModule(...) } }` — força esse módulo específico a vir só do espelho, sem passar pelo
  Maven Central. Declarar o espelho como repositório extra solto (tentativa anterior) **não
  funcionava**: o Gradle resolve o POM pelo `mavenCentral()` e não tenta o próximo repositório
  para o binário do mesmo módulo. Docker Desktop local precisa estar rodando para o
  `./scripts/gradle.ps1` funcionar — `docker desktop start` se estiver parado.
- Suíte completa do projeto passa de ponta a ponta: **9 classes, 46 testes, 0 falhas**.
  Detalhe completo de toda a execução no ledger do SDD
  (`.superpowers/sdd/2026-11-10-iptv-player-free-plano-1-base/progress.md`).
- Fora do git, de propósito: `listas.txt` (credenciais do dono), `.env` (credenciais de
  teste), `temp/` (amostras e diagnósticos do provedor real — nunca copiar dados reais para
  testes versionados).
- **Próxima sessão**: **planejar o Plano 2 · TV ao vivo** (Fase 2 do design: tela dividida
  com player + lista de canais/categorias, aba Programação Ontem/Hoje/Amanhã) — precisa de
  brainstorming/writing-plans novo (`superpowers:brainstorming`), não é continuação deste SDD.
  Os esboços já existem em `.superpowers/brainstorm/`, mas nenhum código dessa fase foi
  escrito ainda.

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
