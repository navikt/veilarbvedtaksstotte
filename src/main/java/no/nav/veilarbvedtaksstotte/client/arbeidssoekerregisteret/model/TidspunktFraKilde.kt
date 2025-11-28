package no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 *
 * @param tidspunkt Betydningen av tidspunktet er avhengig av avviksType. FORSINKELSE: Tidspunktet er tidspunktet da endringen skulle vært utført.  SLETTET: Tidspunktet er tidspunktet da endringen ble utført(samme som selve recorden).  TIDSPUNKT_KORRIGERT: Tidspunktet som egentlig er korrekt, feks tidspunktet da en periode skulle vært stoppet                    eller startet
 * @param avviksType
 */
data class TidspunktFraKilde(

    @get:JsonProperty("tidspunkt", required = true) val tidspunkt: LocalDateTime,

    @get:JsonProperty("avviksType", required = true) val avviksType: AvviksType = AvviksType.UKJENT_VERDI
)

