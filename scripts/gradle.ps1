# Roda o Gradle do projeto dentro do container cimg/android:2026.10, porque esta máquina
# Windows não tem SDK Android nem Kotlin instalados. Usado por todas as tarefas para verificar.
#
# Uso: .\scripts\gradle.ps1 <tarefas...>
# Ex.:  .\scripts\gradle.ps1 testDebugUnitTest assembleDebug

# --user root: o volume nomeado do cache é criado com dono root pelo Docker; o usuário
# padrão da imagem (circleci) não tem permissão de escrita nele até alguém ajustar o dono
# manualmente. Rodar como root evita esse passo de setup em qualquer máquina nova.
# GRADLE_USER_HOME: como root, o $HOME efetivo é /root, não /home/circleci — sem isso o
# Gradle ignora o volume montado e baixa a distribuição e as dependências do zero em toda
# chamada (achado real da T6: isso já esgotou o limite de requisições do Maven Central).
docker run --rm `
    --user root `
    -e GRADLE_USER_HOME=/home/circleci/.gradle `
    -v "${PWD}:/projeto" `
    -v iptv-gradle-cache:/home/circleci/.gradle `
    -w /projeto `
    cimg/android:2026.10 `
    bash -lc "chmod +x ./gradlew; ./gradlew $($args -join ' ') --no-daemon"

exit $LASTEXITCODE
