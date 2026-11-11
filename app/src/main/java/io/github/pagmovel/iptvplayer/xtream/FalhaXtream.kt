package io.github.pagmovel.iptvplayer.xtream

/**
 * Falhas da comunicação com a API Xtream, separadas por causa para quem chama decidir a
 * mensagem (ex.: credenciais erradas pede novo cadastro; servidor indisponível pede "tentar
 * de novo"; ver design, "Comportamento quando o stream falha").
 */
sealed class FalhaXtream : Exception() {

    /** `auth = 0` na resposta de `player_api.php`: usuário ou senha errados. */
    class CredenciaisInvalidas : FalhaXtream()

    /** Autenticou, mas o status não é `"Active"` (ex.: `"Expired"`, `"Banned"`, `"Disabled"`). */
    data class ContaInativa(val status: String) : FalhaXtream()

    /** Erro de rede, timeout ou HTTP não 2xx — não é problema de credenciais nem de dados. */
    data class ServidorIndisponivel(val causa: Throwable?) : FalhaXtream()

    /**
     * Corpo que não é o JSON esperado (ex.: HTML de uma página de erro) ou um
     * redirecionamento para outro host — o provedor real redireciona links de lista M3U para
     * `www.google.com`, e [XtreamApi] não segue redirecionamento (`followRedirects(false)`).
     */
    data class RespostaInvalida(val detalhe: String) : FalhaXtream()
}

/**
 * Mensagem em pt-BR para mostrar ao usuário (decisão do plano, T11) — usada tanto pelo
 * [io.github.pagmovel.iptvplayer.carga.CargaWorker] quanto pela tela de contas, para a
 * tradução não ficar duplicada nos dois lugares.
 */
fun FalhaXtream.mensagemUsuario(): String = when (this) {
    is FalhaXtream.CredenciaisInvalidas -> "Usuário ou senha incorretos"
    is FalhaXtream.ContaInativa -> "Conta do provedor inativa ou vencida ($status)"
    is FalhaXtream.ServidorIndisponivel -> "Servidor não respondeu. Tente outro endereço do seu provedor."
    is FalhaXtream.RespostaInvalida -> "O servidor respondeu algo inesperado."
}
