package io.github.pagmovel.iptvplayer.dados

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.pagmovel.iptvplayer.conta.ContaDao

/**
 * Banco local do app: catálogo (categorias, canais, filmes, séries, programação) e contas
 * Xtream. Versão 1, com esquema exportado para `app/schemas/` (ver `room.schemaLocation`
 * em `app/build.gradle.kts`) para dar suporte a migrações nas próximas versões.
 *
 * Toda troca de catálogo (apagar + reinserir) acontece dentro de `withTransaction { }`,
 * chamada feita pela T9 — este banco só expõe os DAOs.
 */
@Database(
    entities = [
        Categoria::class,
        Canal::class,
        Filme::class,
        Serie::class,
        Programa::class,
        Conta::class,
        CanalFts::class,
        FilmeFts::class,
        SerieFts::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BancoApp : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
    abstract fun contaDao(): ContaDao
}
