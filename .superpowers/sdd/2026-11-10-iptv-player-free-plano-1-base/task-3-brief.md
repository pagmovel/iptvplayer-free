### T3: Chave de assinatura, Secrets e primeira Release

Tarefa com ações do dono. Cada push e cada tag precisam do "sim" dele na hora.

**Arquivos:**
- Criar (fora do git): `temp/assinatura/iptv-player-free.jks`,
  `temp/assinatura/valores-secrets.txt`

- [ ] **Passo 1: Gerar a chave** com
  `& "C:\Program Files (x86)\Java\jre1.8.0_441\bin\keytool.exe" -genkeypair -v -keystore temp\assinatura\iptv-player-free.jks -alias iptv-player-free -keyalg RSA -keysize 4096 -validity 36500 -storetype PKCS12`.
  Gere senhas aleatórias fortes. Em PKCS12, a senha da chave é igual à do keystore. Grave em
  `valores-secrets.txt` os 4 nomes de Secret com os valores (keystore em base64 com
  `[Convert]::ToBase64String([IO.File]::ReadAllBytes(...))`).
- [ ] **Passo 2: Conferir que nada vazou.** `git status --short` não mostra nada de `temp/`, e
  `git check-ignore -v temp/assinatura/iptv-player-free.jks` mostra a regra `temp/`.
- [ ] **Passo 3: Guiar o dono no cadastro dos Secrets:** repositório → *Settings* → *Secrets and
  variables* → *Actions* → *New repository secret*, uma vez para cada um dos 4 nomes. Peça a ele
  que guarde `iptv-player-free.jks` e `valores-secrets.txt` fora do repositório (pendrive ou
  Drive). Explique que, sem eles, as atualizações deixam de instalar por cima.
- [ ] **Passo 4: Com o "sim" do dono,** faça `git push -u origin main`. Esperado: job `testes`
  verde no Actions.
- [ ] **Passo 5: Com o "sim" do dono,** crie e envie a tag `v0.0.1`. Esperado: Release `v0.0.1`
  com `iptv-player-free-0.0.1.apk`.
- [ ] **Passo 6: Validação pelo dono:** instalar a `v0.0.1` no celular. Depois, mude o texto da
  tela para "IPTV Player Free · v0.0.2", publique a `v0.0.2` (com "sim") e peça que ele instale
  **por cima**, sem desinstalar. Esperado: instala sem "conflito com pacote existente". Só então
  a Fase 0 vira `validada`.
- [ ] **Passo 7: Documentar.** Crie `.claude/skills/iptv-build-assinatura/SKILL.md` com o
  processo de chave e Secrets, o comando do Docker, as pegadinhas encontradas e os acertos.
  Commit `docs: skill de build e assinatura`.

---

## Fase 1 · Contas e carga de dados

Base dos pacotes: `app/src/main/java/io/github/pagmovel/iptvplayer/`; testes em
`app/src/test/java/io/github/pagmovel/iptvplayer/`. Todas as verificações rodam com
`./scripts/gradle.ps1 testDebugUnitTest --tests '<Classe>'`. O resultado esperado é a linha
`BUILD SUCCESSFUL`, ou a falha do teste novo no passo "ver falhar".

