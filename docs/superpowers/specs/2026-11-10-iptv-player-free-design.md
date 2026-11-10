# IPTV Player Free — design

Data: 2026-11-10 · Repositório: `github.com/pagmovel/iptvplayer-free` (público)

## 1. Intenção e motivo

Um player de IPTV próprio para celular Android, com as funções típicas desse tipo de app:
login no provedor, TV ao vivo, filmes, séries e programação. Nome e visual são próprios. Não
copia marca, ícones nem telas de nenhum app existente.

O dono do projeto já usa apps de IPTV prontos e resolveu fazer o seu porque (palavras dele):
os apps atuais têm **limitações de uso**, **telas ruins**, **buscas que consomem muita memória,
travam o app na TV e deixam o celular lento**, e não deixam **adicionar mais recursos**.

O que define o sucesso:

- A busca e a navegação continuam rápidas mesmo com listas enormes, com dezenas de milhares de
  canais, filmes e episódios. O app nunca trava por causa do tamanho da lista. *(meta minha,
  derivada da reclamação dele)*
- As telas são agradáveis de usar no celular.
- O código é uma base que dá para ampliar com recursos novos depois.

## 2. Objetivos, não objetivos e anti-objetivos

**Objetivos (esta versão):**

- Login **só por Xtream Codes** (servidor + usuário + senha). O dono disse: "eu não uso [M3U],
  eu só conecto com usuário e senha no app".
- Na tela de login, o usuário pode **colar um link do provedor** (`http://servidor/get.php?
  username=…&password=…`), e o app preenche os três campos sozinho. Não é login M3U: o app só
  extrai do link os dados do Xtream. *(decisão minha: o provedor manda os acessos nesse formato,
  e digitar a senha no celular é chato)*
- Seções: Início, TV ao vivo, Filmes, Séries e Busca.
- Programação (EPG) dos canais ao vivo.
- Favoritos e "continuar assistindo" (retomar filme ou episódio de onde parou).
- Várias contas salvas no app, com uma ativa por vez.
- Distribuição por APK para família e amigos, com aviso de versão nova.

**Não objetivos (ficam para depois):**

- Versão para TV. A TV do dono é **LG, que roda webOS, não Android**. Será outro app, que
  reaproveita o design e as regras, mas não o código.
- Recursos extras além do básico. O dono pediu explicitamente: "deixar recursos para depois,
  vamos primeiro criar o app".
- Catch-up (assistir a um programa que já passou), gravação, controle parental, Chromecast,
  legendas externas, grade completa canais × horários.
- **Login por lista M3U.** O dono não usa, e o provedor dele bloqueia o download (§3.1).
- Vários servidores por conta, com troca automática quando um cai. O provedor dá 4 endereços
  para a mesma conta. Por ora, o usuário escolhe um e pode editar a conta para trocar.
- Publicação na Play Store.
- Servidor ou backend próprio.

**Anti-objetivos (o que faria o projeto fracassar, mesmo funcionando):**

- Carregar a lista inteira na memória para buscar ou filtrar. É o defeito que motivou o projeto.
- Travar a interface durante o download ou o processamento da lista ou da programação.
- Atualização que apaga contas, favoritos ou progresso (ver assinatura em §4).
- Qualquer senha, conta ou URL de provedor no repositório, que é público.

## 3. Restrições

- **Plataforma:** celular Android, com a tela em pé como uso principal.
- **Sem ambiente local de build:** a máquina do dono não tem SDK do Android nem Kotlin. Ele
  também não programa em Kotlin. Todo build acontece no **GitHub Actions**.
- **Sem emulador:** o teste real é no celular do dono, com o APK baixado do GitHub. O ciclo é:
  envio do código → build no Actions → download → instalação → retorno do dono. Por isso, a
  lógica que não depende de tela (cliente Xtream, parser da programação, leitura do link colado, busca)
  precisa de testes automatizados que rodem no próprio Actions.
- **Repositório público:** nada sensível no código. A chave de assinatura fica em GitHub
  Secrets.
- **Idioma:** interface, commits, comentários e documentação em pt-BR.

## 3.1 Dados reais do provedor do dono (coletados em 2026-11-10)

Coleta feita só com leitura, pelo script `temp/coletar_amostras.py`, a partir de
`listas.txt`. Esse arquivo tem as credenciais e está no `.gitignore`. As amostras, sem usuário
e senha, ficam em `temp/amostras/`, fora do repositório. Não versionar.

