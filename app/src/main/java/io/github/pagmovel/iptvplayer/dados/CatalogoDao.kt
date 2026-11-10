package io.github.pagmovel.iptvplayer.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Contagem de itens do catálogo de uma conta, por tipo. */
data class Contagem(val canais: Int, val filmes: Int, val series: Int, val programas: Int)

/**
 * Acesso ao catálogo (categorias, canais, filmes, séries e programação) e à busca textual
 * por nome. Os métodos públicos de inserção em massa já deixam o índice FTS (ver `Fts.kt`)
 * reconstruído ao final — quem chama não precisa saber que ele existe.
 */
@Dao
abstract class CatalogoDao {

    @Insert
    abstract suspend fun inserirCategorias(categorias: List<Categoria>)

    @Insert
    protected abstract suspend fun inserirCanaisBruto(canais: List<Canal>)

    @Query("INSERT INTO CanalFts(CanalFts) VALUES ('rebuild')")
    protected abstract suspend fun reconstruirIndiceCanais()

    @Transaction
    open suspend fun inserirCanais(canais: List<Canal>) {
        inserirCanaisBruto(canais)
        reconstruirIndiceCanais()
    }

    @Insert
    protected abstract suspend fun inserirFilmesBruto(filmes: List<Filme>)

    @Query("INSERT INTO FilmeFts(FilmeFts) VALUES ('rebuild')")
    protected abstract suspend fun reconstruirIndiceFilmes()

    @Transaction
    open suspend fun inserirFilmes(filmes: List<Filme>) {
        inserirFilmesBruto(filmes)
        reconstruirIndiceFilmes()
    }

    @Insert
    protected abstract suspend fun inserirSeriesBruto(series: List<Serie>)

    @Query("INSERT INTO SerieFts(SerieFts) VALUES ('rebuild')")
    protected abstract suspend fun reconstruirIndiceSeries()

    @Transaction
    open suspend fun inserirSeries(series: List<Serie>) {
        inserirSeriesBruto(series)
        reconstruirIndiceSeries()
    }

    @Insert
    abstract suspend fun inserirProgramas(programas: List<Programa>)

    @Query("DELETE FROM Categoria WHERE contaId = :contaId")
    protected abstract suspend fun apagarCategorias(contaId: Long)

    @Query("DELETE FROM Canal WHERE contaId = :contaId")
    protected abstract suspend fun apagarCanais(contaId: Long)

    @Query("DELETE FROM Filme WHERE contaId = :contaId")
    protected abstract suspend fun apagarFilmes(contaId: Long)

    @Query("DELETE FROM Serie WHERE contaId = :contaId")
    protected abstract suspend fun apagarSeries(contaId: Long)

    // ponytail: reconstrói o índice FTS inteiro (todas as contas) a cada chamada, porque a
    // troca de catálogo já é um recarregamento completo e pouco frequente; trocar por um
    // rebuild incremental só se isso aparecer como lento num catálogo real.
    @Transaction
    open suspend fun apagarCatalogo(contaId: Long) {
        apagarCategorias(contaId)
        apagarCanais(contaId)
        apagarFilmes(contaId)
        apagarSeries(contaId)
        reconstruirIndiceCanais()
        reconstruirIndiceFilmes()
        reconstruirIndiceSeries()
    }

    @Query("DELETE FROM Programa WHERE contaId = :contaId")
    abstract suspend fun apagarProgramacao(contaId: Long)

    @Query(
        "SELECT " +
            "(SELECT COUNT(*) FROM Canal WHERE contaId = :contaId) AS canais, " +
            "(SELECT COUNT(*) FROM Filme WHERE contaId = :contaId) AS filmes, " +
            "(SELECT COUNT(*) FROM Serie WHERE contaId = :contaId) AS series, " +
            "(SELECT COUNT(*) FROM Programa WHERE contaId = :contaId) AS programas",
    )
    abstract suspend fun contagem(contaId: Long): Contagem

    // Join por rowid: CanalFts só guarda o nome, então para filtrar por conta (e devolver o
    // Canal inteiro) é preciso voltar à tabela de origem pelo rowid que as duas compartilham.
    @Query(
        "SELECT Canal.* FROM Canal " +
            "JOIN CanalFts ON CanalFts.rowid = Canal.rowid " +
            "WHERE Canal.contaId = :contaId AND CanalFts.CanalFts MATCH :termoFts",
    )
    protected abstract suspend fun buscarCanaisPorTermoFts(contaId: Long, termoFts: String): List<Canal>

    /** Busca por nome, ignorando acento/maiúscula e aceitando prefixo (útil ao digitar). */
    suspend fun buscarNomesCanais(contaId: Long, termo: String): List<Canal> {
        val termoFts = termoParaFts(termo)
        if (termoFts.isEmpty()) return emptyList()
        return buscarCanaisPorTermoFts(contaId, termoFts)
    }
}

/**
 * Prepara o termo digitado para o MATCH do FTS4: separa por espaço, escapa aspas e junta
 * cada palavra como `"palavra*"` — o `*` precisa ficar DENTRO das aspas para valer como
 * prefixo (https://www.sqlite.org/fts3.html#fts4query, exemplo `"lin* app*"`; `"palavra"*`
 * com o `*` fora não aplica prefixo — é um erro fácil de cometer e que este comentário
 * existe para não repetir). O espaço entre os termos é AND implícito no FTS4.
 */
private fun termoParaFts(termo: String): String =
    termo.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ") { "\"${it.replace("\"", "\"\"")}*\"" }
