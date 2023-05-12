package no.nav.modia.soknadsstatus.kafka

import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import java.time.Instant
import javax.sql.DataSource

interface DeadLetterMessageSkipService {
    suspend fun shouldSkip(key: String): Boolean
}

class DeadLetterMessageSkipServiceImpl(private val repository: DeadLetterMessageRepository) : DeadLetterMessageSkipService {
    override suspend fun shouldSkip(key: String): Boolean = repository.getAndMarkAsSkipped(key).isNotEmpty()
}

class DeadLetterMessageRepository(tableName: String, private val dataSource: DataSource) {
    private val tabell = Tabell(tableName)

    private fun get(key: String): Result<List<SkipTableEntry>> {
        return dataSource.executeQuery("SELECT * from $tabell where ${tabell.key} = ?", key).map { rows ->
            rows.map {
                SkipTableEntry(
                    key = it.rs.getString(tabell.key),
                    createdAt = it.rs.getTimestamp(tabell.createdAt).toInstant(),
                    skippedAt = it.rs.getTimestamp(tabell.skippedAt).toInstant()
                )
            }.toList()
        }
    }

    private fun markAsSkipped(key: String): Result<Boolean> {
        return dataSource.execute("INSERT INTO $tabell(${tabell.skippedAt}) VALUES(NOW()) WHERE key = ?", key)
    }

    fun getAndMarkAsSkipped(key: String): List<SkipTableEntry> = get(key).fold(onSuccess = {
        markAsSkipped(key)
        it
    }) {
        listOf()
    }
}

private data class Tabell(
    val tableName: String,
    val key: String = "key",
    val createdAt: String = "createdAt",
    val skippedAt: String = "skippedAt"
) {
    override fun toString(): String = tableName
}

data class SkipTableEntry(
    val key: String,
    val createdAt: Instant,
    val skippedAt: Instant? = null
)
