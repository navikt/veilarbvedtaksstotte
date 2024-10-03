package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.metrics.Event
import no.nav.common.metrics.MetricsClient
import no.nav.common.utils.EnvironmentUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/logger")
class Frontendlogger(val metricsClient: MetricsClient) {
    val log: Logger = LoggerFactory.getLogger(Frontendlogger::class.java)

    @PostMapping("/event")
    fun skrivEventTilInflux(@RequestBody event: FrontendEvent) {
        /* val toInflux = Event(event.name + ".event")
        event.tags?.forEach(toInflux::addTagToReport)
        event.fields?.forEach(toInflux::addFieldToReport)
        toInflux.tags["environment"] = if (EnvironmentUtils.isProduction().orElse(false)) "p" else "q1"

        if (!EnvironmentUtils.isProduction().orElse(false)) {
            log.info("Skriver event til influx: " + eventToString(event.name, toInflux))
        }
        metricsClient.report(toInflux) */
    }

    class FrontendEvent {
        var name: String? = null
        var fields: Map<String, Any>? = null
        var tags: Map<String, String>? = null
    }

    fun eventToString(name: String?, event: Event): String {
        return ("name: " + name + ".event, fields: " + (event.fields?.entries ?: "[]")
                + ", tags: " + (event.tags?.entries ?: "[]"))
    }
}