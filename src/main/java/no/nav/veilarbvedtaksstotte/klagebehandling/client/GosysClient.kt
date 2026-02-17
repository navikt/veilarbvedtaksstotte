package no.nav.veilarbvedtaksstotte.klagebehandling.client

import no.nav.common.health.HealthCheck

interface GosysClient : HealthCheck {
    fun hentOppgave()
}



