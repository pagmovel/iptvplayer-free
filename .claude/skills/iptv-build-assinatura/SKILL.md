---
name: iptv-build-assinatura
description: Como gerar/renovar a chave de assinatura do release e os 4 Secrets do GitHub Actions deste projeto. Use antes de gerar ou trocar a chave de assinatura, ou antes de depurar uma falha do job `release` no Actions.
---

# Chave de assinatura e Secrets do release

## Quando usar esta skill

Antes de gerar ou renovar o keystore de assinatura (`*.jks`), antes de mexer no
`signingConfigs` do `app/build.gradle.kts`, ou antes de investigar uma falha do job
`release` em `.github/workflows/build.yml`.

## Processo correto

A chave e os 4 Secrets são gerados **dentro do mesmo container usado para assinar no CI**
(`cimg/android:2026.10`, o mesmo do `scripts/gradle.ps1`), nunca com um JDK local:

```powershell
docker run --rm `
    -e SENHA="<senha aleatoria forte>" `
    -e DNAME="CN=IPTVPlayerFree,OU=Dev,O=Independente,L=Desconhecido,ST=Desconhecido,C=BR" `
    -v "<pasta de saida>:/saida" `
    cimg/android:2026.10 `
    bash -c 'keytool -genkeypair -v -keystore /saida/iptv-player-free.jks -alias iptv-player-free -keyalg RSA -keysize 4096 -validity 36500 -storetype PKCS12 -storepass "$SENHA" -keypass "$SENHA" -dname "$DNAME"'
```

Depois, codifique o `.jks` em base64 (`[Convert]::ToBase64String([IO.File]::ReadAllBytes(...))`)
e cadastre/atualize os 4 Secrets no GitHub (`Settings` → `Secrets and variables` → `Actions`):
`ASSINATURA_KEYSTORE_BASE64`, `ASSINATURA_KEYSTORE_SENHA`, `ASSINATURA_ALIAS`,
`ASSINATURA_ALIAS_SENHA`. Em PKCS12, a senha da chave é igual à do keystore.

**Antes de cadastrar os Secrets, verifique localmente** que o keystore assina de verdade,
simulando o job `release` (com `--user root` só para contornar permissão do bind mount no
Windows — ver pegadinha 2 abaixo; isso não existe no runner do GitHub):

```powershell
docker run --rm --user root -e GRADLE_USER_HOME=/home/circleci/.gradle `
    -e ASSINATURA_KEYSTORE_SENHA="$senha" -e ASSINATURA_ALIAS="iptv-player-free" -e ASSINATURA_ALIAS_SENHA="$senha" `
    -v "${PWD}:/projeto" -v iptv-gradle-cache:/home/circleci/.gradle -w /projeto `
    cimg/android:2026.10 bash -lc "./gradlew assembleRelease --no-daemon"
```

Esperado: `BUILD SUCCESSFUL` e `app/build/outputs/apk/release/app-release.apk` (sem "-unsigned"
no nome — esse é o primeiro sinal de que assinou). Para confirmar de verdade, não só pelo
nome do arquivo:

```bash
/home/circleci/android-sdk/build-tools/37.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## Pegadinhas confirmadas

1. **Nunca gere o keystore com um JDK diferente do que vai assiná-lo depois.** Na primeira
   tentativa (2026-11-11), gerei a chave com o JDK 8 local da máquina
   (`C:\Program Files (x86)\Java\jre1.8.0_441`, sugerido por um plano escrito antes de eu
   conferir qual JDK o container realmente usa) e ela foi cadastrada nos 4 Secrets. A tag
   `v0.0.1` disparou o job `release` no GitHub Actions e ele falhou de verdade (log real,
   não suposição) com:
   ```
   com.android.ide.common.signing.KeytoolException: Failed to read key *** from store
   "/projeto/app/assinatura.jks": Get Key failed: getSecretKey failed: Password is not ASCII
   ```
   mesmo a senha sendo só hexadecimal (`0-9a-f`, ASCII puro) — não é problema de caractere
   especial na senha, é o **JDK 8 (Oracle, legado) codificando o PKCS12 de um jeito que o
   OpenJDK 21 de dentro do `cimg/android:2026.10` (o que assina no CI) não lê igual**.
   Confirmado reproduzindo local: gerar e assinar com o **mesmo** JDK 21 do container
   funciona (`BUILD SUCCESSFUL`, `apksigner verify` confirma o certificado); gerar com JDK 8
   e assinar com JDK 21 reproduz exatamente o erro do CI. **Acerto:** sempre gerar a chave
   com `docker run ... cimg/android:2026.10 ... keytool ...` (comando acima), nunca com um
   `keytool.exe` instalado na máquina local, mesmo que mais rápido/conveniente.
2. **Bind mount do Docker Desktop no Windows: `chmod`/permissão de arquivo não se comporta
   como no runner Linux do GitHub Actions.** Rodar o container **sem** `--user root` contra
   uma pasta do projeto recém-clonada no Windows falha com `chmod: changing permissions of
   './gradlew': Operation not permitted` (arquivo novo, bit de execução nunca setado). Numa
   pasta onde o `gradlew` já tinha sido tornado executável por uma rodada anterior **como
   root**, o mesmo comando sem `--user root` passa do chmod mas falha depois com
   `Permission denied` num lock file dentro de `.gradle/` (o `.gradle/` ficou com dono
   `root`). **Isso é uma particularidade desta máquina Windows, não do runner real** — no
   GitHub Actions (Linux nativo, checkout limpo a cada job) o job `testes` roda sem
   `--user root` e passa normalmente. Não confunda uma falha de verificação local (Windows)
   com uma falha real do CI (Linux) — sempre que possível, confira o log real do Actions
   antes de concluir a causa.
3. **Logs do Actions pela API exigem autenticação mesmo em repositório público**
   (`GET /repos/.../actions/jobs/{id}/logs` devolve 403 sem token, não há `gh` CLI nesta
   máquina). Quando precisar do log de verdade de um job, peça para o dono abrir
   `https://github.com/<owner>/<repo>/actions/runs/<id>/job/<id>` no navegador dele e colar o
   texto — é mais rápido e confiável do que tentar reproduzir localmente às cegas.
4. O status de `/actions/runs` e `/actions/runs/{id}/jobs` (sem `/logs`) funciona sem token
   para repositório público — dá para ver `status`/`conclusion` por job e por run.
