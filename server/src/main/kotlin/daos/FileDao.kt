package com.perpheads.files.daos

import com.perpheads.files.data.File
import com.perpheads.files.data.FileTotalStatistics
import com.perpheads.files.data.FileUserStatistics
import com.perpheads.files.db.Tables.FILES
import com.perpheads.files.db.Tables.USERS
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.types.UInteger

class FileDao(conf: Configuration) {
    private val dslContext = DSL.using(conf)

    fun updateThumbnail(fileId: Int, thumbnail: ByteArray, create: DSLContext = dslContext) {
        create.update(FILES)
            .set(FILES.THUMBNAIL, thumbnail)
            .where(FILES.FILE_ID.eq(fileId))
            .execute()
    }

    fun updateMD5(
        fileId: Int,
        md5: String,
        fileSize: Int,
        create: DSLContext = dslContext
    ) {
        create.update(FILES)
            .set(FILES.MD5, md5)
            .set(FILES.SIZE, UInteger.valueOf(fileSize))
            .where(FILES.FILE_ID.eq(fileId))
            .execute()
    }

    fun rename(
        fileId: Int,
        name: String,
        create: DSLContext = dslContext
    ) {
        create.update(FILES)
            .set(FILES.FILE_NAME, name)
            .where(FILES.FILE_ID.eq(fileId))
            .execute()
    }

    fun getThumbnails(fileIds: List<Int>, userId: Int, create: DSLContext = dslContext): List<Pair<Int, ByteArray>> {
        return create.select(FILES.FILE_ID, FILES.THUMBNAIL)
            .from(FILES)
            .where(FILES.FILE_ID.`in`(fileIds))
            .and(FILES.THUMBNAIL.isNotNull)
            .and(FILES.USER_ID.eq(userId))
            .orderBy(FILES.FILE_ID.desc())
            .fetch {
                it[FILES.FILE_ID] to it[FILES.THUMBNAIL]
            }
    }

    fun findFileIds(create: DSLContext = dslContext): List<Int> {
        return create.select(FILES.FILE_ID)
            .from(FILES)
            .orderBy(FILES.FILE_ID.desc())
            .fetch(FILES.FILE_ID)
    }

    fun findFiles(
        userId: Int,
        beforeId: Int?,
        offset: Int,
        limit: Int,
        searchStr: String?,
        create: DSLContext = dslContext
    ): Pair<Int, List<File>> {
        var condition = FILES.USER_ID.eq(userId)

        if (beforeId != null) {
            condition = condition.and(FILES.FILE_ID.lt(beforeId))
        }
        if (searchStr != null) {
            condition = condition.and(FILES.FILE_NAME.likeIgnoreCase("%" + searchStr.replace("%", "!%") + "%", '!'))
        }

        val query = create.select()
            .from(FILES)
            .where(condition)
            .orderBy(FILES.FILE_ID.desc())
        val fileCount = create.selectCount().from(FILES).where(condition).fetchSingle().value1()
        val files = query.offset(offset).limit(limit).fetchInto(File::class.java)
        return fileCount to files
    }

    fun create(file: File, create: DSLContext = dslContext): Int {
        val record = create.newRecord(FILES)
        record.from(file)
        record.changed(FILES.FILE_ID, false)
        record.insert()
        return record.fileId
    }

    fun findByLink(link: String, create: DSLContext = dslContext): File? {
        return create.select()
            .from(FILES)
            .where(FILES.LINK.eq(link))
            .fetchOneInto(File::class.java)
    }

    fun findById(fileId: Int, create: DSLContext = dslContext): File? {
        return create.select()
            .from(FILES)
            .where(FILES.FILE_ID.eq(fileId))
            .fetchOneInto(File::class.java)
    }

    fun delete(fileId: Int, create: DSLContext = dslContext) {
        create.deleteFrom(FILES)
            .where(FILES.FILE_ID.eq(fileId))
            .execute()
    }

    fun getTotalStatistics(create: DSLContext = dslContext): FileTotalStatistics {
        return create.select(DSL.count(FILES.FILE_ID), DSL.sum(FILES.SIZE))
            .from(FILES)
            .fetchSingle {
                FileTotalStatistics(
                    fileCount = it.value1(),
                    storageUsed = it.value2().toLong()
                )
            }
    }

    fun getUserStatistics(create: DSLContext = dslContext): List<FileUserStatistics> {
        val sumField = DSL.sum(FILES.SIZE).`as`("sum")
        return create.select(USERS.NAME, DSL.count(FILES.FILE_ID), sumField)
            .from(USERS)
            .join(FILES).on(FILES.USER_ID.eq(USERS.USER_ID))
            .groupBy(USERS.NAME)
            .orderBy(sumField.desc())
            .limit(100)
            .fetch {
                FileUserStatistics(
                    name = it.value1(),
                    fileCount = it.value2(),
                    storageUsed = it.value3().toLong()
                )
            }
    }
}