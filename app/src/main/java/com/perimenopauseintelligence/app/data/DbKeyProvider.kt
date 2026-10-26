package com.perimenopauseintelligence.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The database passphrase is random, generated on the device, and stored only after being
 * encrypted with a non-exportable Android Keystore key. It is never logged or transmitted.
 */
object DbKeyProvider {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "symptom_db_wrapping_key"
    private const val PREFS = "db_key_store"
    private const val PREF_KEY = "wrapped_passphrase"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    @Synchronized
    fun passphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(PREF_KEY, null)
        if (stored != null) return unwrap(Base64.decode(stored, Base64.NO_WRAP))
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(PREF_KEY, Base64.encodeToString(wrap(raw), Base64.NO_WRAP)).apply()
        return raw
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private fun wrap(raw: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return cipher.iv + cipher.doFinal(raw)
    }

    private fun unwrap(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, 12)
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        return cipher.doFinal(blob, 12, blob.size - 12)
    }
}
