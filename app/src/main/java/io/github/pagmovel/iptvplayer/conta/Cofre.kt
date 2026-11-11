package io.github.pagmovel.iptvplayer.conta
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
/**
 * Cifra/decifra a senha da conta Xtream antes de gravar no banco — a senha nunca vai em
 * texto puro para o `Conta.senhaCifrada` (ver T10). Interface só por causa do teste: o
 * Robolectric não emula o `AndroidKeyStore`, então [ContasRepositorioTest] usa um Cofre
 * falso em memória; [CofreKeystore] é a segunda implementação, a real.
 */
interface Cofre {
    fun cifrar(texto: String): ByteArray
    fun decifrar(dados: ByteArray): String
}
/**
 * Implementação real: AES/GCM/NoPadding com chave não exportável no `AndroidKeyStore`. O IV
 * (12 bytes, tamanho padrão do GCM) vai prefixado ao resultado porque tem que ser único a
 * cada cifragem e `decifrar` precisa dele de volta — guardar numa coluna separada seria mais
 * uma coisa para sincronizar com o mesmo propósito.
 */
class CofreKeystore : Cofre {
    override fun cifrar(texto: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMACAO)
        cipher.init(Cipher.ENCRYPT_MODE, chave())
        return cipher.iv + cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
    }
    override fun decifrar(dados: ByteArray): String {
        val iv = dados.copyOfRange(0, TAMANHO_IV)
        val cifrado = dados.copyOfRange(TAMANHO_IV, dados.size)
        val cipher = Cipher.getInstance(TRANSFORMACAO)
        cipher.init(Cipher.DECRYPT_MODE, chave(), GCMParameterSpec(TAMANHO_TAG_BITS, iv))
        return cipher.doFinal(cifrado).toString(Charsets.UTF_8)
    }
    private fun chave(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gerador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gerador.generateKey()
    }
    private companion object {
        const val ALIAS = "iptv-player-free-senhas"
        const val TRANSFORMACAO = "AES/GCM/NoPadding"
        const val TAMANHO_IV = 12
        const val TAMANHO_TAG_BITS = 128
    }
}