- O provedor entrega **4 endereços de servidor** (DNS alternativos) com **o mesmo usuário e
  senha**. A **API Xtream (`player_api.php`) respondeu nos 4**.
- Conta: `max_connections = 2`; formatos permitidos: `m3u8`, `ts`, `rtmp`. Servidor XUI.
- Volume: **39 categorias ao vivo / 946 canais**; **76 categorias de filmes / 16.122 filmes**
  (6,3 MB de JSON); **39 categorias de séries / 8.714 séries** (7,6 MB de JSON).
- Programação: `xmltv.php` com **8,2 MB**. Os horários vêm em UTC (`+0000`) e há atributos
  `start_timestamp`/`stop_timestamp`. Os canais se ligam pelo `epg_channel_id` do canal e
  **há ids de canal repetidos** no XMLTV (ex.: versões FHD e HD com o mesmo id).
- `get_short_epg` voltou vazio no primeiro canal. Alguns canais **não têm programação**.
- `get_series_info` voltou com **`seasons` vazio**, mas com episódios em `episodes["1"]`. As
  temporadas precisam ser montadas a partir das chaves de `episodes`, sem confiar em `seasons`.
- Os canais têm `tv_archive`/`tv_archive_duration`, que é a base de um catch-up futuro.
- **Os 8 links M3U (`get.php`, TS e HLS, nos 4 servidores) respondem `302` para
  `www.google.com`**, com qualquer User-Agent testado (navegador, VLC, okhttp). O download da
  lista M3U parece bloqueado pelo provedor. **[INCONCLUSIVO]**: testado só a partir da rede
  desta máquina, não do celular.
- Tudo é servido em `http` (sem TLS). O app precisa permitir tráfego em texto claro.

## 4. Decisões já tomadas

### Tecnologia *(decisão minha; o dono delegou)*

- App nativo em **Kotlin**, com interface em **Jetpack Compose**.
- Player **Media3 / ExoPlayer**, por suportar bem os formatos comuns de IPTV (HLS e MPEG-TS).
- **Banco local SQLite (via Room) com índice de texto (FTS)** para canais, filmes, séries,
  episódios e programação. Toda lista e toda busca consultam o banco com paginação. Nada de
  lista inteira em memória.
- Download e processamento da lista e da programação **em segundo plano e em fluxo**: ler aos
  poucos e gravar em lotes, sem montar o arquivo inteiro na memória. O arquivo de programação
  pode ter centenas de MB.
- Capas carregadas sob demanda, só quando aparecem na tela, e guardadas em cache no aparelho.

### Build e distribuição *(dono decidiu: GitHub; repositório público)*

- GitHub Actions compila, roda os testes e gera o APK.
- APK de release **assinado sempre com a mesma chave**, guardada em GitHub Secrets. Assim a
  atualização instala por cima e preserva os dados.
- **Geração da chave** *(decisão minha)*: feita uma única vez na máquina do dono, com o
  `keytool` do Java 8 já instalado (`C:\Program Files (x86)\Java\jre1.8.0_441\bin\keytool.exe`,
  fora do PATH). Não precisa de container. **Nunca** gerar pelo Actions: chave nova a cada
  build quebra as atualizações, e chave publicada como artefato fica exposta num repositório
  público.
- **Cadastro nos Secrets:** o dono não conhece o processo, então quem constrói o guia na
  Fase 0. Um arquivo local fora do repositório traz os 4 valores prontos (keystore em base64,
  senha do keystore, alias, senha do alias), e o dono os cola em *Settings → Secrets and
  variables → Actions → New repository secret*. Alternativa: se o dono instalar e autenticar o
  `gh`, quem constrói cadastra os valores. O dono guarda uma cópia de segurança do keystore fora
  do repositório. Se a chave se perder, as atualizações deixam de instalar por cima.
- **Alvo mínimo** *(decisão minha)*: Android 8 (API 26). A programação guardada vai só de
  ontem até depois de amanhã, para limitar o espaço ocupado com várias contas.
- A versão sai na página de **Releases** do repositório. É de lá que família e amigos baixam.
- O app consulta as Releases públicas para avisar que há versão nova e abre o link de download.
  *(Esse comportamento é palpite meu e foi apresentado ao dono, que não o recusou.)*
- Instalação fora da Play Store: o usuário permite "fontes desconhecidas" uma vez.

### Contas e dados

- Cada pessoa entra com a própria conta, no próprio celular. Não há servidor nosso.
- Senhas guardadas criptografadas no aparelho.
- Várias contas salvas, uma ativa. *(decisão minha)*

