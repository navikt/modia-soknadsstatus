package no.nav.modia.soknadsstatus

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object Encoding {
    fun <T> encode(
        serializer: KSerializer<T>,
        value: T,
    ): String {
        if (serializer == String.serializer()) return value as String

        return Json.encodeToString(serializer, value)
    }

    fun <T> decode(
        serializer: KSerializer<T>,
        value: String,
    ): T {
        if (serializer == String.serializer()) return value as T

        return Json.decodeFromString(serializer, value)
    }
}
