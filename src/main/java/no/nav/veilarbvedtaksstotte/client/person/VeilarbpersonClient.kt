package no.nav.veilarbvedtaksstotte.client.person

import no.nav.common.health.HealthCheck
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.person.dto.Adressebeskyttelse
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto
import no.nav.veilarbvedtaksstotte.client.person.dto.PersonNavn
import no.nav.veilarbvedtaksstotte.client.person.dto.FoedselsdatoOgAar
import no.nav.veilarbvedtaksstotte.domain.Malform

interface VeilarbpersonClient : HealthCheck {
    fun hentPersonNavn(fnr: String): PersonNavn
    fun hentPersonNavnForJournalforing(fnr: String): PersonNavn
    fun hentCVOgJobbprofil(fnr: String): CvDto
    fun hentMalform(fnr: Fnr): Malform
    fun hentAdressebeskyttelse(fnr: Fnr): Adressebeskyttelse
    fun hentFødselsdato(fnr: Fnr): FoedselsdatoOgAar
}
