package com.perimenopauseintelligence.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCodecTest {
    @Test
    fun correctPinMatchesAndWrongPinDoesNot() {
        val salt = PinCodec.newSalt()
        val hash = PinCodec.hash("1234", salt)
        assertTrue(PinCodec.matches("1234", salt, hash))
        assertFalse(PinCodec.matches("1235", salt, hash))
    }

    @Test
    fun saltChangesTheHash() {
        val a = PinCodec.hash("1234", PinCodec.newSalt())
        val b = PinCodec.hash("1234", PinCodec.newSalt())
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun hashIsDeterministicForSameSalt() {
        val salt = PinCodec.newSalt()
        assertArrayEquals(PinCodec.hash("987654", salt), PinCodec.hash("987654", salt))
    }

    @Test
    fun validatesLengthAndDigits() {
        assertTrue(PinCodec.isValid("1234"))
        assertTrue(PinCodec.isValid("12345678"))
        assertFalse(PinCodec.isValid("123"))
        assertFalse(PinCodec.isValid("123456789"))
        assertFalse(PinCodec.isValid("12ab"))
        assertFalse(PinCodec.isValid(""))
    }
}
