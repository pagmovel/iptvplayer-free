package io.github.pagmovel.iptvplayer.aovivo

import io.github.pagmovel.iptvplayer.dados.Canal

enum class Qualidade(val rotulo: String) {
    SD("SD"),
    HD("HD"),
    FHD("FHD"),
    QUATRO_K("4K"),
}

data class VarianteQualidade(val qualidade: Qualidade, val streamId: Long)

data class CanalAgrupado(
    val nomeBase: String,
    val categoriaId: Long,
    val icone: String?,
    val epgId: String?,
    val numero: Int?,
    val variantes: List<VarianteQualidade>,
)

fun chaveCategoria(categoriaId: Long?): Long {
    return categoriaId ?: -1L
}

fun qualidadeDoNome(nome: String): Qualidade? {
    val regex = Regex("""(?i)\s+(4K|FHD|HD|SD)$""")
    val match = regex.find(nome)
    return match?.let {
        when (it.groupValues[1].uppercase()) {
            "SD" -> Qualidade.SD
            "HD" -> Qualidade.HD
            "FHD" -> Qualidade.FHD
            "4K" -> Qualidade.QUATRO_K
            else -> null
        }
    }
}

fun nomeBase(nome: String): String {
    val regex = Regex("""(?i)\s+(4K|FHD|HD|SD)$""")
    return nome.replace(regex, "")
}

fun agruparPorQualidade(canais: List<Canal>): List<CanalAgrupado> {
    // Agrupar por (nomeBase, categoriaId normalizada)
    val agrupados = canais.groupBy { canal ->
        Pair(nomeBase(canal.nome), chaveCategoria(canal.categoriaId))
    }

    return agrupados.map { (chave, grupoCanais) ->
        val (nome, categoriaId) = chave

        // Usar os valores do primeiro canal do grupo (são do mesmo canal lógico)
        val primeiro = grupoCanais.first()

        // Criar variantes ordenadas da maior pra menor qualidade
        val variantes = grupoCanais
            .map { canal ->
                val qualidade = qualidadeDoNome(canal.nome) ?: Qualidade.SD
                VarianteQualidade(qualidade, canal.streamId)
            }
            .sortedByDescending { it.qualidade.ordinal }

        CanalAgrupado(
            nomeBase = nome,
            categoriaId = categoriaId,
            icone = primeiro.icone,
            epgId = primeiro.epgId,
            numero = primeiro.numero,
            variantes = variantes
        )
    }
}
