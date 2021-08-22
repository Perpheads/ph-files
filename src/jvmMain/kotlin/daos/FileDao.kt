package com.perpheads.files.daos

import com.perpheads.files.data.File
import com.perpheads.files.db.Tables.FILES
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.types.UInteger

class FileDao(
    private val conf: Configuration
) {
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

    fun getThumbnails(fileIds: List<Int>, userId: Int, create: DSLContext = dslContext): List<Pair<Int, ByteArray>> {
        return create.select(FILES.FILE_ID, FILES.THUMBNAIL)
            .from(FILES)
            .where(FILES.FILE_ID.`in`(fileIds))
            .and(FILES.THUMBNAIL.isNotNull)
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
            condition = condition.and(
                "MATCH(?) AGAINST(CONCAT('*', ?, '*') IN BOOLEAN MODE)",
                FILES.FILE_NAME, searchStr
            )
        }

        val query = create.select()
            .from(FILES)
            .where(condition)
            .orderBy(FILES.FILE_ID.desc())
        val fileCount = create.fetchCount(query)
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
}