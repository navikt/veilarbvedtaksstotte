package no.nav.veilarbvedtaksstotte.utils

import no.nav.common.rest.client.RestUtils
import okhttp3.Response
import tools.jackson.databind.ObjectMapper

object JsonUtils {

    @JvmStatic
    val objectMapper: ObjectMapper = no.nav.common.json.JsonUtils.getMapper()

    @JvmStatic
    fun init() {
        // noop, trigger evaluering av objectMapper og registrering av moduler
    }

    @JvmStatic
    fun <T> fromJson(json: String, valueClass: Class<T>): T {
        return objectMapper.readValue(json, valueClass)
    }

    @JvmStatic
    fun <T> fromJsonArray(json: String, valueClass: Class<T>): List<T> {
        val listType = objectMapper.typeFactory.constructCollectionType(List::class.java, valueClass)
        return objectMapper.readValue(json, listType)
    }
}

inline fun <reified T> Response.deserializeJson(): T? {
    return RestUtils.getBodyStr(this)
        .map { JsonUtils.objectMapper.readValue(it, T::class.java) }
        .orElse(null)
}

inline fun <reified T> Response.deserializeJsonAndThrowOnNull(): T {
    return this.deserializeJson() ?: throw IllegalStateException("Unable to parse JSON object from response body")
}

fun <T> T.toJson(): String {
    return JsonUtils.objectMapper.writeValueAsString(this)
}
