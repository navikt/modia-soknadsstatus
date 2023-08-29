package no.nav.modia.soknadsstatus

import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.util.Collections
import javax.sql.DataSource

object SqlDsl {
    private fun <T> DataSource.useConnection(block: (Connection) -> T): T = connection.use(block)


    fun <T> DataSource.executeQuery(
        sql: String,
        vararg variables: Any,
        block: (resultSet: ResultSet) -> T
    ): List<T> {
        return useConnection { connection ->
            var rows = mutableListOf<T>()
            val rs = preparedStatement(connection, sql, variables).executeQuery()
            while (rs.next()) {
                rows.add(block(rs))
            }
            Collections.unmodifiableList(rows)
        }
    }

    fun DataSource.execute(sql: String, vararg variables: Any?): Boolean {
        return useConnection { connection ->
            preparedStatement(connection, sql, variables).execute()
        }
    }

    fun <T> Connection.executeQuery(
        sql: String,
        vararg variables: Any,
        block: (resultSet: ResultSet) -> T
    ): List<T> {
        var rows = mutableListOf<T>()
        val rs = preparedStatement(this, sql, variables).executeQuery()
        while (rs.next()) {
            rows.add(block(rs))
        }
        return Collections.unmodifiableList(rows)
    }

    fun Connection.execute(sql: String, vararg variables: Any?): Boolean {
        return preparedStatement(this, sql, variables).execute()
    }

    fun <T> Connection.executeWithResult(
        sql: String,
        vararg variables: Any?,
        block: (resultSet: ResultSet) -> T
    ): List<T> {
        val stmt = preparedStatement(this, sql, variables)
        stmt.execute()
        val rs = stmt.resultSet

        var rows = mutableListOf<T>()
        while (rs.next()) {
            rows.add(block(rs))
        }
        return Collections.unmodifiableList(rows)
    }

    private fun preparedStatement(
        connection: Connection,
        sql: String,
        variables: Array<out Any?>,
    ): PreparedStatement {
        val stmt = connection.prepareStatement(sql)
        variables.forEachIndexed { index, value ->
            stmt.setAny(index + 1, value)
        }
        return stmt
    }

    private fun PreparedStatement.setAny(index: Int, value: Any?) {
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
            null -> setNull(index, Types.VARCHAR)
        }
    }
}
