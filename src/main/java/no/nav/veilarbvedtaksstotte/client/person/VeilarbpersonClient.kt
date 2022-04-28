package no.nav.veilarbvedtaksstotte.client.person

import no.nav.common.health.HealthCheck
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.Målform

interface VeilarbpersonClient : HealthCheck {
    fun hentPersonNavn(fnr: String): PersonNavn
    fun hentCVOgJobbprofil(fnr: String): String
    fun hentMålform(fnr: Fnr): Målform
}
