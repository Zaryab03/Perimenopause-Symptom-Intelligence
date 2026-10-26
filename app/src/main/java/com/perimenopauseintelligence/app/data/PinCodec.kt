package com.perimenopauseintelligence.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN hashing with a per-device random salt. The PIN itself is never stored. */
object PinCodec {
    private const val ITERATIONS = 120_000
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)).encoded

    fun matches(pin: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expected)

    fun isValid(pin: String): Boolean = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it.isDigit() }
}
