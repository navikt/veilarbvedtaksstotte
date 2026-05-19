package no.nav.veilarbvedtaksstotte.klagebehandling.controller

import no.nav.veilarbvedtaksstotte.klagebehandling.service.Feil
import org.springframework.http.ProblemDetail

data class KlagebehandlingFeilRepons(
    val feilkode: Feil.Årsak
) : ProblemDetail()