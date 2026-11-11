package io.github.pagmovel.iptvplayer.conta
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.pagmovel.iptvplayer.dados.Conta
import kotlinx.coroutines.flow.Flow
/**
 * Acesso à tabela de contas Xtream cadastradas no app (ver [Conta]). `listar()` devolve um
 * [Flow] para a tela de contas se atualizar sozinha quando alguma conta é inserida,
 * atualizada ou apagada — sem precisar que quem chama recarregue manualmente.
 */
@Dao
abstract class ContaDao {
    @Insert
    abstract suspend fun inserir(conta: Conta): Long
    @Update
    abstract suspend fun atualizar(conta: Conta)
    @Query("DELETE FROM Conta WHERE id = :id")
    abstract suspend fun apagar(id: Long)
    @Query("SELECT * FROM Conta")
    abstract fun listar(): Flow<List<Conta>>
    @Query("SELECT * FROM Conta WHERE ativa = 1 LIMIT 1")
    abstract suspend fun ativa(): Conta?
    @Query("UPDATE Conta SET ultimaCargaEm = :quando WHERE id = :id")
    abstract suspend fun marcarCarga(id: Long, quando: Long)
}
