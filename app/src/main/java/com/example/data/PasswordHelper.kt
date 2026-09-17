package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHelper {
    private const val SALT_LENGTH = 16
    private const val HASH_ALGORITHM = "SHA-256"

    fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        digest.update(salt.toByteArray())
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        val computedHash = hashPassword(password, salt)
        return computedHash == storedHash
    }

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return buildString(8) {
            repeat(8) { append(chars[SecureRandom().nextInt(chars.length)]) }
        }
    }
}
