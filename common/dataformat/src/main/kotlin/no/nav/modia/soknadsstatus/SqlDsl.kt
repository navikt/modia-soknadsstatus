package no.nav.modia.soknadsstatus

import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Time
import java.sql.Timestamp
import javax.sql.DataSource

object SqlDsl {
    private fun <T> DataSource.useConnection(block: (Connection) -> T): Result<T> = runCatching {
        connection.let(block)
    }

    fun DataSource.executeQuery(sql: String, vararg variables: Any): Result<Row> {
        return useConnection { connection ->
            Row(
                preparedStatement(connection, sql, variables).executeQuery()
            )
        }
    }

    fun DataSource.execute(sql: String, vararg variables: Any): Result<Boolean> {
        return useConnection { connection ->
            preparedStatement(connection, sql, variables).execute()
        }
    }

    private fun preparedStatement(
        connection: Connection,
        sql: String,
        variables: Array<out Any>
    ): PreparedStatement {
        val stmt = connection.prepareStatement(sql)
        variables.forEachIndexed { index, value ->
            stmt.setAny(index + 1, value)
        }
        return stmt
    }

    private fun PreparedStatement.setAny(index: Int, value: Any) {
        when (value) {
            is Boolean -> setBoolean(index, value)
            is Byte -> setByte(index, value)
            is Int -> setInt(index, value)
            is Long -> setLong(index, value)
            is Double -> setDouble(index, value)
            is Float -> setFloat(index, value)
            is ByteArray -> setBytes(index, value)
            is Date -> setDate(index, value)
            is Time -> setTime(index, value)
            is Timestamp -> setTimestamp(index, value)
            is String -> setString(index, value)
        }
    }

    class Row(val resultSet: ResultSet) : Sequence<Row> {
        override fun iterator(): Iterator<Row> {
            return object : Iterator<Row> {
                override fun hasNext() = resultSet.next()
                override fun next() = Row(resultSet)
            }
        }

        val rs = resultSet
    }
}
