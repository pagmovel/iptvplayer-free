package io.github.pagmovel.iptvplayer.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Contagem de itens do catálogo de uma conta, por tipo. */
data class Contagem(val canais: Int, val filmes: Int, val series: Int, val programas: Int)

/**
 * Acesso ao catálogo (categorias, canais, filmes, séries e programação) e à busca textual
 * por nome. Os índices FTS (ver `Fts.kt`) se mantêm sincronizados sozinhos: o Room gera
 * triggers de sincronia (`room_fts_content_sync_...`, visíveis em
 * `app/schemas/.../1.json`) que disparam a cada INSERT/UPDATE/DELETE nas tabelas de
 * origem — este DAO não reconstrói índice nenhum manualmente.
 */
@Dao
abstract class CatalogoDao {

    // REPLACE: o provedor real pode mandar o mesmo item duas vezes na mesma lista (achado
    // real, T-pós-lançamento: stream_id duplicado em get_vod_streams quebrava a carga com
    // SQLiteConstraintException, sem virar FalhaXtream — ver skill iptv-xtream-carga).

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserirCategorias(categorias: List<Categoria>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserirCanais(canais: List<Canal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserirFilmes(filmes: List<Filme>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserirSeries(series: List<Serie>)

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

    @Transaction
    open suspend fun apagarCatalogo(contaId: Long) {
        apagarCategorias(contaId)
        apagarCanais(contaId)
        apagarFilmes(contaId)
        apagarSeries(contaId)
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

    // REPLACE: favoritar de novo o mesmo canal (mesma chave) só atualiza criadoEm, em vez de
    // lançar SQLiteConstraintException.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun favoritar(favorito: CanalFavorito)
    @Query("DELETE FROM CanalFavorito WHERE contaId = :contaId AND nomeBase = :nomeBase AND categoriaId = :categoriaId")
    abstract suspend fun desfavoritar(contaId: Long, nomeBase: String, categoriaId: Long)
    @Query("SELECT * FROM CanalFavorito WHERE contaId = :contaId")
    abstract fun favoritos(contaId: Long): Flow<List<CanalFavorito>>

    @Query("SELECT * FROM Categoria WHERE contaId = :contaId AND tipo = :tipo")
    abstract fun categorias(contaId: Long, tipo: TipoConteudo): Flow<List<Categoria>>

    @Query("SELECT * FROM Canal WHERE contaId = :contaId")
    abstract fun canais(contaId: Long): Flow<List<Canal>>

    @Query("SELECT * FROM Programa WHERE contaId = :contaId AND epgId = :epgId AND inicio >= :inicioMs AND inicio < :fimMs ORDER BY inicio ASC")
    abstract suspend fun programacao(contaId: Long, epgId: String, inicioMs: Long, fimMs: Long): List<Programa>
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
