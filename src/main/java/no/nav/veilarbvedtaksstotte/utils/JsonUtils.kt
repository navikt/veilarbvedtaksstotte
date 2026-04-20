package no.nav.veilarbvedtaksstotte.utils

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import no.nav.common.rest.client.RestUtils
import okhttp3.Response

object JsonUtils {

    @JvmStatic
    val objectMapper: JsonMapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addModule(KotlinModule.Builder().build())
        .build()

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
