### T2: Workflow do GitHub Actions (CI e Release assinada)

**Arquivos:**
- Criar: `.github/workflows/build.yml`
- Modificar: `app/build.gradle.kts` (assinatura e versão)

**Interfaces:**
- Consome: Secrets `ASSINATURA_KEYSTORE_BASE64`, `ASSINATURA_KEYSTORE_SENHA`,
  `ASSINATURA_ALIAS`, `ASSINATURA_ALIAS_SENHA` (cadastrados na T3).
- Produz: em todo push e pull request, `testDebugUnitTest` + `assembleDebug`. Em tag `v*`, os
  testes, `assembleRelease` assinado e uma Release do GitHub com o anexo
  `iptv-player-free-<versão>.apk`.

- [ ] **Passo 1: Assinatura e versão no Gradle.** `signingConfigs.release` lê as 4 variáveis de
  ambiente acima; o keystore é decodificado do base64 para `app/assinatura.jks` pelo workflow.
  Sem as variáveis, o `release` não assina, para o build local não quebrar. `versionName` = tag
  sem o `v` (variável `VERSAO_APP`; padrão `0.0.0-dev`). `versionCode` = `GITHUB_RUN_NUMBER`
  (padrão `1`). Adicione `app/assinatura.jks` e `*.jks` ao `.gitignore`.
- [ ] **Passo 2: Workflow.** Use JDK 17 com `actions/setup-java` e cache do Gradle. Os jobs
  `testes` (sempre) e `release` (`if: startsWith(github.ref, 'refs/tags/v')`, depende de
  `testes`) criam a Release com `gh release create "$GITHUB_REF_NAME" <apk> --generate-notes`.
  Use `permissions: contents: write` só no job de release. **Nunca** suba o keystore como
  artefato.
- [ ] **Passo 3: Verificar localmente** que o build sem as variáveis continua funcionando:
  `./scripts/gradle.ps1 testDebugUnitTest assembleDebug assembleRelease`. Esperado:
  `BUILD SUCCESSFUL`, com o APK release não assinado.
- [ ] **Passo 4: Commit.** `ci: build, testes e release assinada no GitHub Actions`.

