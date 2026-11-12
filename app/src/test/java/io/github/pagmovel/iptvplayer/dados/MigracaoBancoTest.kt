package io.github.pagmovel.iptvplayer.dados

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Testa a `Migration(1, 2)` real (a mesma registrada em `AppIptv.kt`) contra um banco
 * recriado a partir do schema exportado em `app/schemas/.../1.json`.
 *
 * Não usa `androidx.room.testing.MigrationTestHelper`: ele lê o schema como asset Android via
 * `InstrumentationRegistry`, e neste projeto (unit test Robolectric, sem androidTest/emulador)
 * o sourceSet de teste não é mesclado nos assets que o Robolectric enxerga — achado real desta
 * tarefa (`FileNotFoundException: Cannot find the schema file in the assets folder`, mesmo com
 * `testOptions.unitTests.isIncludeAndroidResources = true` e `sourceSets.test.assets`; a
 * propriedade `android_merged_assets` gerada pelo AGP só aponta pro merge do variant de
 * produção, nunca pro sourceSet `test`). Em vez disso, o teste lê o `1.json` como resource de
 * teste comum (`sourceSets.test.resources`, ver `app/build.gradle.kts`), recria o schema v1 com
 * o SQL exato que o Room já gerou naquela versão, e abre com o `Room.databaseBuilder` real
 * (o mesmo builder de produção, com a mesma `Migration` registrada) para migrar de verdade.
 */
@RunWith(RobolectricTestRunner::class)
class MigracaoBancoTest {
    private var arquivoBanco: File? = null

    @After
    fun apagarArquivo() {
        arquivoBanco?.delete()
    }

    @Test
    fun `migracao 1 para 2 cria a tabela CanalFavorito sem apagar dados existentes`() = runTest {
        val arquivo = File.createTempFile("iptv-migracao-teste", ".db")
        arquivoBanco = arquivo
        criarBancoVersao1(arquivo)

        val bancoMigrado = Room.databaseBuilder(RuntimeEnvironment.getApplication(), BancoApp::class.java, arquivo.absolutePath)
            .addMigrations(BancoApp.MIGRACAO_1_2)
            .allowMainThreadQueries()
            .build()

        // A Conta inserida na versão 1 continua lá depois da migração (onUpgrade real, não
        // fallbackToDestructiveMigration).
        assertEquals("Conta Teste", bancoMigrado.contaDao().ativa()?.nome)

        // A tabela nova existe e aceita favoritar/consultar — não é só o nome na lista de
        // entities do @Database, o CREATE TABLE da Migration(1, 2) realmente rodou.
        val favorito = CanalFavorito(contaId = 1, nomeBase = "Globo", categoriaId = -1L, criadoEm = 1000L)
        bancoMigrado.catalogoDao().favoritar(favorito)
        assertEquals(listOf(favorito), bancoMigrado.catalogoDao().favoritos(1).first())

        bancoMigrado.close()
    }
}

/** Recria fielmente o schema v1 (`app/schemas/.../1.json`) com uma Conta de teste já inserida. */
private fun criarBancoVersao1(arquivo: File) {
    val textoSchema = Thread.currentThread().contextClassLoader!!
        .getResourceAsStream("io.github.pagmovel.iptvplayer.dados.BancoApp/1.json")!!
        .bufferedReader()
        .readText()
    val schema = JSONObject(textoSchema).getJSONObject("database")

    val db = SQLiteDatabase.openOrCreateDatabase(arquivo, null)

    val setupQueries = schema.getJSONArray("setupQueries")
    for (i in 0 until setupQueries.length()) {
        db.execSQL(setupQueries.getString(i))
    }

    val entidades = schema.getJSONArray("entities")
    for (i in 0 until entidades.length()) {
        val entidade = entidades.getJSONObject(i)
        val tabela = entidade.getString("tableName")
        db.execSQL(entidade.getString("createSql").replace("\${TABLE_NAME}", tabela))
        if (entidade.has("contentSyncTriggers")) {
            val gatilhos = entidade.getJSONArray("contentSyncTriggers")
            for (j in 0 until gatilhos.length()) {
                db.execSQL(gatilhos.getString(j))
            }
        }
    }

    db.execSQL(
        "INSERT INTO Conta (id, nome, servidor, usuario, senhaCifrada, ativa, ultimaCargaEm) " +
            "VALUES (1, 'Conta Teste', 'http://servidor', 'usuario', X'00', 1, NULL)",
    )
    db.version = 1
    db.close()
}
