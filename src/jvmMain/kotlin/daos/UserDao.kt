package com.perpheads.files.daos

import com.perpheads.files.data.User
import com.perpheads.files.db.Tables.USERS
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.mindrot.jbcrypt.BCrypt


class UserDao(
    private val conf: Configuration
) {
    private val dslContext = DSL.using(conf)

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

    fun updateApiKey(userId: Int, apiKey: String, create: DSLContext = dslContext) {
        create.update(USERS)
            .set(USERS.API_KEY, apiKey)
            .where(USERS.USER_ID.eq(userId))
            .execute()
    }

    fun changePassword(userId: Int, newPassword: String, create: DSLContext = dslContext) {
        create.update(USERS)
            .set(USERS.PASSWORD, BCrypt.hashpw(newPassword, BCrypt.gensalt()))
            .where(USERS.USER_ID.eq(userId))
            .execute()
    }
}