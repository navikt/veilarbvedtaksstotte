package no.nav.veilarbvedtaksstotte.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.common.json.JsonMapper
import no.nav.common.rest.client.RestUtils
import okhttp3.Response

object JsonUtils {

    val objectMapper: ObjectMapper = JsonMapper.defaultObjectMapper().registerModule(KotlinModule())

    @JvmStatic
    fun createNoDataStr(noDataMsg: String?): String {
        return createJsonStr("ingenData", noDataMsg)
    }

    fun createJsonStr(fieldName: String?, value: String?): String {
        val error = JsonNodeFactory.instance.objectNode()
        error.put(fieldName, value)
        return error.toString()
    }

    @JvmStatic
    fun <T> fromJson(json: String, valueClass: Class<T>): T {
        return objectMapper.readValue(json, valueClass)
    }
}

inline fun <reified T> Response.deserializeJson(): T? {
    return RestUtils.getBodyStr(this)
            .map { JsonUtils.objectMapper.readValue(it, T::class.java) }
            .orElse(null)
}

inline fun <reified T> Response.deserializeJsonOrThrow(): T {
    return this.deserializeJson() ?: throw IllegalStateException("Unable to parse JSON object from response body")
}

fun <T> T.toJson(): String {
    return JsonUtils.objectMapper.writeValueAsString(this)
}
