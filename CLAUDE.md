# IPTV Player Free

Player de IPTV para celular Android (Kotlin), com login Xtream. O APK é gerado pelo GitHub Actions.

- Design aprovado: [docs/superpowers/specs/2026-11-10-iptv-player-free-design.md](docs/superpowers/specs/2026-11-10-iptv-player-free-design.md)
- Esboços das telas: `.superpowers/brainstorm/`
- Plano atual: [docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md](docs/superpowers/plans/2026-11-10-iptv-player-free-plano-1-base.md)

## Onde paramos (2026-11-10)

- Design e Plano 1 aprovados pelo dono. Execução em andamento via
  **superpowers:subagent-driven-development**, tarefa por tarefa (T1…T11 do Plano 1).
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

## Plugins e skills usados

- **superpowers** (plugin): brainstorming, writing-plans e execução do plano.
- **context-mode** (plugin): leitura e execução com saída filtrada.
- **impeccable** (plugin): hook de qualidade visual. As exceções ficam em `.impeccable/config.json`.
