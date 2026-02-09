package com.pecadoartesano.core.security

import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordService {
    private val bcrypt = BCrypt.withDefaults()
    private val bcryptVerifier = BCrypt.verifyer()

    fun hash(password: String): String {
        return bcrypt.hashToString(12, password.toCharArray())
    }
    fun verify(password: String, passwodHash: String): Boolean {
        return bcryptVerifier.verify(password.toCharArray(), passwodHash).verified
    }
}