package no.nav.veilarbvedtaksstotte.service

import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbvedtaksstotte.client.norg2.*
import no.nav.veilarbvedtaksstotte.utils.TestUtils.givenWiremockOkJsonResponse
import no.nav.veilarbvedtaksstotte.utils.TestUtils.readTestResourceFile
import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class EnhetInfoServiceTest {

    private lateinit var enhetInfoService: EnhetInfoService

    private val enhetId = EnhetId.of("1234")
    private val eierEnhetId = EnhetId.of("4321")
    private val gyldigFra = LocalDate.now().minusDays(2)
    private val ugyldigFra = LocalDate.now().plusDays(1)
    private val gyldigTil = LocalDate.now().plusDays(2)
    private val ugyldigTil = LocalDate.now().minusDays(1)

    private val wireMockRule = WireMockRule()

    @Rule
    fun getWireMockRule() = wireMockRule

    @Before
    fun setup() {
        val enhetClient: Norg2Client = Norg2ClientImpl("http://localhost:" + wireMockRule.port())
        enhetInfoService = EnhetInfoService(enhetClient)
    }

    @Test
    fun finner_riktig_enhet() {
        val json: String = readTestResourceFile("norg2/enheter.json")
        givenWiremockOkJsonResponse("/api/v1/enhet?enhetStatusListe=AKTIV", json)
        val enhet = enhetInfoService.hentEnhet(enhetId)
        assertEquals(enhetId.get(), enhet.enhetNr)
        assertEquals("NAV Enhet 2", enhet.navn)
    }

    @Test
    fun opprinnelig_enhet_som_avsenderenhet_dersom_enhet_har_postboksadresse() {
        gittEnhetMedKontaktinfoPostboksadresse(enhetId)
        val enhetKontaktinformasjon: EnhetKontaktinformasjon = enhetInfoService.utledEnhetKontaktinformasjon(enhetId)
        assertEquals(enhetId, enhetKontaktinformasjon.enhetNr)
        Assert.assertTrue(enhetKontaktinformasjon.postadresse is EnhetPostboksadresse)
        val adresse: EnhetPostboksadresse = enhetKontaktinformasjon.postadresse as EnhetPostboksadresse
        assertEquals(adresse.postnummer, "1234")
        assertEquals(adresse.poststed, "STED")
        assertEquals(adresse.postboksnummer, "1")
        assertEquals(adresse.postboksanlegg, "Anlegg")
    }

    @Test
    fun opprinnelig_enhet_som_avsenderenhet_dersom_enhet_har_stedssadresse() {
        gittEnhetMedKontaktinfoStedsadresse(enhetId)
        val enhetKontaktinformasjon: EnhetKontaktinformasjon = enhetInfoService.utledEnhetKontaktinformasjon(enhetId)
        assertEquals(enhetId, enhetKontaktinformasjon.enhetNr)
        Assert.assertTrue(enhetKontaktinformasjon.postadresse is EnhetStedsadresse)
        val adresse: EnhetStedsadresse = enhetKontaktinformasjon.postadresse as EnhetStedsadresse
        assertEquals(adresse.postnummer, "1234")
        assertEquals(adresse.poststed, "STED")
        assertEquals(adresse.gatenavn, "GATE")
        assertEquals(adresse.husnummer, "1")
        assertEquals(adresse.husbokstav, "A")
        assertEquals(adresse.adresseTilleggsnavn, "Tilleggsnavn")
    }

    @Test
    fun eierenhet_som_avsenderenhet_dersom_opprinnelig_enhet_ikke_har_postadresse() {
        gittEnhetUtenKontaktinfo(enhetId)
        gittEnhetMedKontaktinfoPostboksadresse(eierEnhetId)
        val replace = HashMap<String, String?>()
        replace["ORG_TYPE_1"] = "EIER"
        replace["ENHET_NR_1"] = eierEnhetId.get()
        gittEnhetOrganiseringMed3Enheter(enhetId, replace)
        val enhetKontaktinformasjon: EnhetKontaktinformasjon = enhetInfoService.utledEnhetKontaktinformasjon(enhetId)
        assertEquals(eierEnhetId, enhetKontaktinformasjon.enhetNr)
    }

    @Test
    fun ingen_eier() {
        gittEnhetUtenKontaktinfo(enhetId)
        gittEnhetOrganiseringMed3Enheter(enhetId, HashMap())
        Assertions.assertThatThrownBy { enhetInfoService.utledEnhetKontaktinformasjon(enhetId) }
            .isExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun flere_eiere() {
        gittEnhetUtenKontaktinfo(enhetId)
        val replace = HashMap<String, String?>()
        replace["ORG_TYPE_1"] = "EIER"
        replace["ORG_TYPE_2"] = "EIER"
        gittEnhetOrganiseringMed3Enheter(enhetId, replace)
        Assertions.assertThatThrownBy { enhetInfoService.utledEnhetKontaktinformasjon(enhetId) }
            .isExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun eier_mangler_adresse() {
        gittEnhetUtenKontaktinfo(enhetId)
        gittEnhetUtenKontaktinfo(eierEnhetId)
        val replace = HashMap<String, String?>()
        replace["ORG_TYPE_1"] = "EIER"
        replace["ENHET_NR_1"] = eierEnhetId.get()
        gittEnhetOrganiseringMed3Enheter(enhetId, replace)
        Assertions.assertThatThrownBy { enhetInfoService.utledEnhetKontaktinformasjon(enhetId) }
            .isExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun eier_gyldig_fra_gyldig_til() {
        setupEierGyldighetTest(gyldigFra, gyldigTil)
        assertEquals(eierEnhetId, enhetInfoService.utledEnhetKontaktinformasjon(enhetId).enhetNr)
    }

    @Test
    fun eier_ugyldig_fra_gyldig_til() {
        setupEierGyldighetTest(ugyldigFra, gyldigTil)
        Assertions.assertThatThrownBy { enhetInfoService.utledEnhetKontaktinformasjon(enhetId) }
            .isExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun eier_gyldig_fra_ugyldig_til() {
        setupEierGyldighetTest(gyldigFra, ugyldigTil)
        Assertions.assertThatThrownBy { enhetInfoService.utledEnhetKontaktinformasjon(enhetId) }
            .isExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun eier_ugyldig_fra_ugyldig_til() {
        setupEierGyldighetTest(ugyldigFra, ugyldigTil)
        Assertions.assertThatThrownBy { enhetInfoService.utledEnhetKontaktinformasjon(enhetId) }
            .isExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun eier_gyldig_fra_null_til() {
        setupEierGyldighetTest(gyldigFra, null)
        assertEquals(eierEnhetId, enhetInfoService.utledEnhetKontaktinformasjon(enhetId).enhetNr)
    }

    @Test
    fun eier_ugyldig_fra_null_til() {
        setupEierGyldighetTest(ugyldigFra, null)
        Assertions.assertThatThrownBy { enhetInfoService.utledEnhetKontaktinformasjon(enhetId) }
            .isExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun eier_null_fra_gyldig_til() {
        setupEierGyldighetTest(null, gyldigTil)
        assertEquals(eierEnhetId, enhetInfoService.utledEnhetKontaktinformasjon(enhetId).enhetNr)
    }

    @Test
    fun eier_null_fra_ugyldig_til() {
        setupEierGyldighetTest(null, ugyldigTil)
        Assertions.assertThatThrownBy { enhetInfoService.utledEnhetKontaktinformasjon(enhetId) }
            .isExactlyInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun eier_lik_fra_lik_til() {
        setupEierGyldighetTest(LocalDate.now(), LocalDate.now())
        assertEquals(eierEnhetId, enhetInfoService.utledEnhetKontaktinformasjon(enhetId).enhetNr)
    }

    @Test
    fun eier_null_fra_null_til() {
        setupEierGyldighetTest(null, null)
        assertEquals(eierEnhetId, enhetInfoService.utledEnhetKontaktinformasjon(enhetId).enhetNr)
    }

    @Test
    fun har_telefonnummer() {
        gittEnhetMedKontaktinfoPostboksadresse(enhetId)
        val enhetKontaktinformasjon: EnhetKontaktinformasjon = enhetInfoService.utledEnhetKontaktinformasjon(enhetId)
        assertEquals(enhetKontaktinformasjon.telefonnummer, "12345678")
    }

    private fun gittEnhetUtenKontaktinfo(enhetId: EnhetId) {
        val json: String = readTestResourceFile("norg2/kontaktinformasjon_uten_adresse.json")
        val response = json.replace("ENHET_NR", enhetId.get())
        gittKontaktinformasjonResponse(enhetId, response)
    }

    private fun gittEnhetMedKontaktinfoStedsadresse(enhetId: EnhetId) {
        val json: String = readTestResourceFile("norg2/kontaktinformasjon_med_stedsadresse.json")
        val response = json.replace("ENHET_NR", enhetId.get())
        gittKontaktinformasjonResponse(enhetId, response)
    }

    private fun gittEnhetMedKontaktinfoPostboksadresse(enhetId: EnhetId) {
        val json: String = readTestResourceFile("norg2/kontaktinformasjon_med_postboksadresse.json")
        val response = json.replace("ENHET_NR", enhetId.get())
        gittKontaktinformasjonResponse(enhetId, response)
    }

    private fun gittKontaktinformasjonResponse(enhetId: EnhetId, response: String) {
        givenWiremockOkJsonResponse("/api/v1/enhet/" + enhetId.get() + "/kontaktinformasjon", response)
    }

    private fun gittEnhetOrganiseringMed3Enheter(enhetNr: EnhetId, replace: MutableMap<String, String?>) {
        val json: String = readTestResourceFile("norg2/organisering.json")

        replace.putIfAbsent("FRA_1", null)
        replace.putIfAbsent("TIL_1", null)
        replace.putIfAbsent("FRA_2", null)
        replace.putIfAbsent("TIL_2", null)
        replace.putIfAbsent("FRA_3", null)
        replace.putIfAbsent("TIL_3", null)

        val response = replace.entries.fold(json) { a, b ->
            a.replace(
                b.key,
                b.value ?: "null"
            )
        }

        givenWiremockOkJsonResponse("/api/v1/enhet/" + enhetNr.get() + "/organisering", response)
    }

    private fun setupEierGyldighetTest(gyldigFra: LocalDate?, gyldigTil: LocalDate?) {
        gittEnhetUtenKontaktinfo(enhetId)
        gittEnhetMedKontaktinfoPostboksadresse(eierEnhetId)
        val replace = HashMap<String, String?>()
        replace["ORG_TYPE_2"] = "EIER"
        replace["ENHET_NR_2"] = eierEnhetId.get()
        replace["FRA_2"] = wrapQuotes(formatDate(gyldigFra))
        replace["TIL_2"] = wrapQuotes(formatDate(gyldigTil))
        gittEnhetOrganiseringMed3Enheter(enhetId, replace)
    }

    private fun wrapQuotes(s: String?): String? {
        return Optional.ofNullable(s).map { x: String ->
            String.format(
                "\"%s\"",
                x
            )
        }.orElse(null)
    }

    private fun formatDate(date: LocalDate?): String? {
        return Optional.ofNullable(date).map { x: LocalDate ->
            x.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
            )
        }.orElse(null)
    }
}
