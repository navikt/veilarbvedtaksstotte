package no.nav.veilarbvedtaksstotte.service

import com.nimbusds.jwt.JWTClaimsSet
import io.getunleash.DefaultUnleash
import no.nav.common.abac.AbacClient
import no.nav.common.abac.Pep
import no.nav.common.abac.constants.NavAttributter
import no.nav.common.abac.constants.StandardAttributter
import no.nav.common.abac.domain.Attribute
import no.nav.common.abac.domain.request.*
import no.nav.common.abac.domain.response.Decision
import no.nav.common.abac.domain.response.Response
import no.nav.common.abac.domain.response.XacmlResponse
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.common.utils.Credentials
import no.nav.common.utils.Pair
import no.nav.poao_tilgang.client.*
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.text.ParseException
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors

@Service
class AuthService(
    private val aktorOppslagClient: AktorOppslagClient,
    private val veilarbPep: Pep,
    private val veilarbarenaService: VeilarbarenaService,
    private val abacClient: AbacClient,
    private val serviceUserCredentials: Credentials,
    private val authContextHolder: AuthContextHolder,
    private val utrullingService: UtrullingService,
    private val poaoTilgangClient: PoaoTilgangClient,
    private val unleashService: DefaultUnleash
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun sjekkVeilederTilgangTilBruker(fnr: Fnr) {
        sjekkVeilederTilgangTilBruker({ fnr }) { aktorOppslagClient.hentAktorId(fnr) }
    }

    fun sjekkVeilederTilgangTilBruker(aktorId: AktorId) {
        sjekkVeilederTilgangTilBruker({ aktorOppslagClient.hentFnr(aktorId) }) { aktorId }
    }

    fun sjekkTilgangTilBrukerOgEnhet(fnr: Fnr): AuthKontekst {
        return sjekkTilgangTilBrukerOgEnhet({ fnr }) { aktorOppslagClient.hentAktorId(fnr) }
    }

    fun sjekkTilgangTilBrukerOgEnhet(aktorId: AktorId): AuthKontekst {
        return sjekkTilgangTilBrukerOgEnhet({ aktorOppslagClient.hentFnr(aktorId) }) { aktorId }
    }

    fun sjekkEksternbrukerTilgangTilBruker(fnr: Fnr) {
        harSikkerhetsNivaa4()
        val desicion = poaoTilgangClient.evaluatePolicy(
            EksternBrukerTilgangTilEksternBrukerPolicyInput(
                hentInnloggetPersonIdent(), fnr.get()
            )
        ).getOrThrow()
        if (desicion.isDeny) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
    }

    private fun sjekkVeilederTilgangTilBruker(
        fnrSupplier: Supplier<Fnr>,
        aktorIdSupplier: Supplier<AktorId>
    ): Pair<Fnr, AktorId> {
        sjekkInternBruker()
        val fnr = fnrSupplier.get()
        val aktorId = aktorIdSupplier.get()

        val harVeilederTilgangTilPerson =
            veilarbPep.harVeilederTilgangTilPerson(NavIdent.of(innloggetVeilederIdent), ActionId.WRITE, aktorId)

            poaoTilgangClient.evaluatePolicy(
                NavAnsattTilgangTilEksternBrukerPolicyInput(
                    hentInnloggetVeilederUUID(), TilgangType.SKRIVE, fnr.get()
                )
            ).getOrThrow()

        if (!harVeilederTilgangTilPerson) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        return Pair.of(fnr, aktorId)
    }

    private fun sjekkTilgangTilBrukerOgEnhet(
        fnrSupplier: Supplier<Fnr>,
        aktorIdSupplier: Supplier<AktorId>
    ): AuthKontekst {
        val fnrAktorIdPair = sjekkVeilederTilgangTilBruker(fnrSupplier, aktorIdSupplier)
        val fnr = fnrAktorIdPair.first
        val aktorId = fnrAktorIdPair.second
        val enhet = sjekkTilgangTilEnhet(fnr.get())
        return AuthKontekst(fnr = fnr.get(), aktorId = aktorId.get(), oppfolgingsenhet = enhet)
    }

    val innloggetVeilederIdent: String
        get() = authContextHolder
            .navIdent
            .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Fant ikke ident for innlogget veileder") }
            .get()

    fun hentInnloggetVeilederUUID(): UUID =
            authContextHolder
            .idTokenClaims.flatMap { authContextHolder.getStringClaim(it, "oid") }
            .map { UUID.fromString(it) }
            .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Fant ikke oid for innlogget veileder") }


    fun getFnrOrThrow(aktorId: String?): Fnr {
        return aktorOppslagClient.hentFnr(AktorId.of(aktorId))
    }

    fun sjekkErAnsvarligVeilederFor(vedtak: Vedtak) {
        if (vedtak.veilederIdent != innloggetVeilederIdent) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke ansvarlig veileder.")
        }
    }

    fun harInnloggetVeilederTilgangTilBrukere(brukerFnrs: List<String?>): Map<String, Boolean> {
        val request = lagSjekkTilgangRequest(serviceUserCredentials.username, innloggetVeilederIdent, brukerFnrs)
        val abacResponse = abacClient.sendRequest(request)
        return mapBrukerTilgangRespons(abacResponse)
    }

    fun lagSjekkTilgangRequest(
        systembrukerNavn: String?,
        veilederIdent: String?,
        brukerFnrs: List<String?>
    ): XacmlRequest {
        val environment = Environment()
        environment.addAttribute(Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, systembrukerNavn))
        val action = Action()
        action.addAttribute(Attribute(StandardAttributter.ACTION_ID, ActionId.WRITE.name))
        val accessSubject = AccessSubject()
        accessSubject.addAttribute(Attribute(StandardAttributter.SUBJECT_ID, veilederIdent))
        accessSubject.addAttribute(Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, "InternBruker"))
        val resources = brukerFnrs.stream()
            .map { fnr: String? -> mapBrukerFnrTilAbacResource(fnr) }
            .collect(Collectors.toList())
        val request = Request()
            .withEnvironment(environment)
            .withAction(action)
            .withAccessSubject(accessSubject)
            .withResources(resources)
        return XacmlRequest().withRequest(request)
    }

    private fun mapBrukerFnrTilAbacResource(fnr: String?): Resource {
        val resource = Resource()
        resource.addAttribute(Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, "veilarb"))
        resource.addAttribute(
            Attribute(
                NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE,
                NavAttributter.RESOURCE_VEILARB_PERSON
            )
        )
        resource.addAttribute(Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, fnr, true))
        return resource
    }

    fun mapBrukerTilgangRespons(xacmlResponse: XacmlResponse): Map<String, Boolean> {
        val tilgangTilBrukere: MutableMap<String, Boolean> = HashMap()
        xacmlResponse.response.forEach(Consumer { response: Response ->
            val harTilgang = response.decision == Decision.Permit

            // There should always be a single category
            val category = response.category[0]
            val brukerFnr = category.attribute.value
            tilgangTilBrukere[brukerFnr] = harTilgang
        })
        return tilgangTilBrukere
    }

    private fun sjekkInternBruker() {
        authContextHolder
            .role
            .filter { role: UserRole -> role == UserRole.INTERN }
            .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Ikke intern bruker") }
    }

    private fun sjekkTilgangTilEnhet(fnr: String): String {
        val enhet = veilarbarenaService.hentOppfolgingsenhet(Fnr.of(fnr))
            .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Enhet er ikke satt på bruker") }
        if (!utrullingService.erUtrullet(enhet)) {
            log.info("Vedtaksstøtte er ikke utrullet for enhet {}. Tilgang er stoppet", enhet)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Vedtaksstøtte er ikke utrullet for enheten")
        }

        val harVeilederTilgangTilEnhet =
            veilarbPep.harVeilederTilgangTilEnhet(NavIdent.of(innloggetVeilederIdent), enhet)

            poaoTilgangClient.evaluatePolicy(
                NavAnsattTilgangTilNavEnhetPolicyInput(
                    hentInnloggetVeilederUUID(), enhet.get()
                )
            ).getOrThrow()

        if (!harVeilederTilgangTilEnhet) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        return enhet.get()
    }

    fun erSystemBruker(): Boolean {
        return authContextHolder.erSystemBruker()
    }

    fun erEksternBruker(): Boolean {
        return authContextHolder.erEksternBruker()
    }

    fun harSystemTilSystemTilgang(): Boolean {
        return authContextHolder.erSystemBruker() && harAADRollerForSystemTilSystemTilgang(null)
    }

    fun harSystemTilSystemTilgangMedEkstraRolle(rolle: String?): Boolean {
        return authContextHolder.erSystemBruker() && harAADRollerForSystemTilSystemTilgang(rolle)
    }

    fun hentApplikasjonFraContex(): String? {
        return authContextHolder.idTokenClaims
            .flatMap { claims: JWTClaimsSet -> getStringClaimOrEmpty(claims, "azp_name") } //  "cluster:team:app"
            .map { claim: String ->
                claim.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            }
            .filter { claims: Array<String> -> claims.size == 3 }
            .map { claims: Array<String> -> claims[2] }
            .orElse(null)
    }

    private fun harAADRollerForSystemTilSystemTilgang(ekstraRolle: String?): Boolean {
        val roles = authContextHolder.idTokenClaims
            .flatMap { claims: JWTClaimsSet ->
                try {
                    return@flatMap Optional.ofNullable(claims.getStringListClaim("roles"))
                } catch (e: ParseException) {
                    return@flatMap Optional.empty<List<String>>()
                }
            }
            .orElse(emptyList())
        return roles.contains("access_as_application") && (ekstraRolle == null || roles.contains(ekstraRolle))
    }

    private fun hentInnloggetPersonIdent(): String {
        log.info("Henter personIdent fra claim")
        return authContextHolder
            .idTokenClaims.flatMap { authContextHolder.getStringClaim(it, "pid") }
            .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Kunne ikke hente pid fra token") }
    }

    private fun harSikkerhetsNivaa4() {
        log.info("Sjekker sikkerhetsnivå fra claim")
        val acrClaim = authContextHolder
            .idTokenClaims.flatMap { authContextHolder.getStringClaim(it, "acr") }

        if (acrClaim.isEmpty || acrClaim.get() != "Level4") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Kunne ikke hente acr fra token")
        }
    }

    companion object {
        private fun getStringClaimOrEmpty(claims: JWTClaimsSet, claimName: String): Optional<String> {
            return try {
                Optional.ofNullable(claims.getStringClaim(claimName))
            } catch (e: Exception) {
                Optional.empty()
            }
        }
    }
}
