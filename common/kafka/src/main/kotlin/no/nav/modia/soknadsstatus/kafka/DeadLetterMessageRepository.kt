package no.nav.modia.soknadsstatus.kafka

import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.sql.DataSource

class DeadLetterMessageRepository(tableName: String, private val dataSource: DataSource) {
    private val log = LoggerFactory.getLogger("${DeadLetterMessageRepository::class.java}-$tableName")
    private val tabell = Tabell(tableName)

    private fun get(key: String): Result<List<SkipTableEntry>> {
        return dataSource.executeQuery("SELECT * FROM $tabell WHERE ${tabell.key} = ?", key).map { rows ->
            rows.map {
                SkipTableEntry(
                    key = it.rs.getString(tabell.key),
                    createdAt = it.rs.getTimestamp(tabell.createdAt).toInstant(),
                    skippedAt = it.rs.getTimestamp(tabell.skippedAt)?.toInstant()
                )
            }.toList()
        }
    }

    private fun markAsSkipped(key: String): Result<Boolean> {
        return dataSource.execute("""
            UPDATE $tabell
            SET ${tabell.skippedAt} = NOW()
            WHERE ${tabell.key} = ?
        """.trimIndent(), key)
    }

    fun getAndMarkAsSkipped(key: String): List<SkipTableEntry> = get(key).fold(onSuccess = {
        if (it.isNotEmpty()) {
            markAsSkipped(key)
        }
        it
    }) {
        log.error("Failed to retrieve dead letter key from DB", it)
        listOf()
    }
}

private data class Tabell(
    val tableName: String,
    val key: String = "key",
    val createdAt: String = "created_at",
    val skippedAt: String = "skipped_at"
) {
    override fun toString(): String = tableName
}

data class SkipTableEntry(
    val key: String,
    val createdAt: Instant,
    val skippedAt: Instant? = null
)
