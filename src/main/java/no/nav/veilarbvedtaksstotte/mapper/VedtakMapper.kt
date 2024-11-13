package no.nav.veilarbvedtaksstotte.mapper

import no.nav.veilarbvedtaksstotte.controller.dto.VedtakDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak

fun toVedtakDTO(vedtak: Vedtak): VedtakDTO {
    return VedtakDTO(
        id = vedtak.id,
        aktorId = vedtak.aktorId,
        hovedmal = vedtak.hovedmal,
        innsatsgruppe = vedtak.innsatsgruppe,
        vedtakStatus = vedtak.vedtakStatus,
        utkastSistOppdatert = vedtak.utkastSistOppdatert,
        vedtakFattet = vedtak.vedtakFattet,
        utkastOpprettet = vedtak.utkastOpprettet,
        begrunnelse = vedtak.begrunnelse,
        veilederIdent = vedtak.veilederIdent,
        veilederNavn = vedtak.veilederNavn,
        oppfolgingsenhetId = vedtak.oppfolgingsenhetId,
        oppfolgingsenhetNavn = vedtak.oppfolgingsenhetNavn,
        beslutterIdent = vedtak.beslutterIdent,
        beslutterNavn = vedtak.beslutterNavn,
        gjeldende = vedtak.isGjeldende,
        opplysninger = vedtak.opplysninger,
        journalpostId = vedtak.journalpostId,
        dokumentInfoId = vedtak.dokumentInfoId,
        dokumentbestillingId = vedtak.dokumentbestillingId,
        beslutterProsessStatus = vedtak.beslutterProsessStatus,
        referanse = vedtak.referanse,
    )
}