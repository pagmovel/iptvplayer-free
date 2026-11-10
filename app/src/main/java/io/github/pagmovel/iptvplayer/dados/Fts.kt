package io.github.pagmovel.iptvplayer.dados

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Índices de busca textual (FTS4) do nome de canais, filmes e séries.
 *
 * São "external content tables" do SQLite: a tabela FTS não guarda cópia do texto, só o
 * índice, e lê o nome direto da entidade de origem (`contentEntity`) pelo rowid implícito
 * que toda tabela SQLite tem (mesmo com chave primária composta, como aqui). Por isso o
 * SQLite não atualiza esse índice sozinho quando a entidade de origem muda em massa — é
 * preciso reconstruí-lo com `INSERT INTO <tabela>(<tabela>) VALUES ('rebuild')`
 * (https://www.sqlite.org/fts3.html#_external_content_fts4_tables_). É isso que
 * `CatalogoDao` faz depois de cada inserção/remoção em massa.
 *
 * `TOKENIZER_UNICODE61` tokeniza em minúsculas e remove acentos por padrão
 * (remove_diacritics=1), por isso a busca ignora acento e maiúscula sem configuração extra.
 */
@Fts4(contentEntity = Canal::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "CanalFts")
data class CanalFts(val nome: String)

@Fts4(contentEntity = Filme::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "FilmeFts")
data class FilmeFts(val nome: String)

@Fts4(contentEntity = Serie::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "SerieFts")
data class SerieFts(val nome: String)
