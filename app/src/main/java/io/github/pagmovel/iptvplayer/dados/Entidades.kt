package io.github.pagmovel.iptvplayer.dados

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tipo de conteúdo de uma categoria/canal Xtream. */
enum class TipoConteudo { AO_VIVO, FILME, SERIE }

/** Categoria do catálogo (ao vivo, filme ou série) de uma conta. */
@Entity(primaryKeys = ["contaId", "tipo", "categoriaId"])
data class Categoria(
    val contaId: Long,
    val tipo: TipoConteudo,
    val categoriaId: Long,
    val nome: String,
)

/** Canal de TV ao vivo de uma conta. */
@Entity(primaryKeys = ["contaId", "streamId"])
data class Canal(
    val contaId: Long,
    val streamId: Long,
    val nome: String,
    val categoriaId: Long?,
    val icone: String?,
    val epgId: String?,
    val numero: Int?,
)

/** Filme (VOD) de uma conta. */
@Entity(primaryKeys = ["contaId", "streamId"])
data class Filme(
    val contaId: Long,
    val streamId: Long,
    val nome: String,
    val categoriaId: Long?,
    val capa: String?,
    val extensao: String?,
    val ano: Int?,
    val nota: Double?,
    val adicionadoEm: Long?,
)

/** Série (VOD) de uma conta. */
@Entity(primaryKeys = ["contaId", "seriesId"])
data class Serie(
    val contaId: Long,
    val seriesId: Long,
    val nome: String,
    val categoriaId: Long?,
    val capa: String?,
    val sinopse: String?,
    val ano: Int?,
    val nota: Double?,
    val modificadaEm: Long?,
)

/**
 * Entrada da programação (EPG) de um canal. A chave composta (contaId, epgId, inicio) já
 * serve de índice de busca por canal/horário — não precisa de índice extra.
 */
@Entity(primaryKeys = ["contaId", "epgId", "inicio"])
data class Programa(
    val contaId: Long,
    val epgId: String,
    val inicio: Long,
    val fim: Long,
    val titulo: String,
    val descricao: String?,
)

/** Conta Xtream cadastrada no app. Preenchida pela T10 (ContaDao). */
@Entity
data class Conta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val servidor: String,
    val usuario: String,
    val senhaCifrada: ByteArray,
    val ativa: Boolean,
    val ultimaCargaEm: Long?,
)