### Navegação *(dono escolheu: "C · Vitrine com fileiras")*

- Barra inferior com quatro abas: **Início · Ao vivo · Filmes · Séries**.
- **Busca na lupa do topo**, disponível em todas as telas. *(decisão minha; o dono pode mudar)*
- **Início:** um item em destaque e fileiras horizontais "Continuar assistindo", "Canais
  favoritos" e "Filmes adicionados recentemente".
- Quando um item não tem capa, o app mostra o nome sobre um fundo neutro.

### Busca *(dono escolheu: "B · Lista única com filtro")*

- Um só campo procura em canais, filmes e séries ao mesmo tempo, com resultados aparecendo
  enquanto se digita.
- Lista única. Cada item leva uma etiqueta (AO VIVO / FILME / SÉRIE). Botões no topo filtram por
  tipo (Tudo / Canais / Filmes / Séries).
- Mostra o total de resultados e, nos canais, o programa que está passando agora.

### TV ao vivo *(dono escolheu: "A · Tela dividida", com programação obrigatória)*

- Celular em pé: **vídeo em cima**; logo abaixo, o programa atual com horário e barra de
  progresso; embaixo, duas abas:
  - **Canais:** lista com filtro por categoria (incluindo Favoritos e Todos) e o programa atual
    de cada canal. Um toque troca o canal sem sair da tela.
  - **Programação:** o dia inteiro do canal atual, com seletor Ontem / Hoje / Amanhã. *(Dono
    escolheu "A · Programação do canal, abaixo do vídeo".)* "Ontem" serve só para consulta.
- Celular deitado: vídeo em tela cheia.
- Canal sem programação: o app mostra "Sem programação" em vez de ficar vazio ou travar.

### Filmes e séries *(decisão minha, apresentada ao dono)*

- Telas de lista por categoria, no mesmo padrão de vitrine e de lista.
- Detalhes: capa, sinopse e botão Assistir. Nas séries, seletor de temporada e lista de
  episódios.
- O player salva o progresso e retoma de onde parou.

Os esboços aprovados estão em `.superpowers/brainstorm/1394-1794316701/content/`
(`navegacao.html`, `busca.html`, `player-aovivo.html`, `grade.html`).

## 5. Deixado para quem for construir

- Identidade visual: paleta, tipografia e ícone. Tema escuro como base, coerente com os
  esboços. Nada que lembre outro app de IPTV.
- Nome do pacote Android e arquitetura interna (módulos, camadas, injeção de dependência).
- Frequência e gatilho de atualização da lista e da programação (ex.: ao abrir, se tiver mais
  de X horas, e por um botão manual).
- Comportamento quando o stream falha (tentar de novo, mensagem).
- Formatos da API Xtream e do XMLTV: confirmados com o provedor real (§3.1). As amostras
  anonimizadas em `temp/amostras/` servem de base para os testes dos parsers. Elas não podem ir
  para o repositório: os testes versionados usam dados sintéticos com o mesmo formato.

## 6. Fases e acompanhamento

Estados: `pendente` → `concluída` (código pronto) → `testada` (verificação rodou) → `validada`
(dono confirmou no celular).

**Fase 0 · Esqueleto e build**
- [ ] Projeto Android + workflow do Actions que gera APK assinado e publica Release — `pendente`
- [ ] Dono instala o APK "olá mundo" e confirma que a atualização instala por cima — `pendente`

**Fase 1 · Contas e carga de dados**
- [ ] Cadastro de conta Xtream (com "colar link"), várias contas e uma ativa — `pendente`
- [ ] Carga em fluxo para o banco local (canais, filmes, séries) — `pendente`
- [ ] Carga em fluxo da programação — `pendente`

**Fase 2 · TV ao vivo**
- [ ] Tela dividida com player, lista de canais e categorias — `pendente`
- [ ] Aba Programação (Ontem/Hoje/Amanhã) e programa atual na lista — `pendente`
- [ ] Tela cheia ao deitar o celular — `pendente`

**Fase 3 · Filmes e séries**
- [ ] Listas por categoria, detalhes, temporadas e episódios — `pendente`
- [ ] Progresso e retomada — `pendente`

**Fase 4 · Início, busca e favoritos**
- [ ] Vitrine do Início — `pendente`
- [ ] Busca única com filtro — `pendente`
- [ ] Favoritos — `pendente`

**Fase 5 · Distribuição**
- [ ] Aviso de versão nova via Releases — `pendente`
- [ ] Instruções de instalação para família e amigos (README) — `pendente`
