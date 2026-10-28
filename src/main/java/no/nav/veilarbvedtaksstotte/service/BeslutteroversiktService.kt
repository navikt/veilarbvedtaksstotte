package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.NavIdent
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktSok
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BrukereMedAntall
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.EnhetMedNavn
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class BeslutteroversiktService(
    private val beslutteroversiktRepository: BeslutteroversiktRepository,
    private val veiledereOgEnhetClient: VeilarbveilederClient,
    private val authService: AuthService,
) {
    fun hentBeslutteroversikt(sok: BeslutteroversiktSok): BrukereMedAntall {
        val veilederIdent = NavIdent.of(authService.innloggetVeilederIdent)
        val enheterFilter = bestemEnheter(sok.filter.enheter, hentVeilederEnheter())

        // Repository tolker tom enhetsliste som «ingen enhetsfilter» og ville returnert utkast fra alle enheter.
        // En veileder uten enheter har ikke tilgang til noen utkast.
        if (enheterFilter.isEmpty()) return BrukereMedAntall(brukere = emptyList(), totaltAntall = 0)

        val sokMedEnheter = sok.copy(filter = sok.filter.copy(enheter = enheterFilter))

        val resultat = beslutteroversiktRepository.sokEtterBrukere(sokMedEnheter, veilederIdent)
        return resultat.copy(brukere = sensurerBrukere(resultat.brukere))
    }

    // Tomt filter betyr alle enhetene veilederen har tilgang til.
    // Søkte enheter utenfor veilederens enheter gir 403.
    private fun bestemEnheter(sokteEnheter: List<EnhetId>, veilederEnheter: List<EnhetId>): List<EnhetId> {
        if (sokteEnheter.isEmpty()) return veilederEnheter

        if (!veilederEnheter.containsAll(sokteEnheter)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder mangler tilgang til enhet")
        }

        return sokteEnheter
    }

    private fun hentVeilederEnheter(): List<EnhetId> {
        val enheter = checkNotNull(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()?.enhetliste) {
            "Fant ikke veilederens enheter"
        }
        return enheter.mapTo(ArrayList()) { enhet ->
            EnhetId(checkNotNull(enhet?.enhetId) { "Veilederens enhet mangler identifikator" })
        }
    }

    // Returnerer brukerne i samme rekkefølge. Brukere veilederen ikke har bekreftet tilgang til,
    // får tomt fnr og navn.
    fun sensurerBrukere(brukere: List<BeslutteroversiktBruker>): List<BeslutteroversiktBruker> {
        if (brukere.isEmpty()) return brukere

        val tilgang = authService.harInnloggetVeilederTilgangTilBrukere(brukere.map { it.brukerFnr })

        return brukere.map { bruker ->
            if (tilgang[bruker.brukerFnr] == true) bruker
            else bruker.copy(brukerFnr = "", brukerFornavn = "", brukerEtternavn = "")
        }
    }

    fun hentEnheterMedUtkast(): List<EnhetMedNavn> =
        beslutteroversiktRepository.hentEnheterMedUtkast()
            .filter { it.enhetId != VIKAFOSSEN_ENHET_ID || authService.harTilgangTilKode6Brukere() }

    private companion object {
        const val VIKAFOSSEN_ENHET_ID = "2103"
    }
}
