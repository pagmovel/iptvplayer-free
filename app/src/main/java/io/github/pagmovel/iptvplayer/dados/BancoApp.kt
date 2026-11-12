package io.github.pagmovel.iptvplayer.dados

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.pagmovel.iptvplayer.conta.ContaDao

/**
 * Banco local do app: catálogo (categorias, canais, filmes, séries, programação), contas
 * Xtream e favoritos. Versão 2, com esquema exportado para `app/schemas/` (ver
 * `room.schemaLocation` em `app/build.gradle.kts`) para dar suporte a migrações nas próximas
 * versões.
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
        CanalFavorito::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class BancoApp : RoomDatabase() {
    abstract fun catalogoDao(): CatalogoDao
    abstract fun contaDao(): ContaDao

    companion object {
        /**
         * 1 -> 2 (T2): só cria a tabela de favoritos; não mexe nas tabelas existentes. Real
         * (SQL explícito), nunca `fallbackToDestructiveMigration` — rodar o app uma vez não
         * apaga o banco do dono. Registrada em `Room.databaseBuilder` (ver `AppIptv.kt`).
         */
        val MIGRACAO_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `CanalFavorito` (`contaId` INTEGER NOT NULL, " +
                        "`nomeBase` TEXT NOT NULL, `categoriaId` INTEGER NOT NULL, " +
                        "`criadoEm` INTEGER NOT NULL, PRIMARY KEY(`contaId`, `nomeBase`, `categoriaId`))",
                )
            }
        }
    }
}
