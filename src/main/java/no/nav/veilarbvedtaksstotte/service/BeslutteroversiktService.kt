package no.nav.veilarbvedtaksstotte.service

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
        val sokMedEnheter = sok.copy(filter = sok.filter.copy(enheter = enheterFilter))

        val resultat = beslutteroversiktRepository.sokEtterBrukere(sokMedEnheter, veilederIdent)
        return resultat.copy(brukere = sensurerBrukere(resultat.brukere))
    }

    // Tomt filter betyr alle enhetene veilederen har tilgang til.
    // Søkte enheter utenfor veilederens enheter, også null, gir 403.
    private fun bestemEnheter(sokteEnheter: List<String?>, veilederEnheter: List<String>): List<String?> {
        if (sokteEnheter.isEmpty()) return veilederEnheter
        if (!veilederEnheter.containsAll(sokteEnheter)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder mangler tilgang til enhet")
        }
        return sokteEnheter
    }

    private fun hentVeilederEnheter(): List<String> {
        val enheter = checkNotNull(veiledereOgEnhetClient.hentInnloggetVeilederEnheter()?.enhetliste) {
            "Fant ikke veilederens enheter"
        }
        return enheter.mapTo(ArrayList()) { enhet ->
            checkNotNull(enhet?.enhetId) { "Veilederens enhet mangler identifikator" }
        }
    }

    // Returnerer brukerne i samme rekkefølge. Brukere veilederen ikke har bekreftet tilgang til,
    // får tomt fnr og navn. Elementene kan være null fordi Java-kode kaller funksjonen direkte.
    fun sensurerBrukere(brukere: List<BeslutteroversiktBruker?>): List<BeslutteroversiktBruker> {
        if (brukere.isEmpty()) return emptyList()

        val brukereMedFnr = brukere.map { bruker ->
            checkNotNull(bruker) { "Brukerlisten inneholder en tom bruker" }
            bruker to checkNotNull(bruker.brukerFnr) { "Bruker mangler identifikator" }
        }
        val tilgang = authService.harInnloggetVeilederTilgangTilBrukere(brukereMedFnr.map { (_, fnr) -> fnr })

        return brukereMedFnr.map { (bruker, fnr) ->
            if (tilgang[fnr] == true) bruker
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
