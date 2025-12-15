package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @param type
 * @param periodeId UUID for perioden som meldingen gjelder, hentes fra Periode topic eller oppslags api
 * @param bekreftelsesloesning
 * @param intervalMS Interval for bekreftelse i millisekunder. Denne gir registeret et hint om hvor ofte løsningen vil sende meldinger, registeret vil ikke gjøre noe når fristen utløper.
 * @param graceMS Grace periode i millisekunder. Hvor lenge den som sender bekreftelse på vegne av arbeidssøker venter etter at intervallet er utløpt før den terminerer sine tjenester og stopper bekreftelse på vegne av arbeidssøker. Feks før dagpengene stoppes og og dagpengeløsningen stopper bekreftelse på vegne av arbeidssøker. Dette brukes av registeret for å å kunne oppdage 'døde' klienter, men har ingen funksjonell betydning.
 */
data class PaaVegneAvStart(

    @get:JsonProperty("type", required = true) val type: Type,

    @get:JsonProperty("periodeId", required = true) val periodeId: java.util.UUID,

    @get:JsonProperty(
        "bekreftelsesloesning",
        required = true
    ) val bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.UKJENT_VERDI,

    @get:JsonProperty("intervalMS", required = true) val intervalMS: Long,

    @get:JsonProperty("graceMS", required = true) val graceMS: Long
) {

    /**
     *
     * Values: PAA_VEGNE_AV_START_V1
     */
    enum class Type(@get:JsonValue val value: String) {

        PAA_VEGNE_AV_START_V1("PAA_VEGNE_AV_START_V1");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: String): Type {
                return entries.firstOrNull { it.value == value }
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'PaaVegneAvStart'")
            }
        }
    }

}

