package no.nav.veilarbvedtaksstotte.utils

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.*
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaHovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.utils.TestData.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

object TestdataGenerator {
    object OppfolgingsvedtakGenerator {
        fun genererRandomVedtak(
            id: Long = Random.nextLong(),
            aktorId: String = IdGenerator.genererRandomAktorId().toString(),
            hovedmal: Hovedmal = DiverseGenerator.velgRandomEnum<Hovedmal>(),
            innsatsgruppe: Innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            vedtakStatus: VedtakStatus = VedtakStatus.SENDT,
            utkastOpprettet: LocalDateTime = LocalDateTime.parse("2025-01-01T10:00:00.000000"),
            utkastSistOppdatert: LocalDateTime = LocalDateTime.parse("2025-01-02T10:00:00.000000"),
            vedtakFattet: LocalDateTime = LocalDateTime.parse("2025-01-03T10:00:00.000000"),
            begrunnelse: String = "Begrunnelsen",
            veilederIdent: String = TEST_VEILEDER_IDENT,
            veilederNavn: String = TEST_VEILEDER_NAVN,
            oppfolgingsenhetId: String = TEST_NAVKONTOR,
            oppfolgingsenhetNavn: String = TEST_OPPFOLGINGSENHET_NAVN,
            beslutterIdent: String = TEST_VEILEDER_IDENT_2,
            beslutterNavn: String = TEST_VEILEDER_2_NAVN,
            gjeldende: Boolean = false,
            opplysninger: List<String> = emptyList(),
            journalpostId: String = DiverseGenerator.genererRandomStrengAvTall(10),
            dokumentInfoId: String = DiverseGenerator.genererRandomStrengAvTall(10),
            dokumentbestillingId: String = DiverseGenerator.genererRandomStrengAvTall(10),
            beslutterProsessStatus: BeslutterProsessStatus = BeslutterProsessStatus.GODKJENT_AV_BESLUTTER,
            referanse: UUID = UUID.randomUUID(),
        ): Vedtak {
            val vedtak = Vedtak()
            vedtak.setId(id)
            vedtak.setAktorId(aktorId)
            vedtak.setHovedmal(hovedmal)
            vedtak.setInnsatsgruppe(innsatsgruppe)
            vedtak.setVedtakStatus(vedtakStatus)
            vedtak.setUtkastSistOppdatert(utkastSistOppdatert)
            vedtak.setVedtakFattet(vedtakFattet)
            vedtak.setUtkastOpprettet(utkastOpprettet)
            vedtak.setBegrunnelse(begrunnelse)
            vedtak.setVeilederIdent(veilederIdent)
            vedtak.setVeilederNavn(veilederNavn)
            vedtak.setOppfolgingsenhetId(oppfolgingsenhetId)
            vedtak.setOppfolgingsenhetNavn(oppfolgingsenhetNavn)
            vedtak.setBeslutterIdent(beslutterIdent)
            vedtak.setBeslutterNavn(beslutterNavn)
            vedtak.setGjeldende(gjeldende)
            vedtak.setOpplysninger(opplysninger)
            vedtak.setJournalpostId(journalpostId)
            vedtak.setDokumentInfoId(dokumentInfoId)
            vedtak.setDokumentbestillingId(dokumentbestillingId)
            vedtak.setBeslutterProsessStatus(beslutterProsessStatus)
            vedtak.setReferanse(referanse)
            return vedtak
        }

        fun genererRandomArenaVedtak(
            fnr: Fnr = IdGenerator.genererRandomFnr(),
            innsatsgruppe: ArenaInnsatsgruppe = DiverseGenerator.velgRandomEnum<ArenaInnsatsgruppe>(),
            hovedmal: ArenaHovedmal? = DiverseGenerator.velgRandomEnum<ArenaHovedmal>(),
            fraDato: LocalDate = LocalDate.parse("2025-01-01"),
            regUser: String = "REG_USER",
            operationTimestamp: LocalDateTime = LocalDateTime.parse("2025-01-01T10:00:00.000000"),
            hendelseId: Long = Random.nextLong(),
            vedtakId: Long = Random.nextLong()
        ): ArenaVedtak {
            return ArenaVedtak(
                fnr = fnr,
                innsatsgruppe = innsatsgruppe,
                hovedmal = hovedmal,
                fraDato = fraDato,
                regUser = regUser,
                operationTimestamp = operationTimestamp,
                hendelseId = hendelseId,
                vedtakId = vedtakId,
            )
        }
    }

    object IdGenerator {
        fun genererRandomFnr(): Fnr {
            return Fnr.of(DiverseGenerator.genererRandomStrengAvTall(11))
        }

        fun genererRandomAktorId(): AktorId {
            return AktorId.of(DiverseGenerator.genererRandomStrengAvTall(13))
        }
    }

    object DiverseGenerator {
        fun genererRandomStrengAvTall(lengde: Long = 1): String {
            if (lengde < 0) {
                throw IllegalStateException("Lengde må være større enn 0.")
            }

            return Random.nextLong(lengde).toString()
        }

        inline fun <reified T : Enum<T>> velgRandomEnum(): T {
            val enumVerdier = enumValues<T>()
            val randomIndeks = Random.nextInt(enumVerdier.size)
            return enumVerdier[randomIndeks]
        }
    }
}
