package com.perpheads.files.daos

import com.perpheads.files.data.Cookie
import com.perpheads.files.data.User
import com.perpheads.files.db.Tables.COOKIES
import com.perpheads.files.db.Tables.USERS
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.LocalDateTime

class CookieDao(
    private val conf: Configuration
) {
    private val dslContext = DSL.using(conf)

    private val cookieValidCondition = COOKIES.EXPIRY.gt(LocalDateTime.now())

    fun create(cookie: Cookie, create: DSLContext = dslContext) {
        val record = create.newRecord(COOKIES)
        record.from(cookie)
        record.insert()
    }

    fun getUserByCookieId(id: String, create: DSLContext = dslContext): User? {
        return create.select()
            .from(COOKIES)
            .join(USERS).on(USERS.USER_ID.eq(COOKIES.USER_ID))
            .where(COOKIES.COOKIE_ID.eq(id))
            .and(cookieValidCondition)
            .fetchOneInto(User::class.java)
    }

    fun getById(id: String, create: DSLContext = dslContext): Cookie? {
        return create.select()
            .from(COOKIES)
            .where(COOKIES.COOKIE_ID.eq(id))
            .and(cookieValidCondition)
            .fetchOneInto(Cookie::class.java)
    }

    fun delete(id: String, create: DSLContext = dslContext): Boolean {
        return create.deleteFrom(COOKIES)
            .where(COOKIES.COOKIE_ID.eq(id))
            .execute() == 1
    }

    fun deleteAllByUser(userId: Int, create: DSLContext = dslContext) {
        create.deleteFrom(COOKIES)
            .where(COOKIES.USER_ID.eq(userId))
            .execute()
    }


}
