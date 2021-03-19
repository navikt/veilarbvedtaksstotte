package no.nav.veilarbvedtaksstotte.domain.vedtak

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.kafka.dto.ArenaVedtakRecord
import no.nav.veilarbvedtaksstotte.utils.DateFormatters.ISO_LOCAL_DATE_MIDNIGHT
import no.nav.veilarbvedtaksstotte.utils.DateFormatters.ISO_LOCAL_DATE_TIME_WITHOUT_T
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ArenaVedtak(
    val fnr: Fnr,
    val innsatsgruppe: ArenaInnsatsgruppe,
    val hovedmal: ArenaHovedmal?,
    val fraDato: LocalDate,
    val regUser: String,
    val operationTimestamp: LocalDateTime
) {

    companion object {
        @JvmStatic
        fun fraRecord(arenaVedtakRecord: ArenaVedtakRecord): ArenaVedtak? {
            if (ArenaInnsatsgruppe.erGyldig(arenaVedtakRecord.after.kvalifiseringsgruppe) &&
                (arenaVedtakRecord.after.hovedmal == null || ArenaHovedmal.erGyldig(arenaVedtakRecord.after.hovedmal))
            ) {
                return ArenaVedtak(
                    fnr = Fnr(arenaVedtakRecord.after.fnr),
                    innsatsgruppe = ArenaInnsatsgruppe.valueOf(arenaVedtakRecord.after.kvalifiseringsgruppe),
                    hovedmal = arenaVedtakRecord.after.hovedmal?.let { ArenaHovedmal.valueOf(it) },
                    fraDato = LocalDate.parse(arenaVedtakRecord.after.fraDato, ISO_LOCAL_DATE_MIDNIGHT),
                    regUser = arenaVedtakRecord.after.regUser,
                    operationTimestamp = LocalDateTime.parse(arenaVedtakRecord.opTs, ISO_LOCAL_DATE_TIME_WITHOUT_T)
                )
            }
            return null
        }
    }

    enum class ArenaInnsatsgruppe {
        BATT, BFORM, IKVAL, VARIG;

        companion object {
            fun erGyldig(value: String): Boolean {
                return values().map { it.name }.contains(value)
            }

            fun tilInnsatsgruppe(arenaInnsatsgruppe: ArenaInnsatsgruppe): Innsatsgruppe =
                when (arenaInnsatsgruppe) {
                    BATT -> Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
                    BFORM -> Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
                    IKVAL -> Innsatsgruppe.STANDARD_INNSATS
                    VARIG -> Innsatsgruppe.VARIG_TILPASSET_INNSATS
                }
        }
    }

    enum class ArenaHovedmal {
        OKEDELT, SKAFFEA, BEHOLDEA;

        companion object {
            fun erGyldig(value: String): Boolean {
                return values().map { it.name }.contains(value)
            }
        }
    }

    /**
     * Siden ArenaVedtak.fraDato har ikke tid, så brukes ArenaVedtak.operationTimestamp fra Kafka-melding til å legge på
     * tid for å kunne finne ut hvilket vedtak som er det nyeste dersom det på samme dag fattes flere vedtak i Arena og
     * eventuelt i denne løsningen, for samme bruker.
     */
    fun beregnetFattetTidspunkt(): LocalDateTime {
        if (operationTimestamp.toLocalDate().equals(fraDato)) {
            return operationTimestamp
        }
        return LocalDateTime.of(fraDato, LocalTime.MIDNIGHT)
    }
}
