package com.perpheads.files.daos

import com.perpheads.files.alphaNumeric
import com.perpheads.files.data.User
import com.perpheads.files.db.Tables.USERS
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom


class UserDao(
    conf: Configuration
) {
    private val dslContext = DSL.using(conf)
    private val secureRandom = SecureRandom()

    fun getByUsername(username: String, create: DSLContext = dslContext): User? {
        return create.select()
            .from(USERS)
            .where(USERS.NAME.eq(username))
            .fetchOneInto(User::class.java)
    }

    fun getByApiKey(apiKey: String, create: DSLContext = dslContext): User? {
        return create.select()
            .from(USERS)
            .where(USERS.API_KEY.eq(apiKey))
            .fetchOneInto(User::class.java)
    }

    private fun generateApiKeyStr(): String {
        return secureRandom.alphaNumeric(32)
    }

    fun generateApiKey(userId: Int, create: DSLContext = dslContext): String {
        val newApiKey = generateApiKeyStr()
        create.update(USERS)
            .set(USERS.API_KEY, newApiKey)
            .where(USERS.USER_ID.eq(userId))
            .execute()
        return newApiKey
    }

    fun changePassword(userId: Int, newPassword: String, create: DSLContext = dslContext) {
        create.update(USERS)
            .set(USERS.PASSWORD, BCrypt.hashpw(newPassword, BCrypt.gensalt()))
            .where(USERS.USER_ID.eq(userId))
            .execute()
    }

    fun createUser(username: String, email: String, password: String, create: DSLContext = dslContext) {
        create.insertInto(USERS)
            .columns(USERS.NAME, USERS.EMAIL, USERS.PASSWORD, USERS.API_KEY, USERS.ADMIN)
            .values(username, email, BCrypt.hashpw(password, BCrypt.gensalt()), generateApiKeyStr(), 0)
            .execute()
    }
}