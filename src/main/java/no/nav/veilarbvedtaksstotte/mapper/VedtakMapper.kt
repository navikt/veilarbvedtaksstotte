package no.nav.veilarbvedtaksstotte.mapper

import no.nav.veilarbvedtaksstotte.controller.dto.VedtakDTO
import no.nav.veilarbvedtaksstotte.controller.dto.VedtakUtkastDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeEntity
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak

fun toVedtakUtkastDTO(vedtak: Vedtak): VedtakUtkastDTO {
    return VedtakUtkastDTO(
        id = vedtak.id,
        hovedmal = vedtak.hovedmal,
        innsatsgruppe = vedtak.innsatsgruppe,
        utkastSistOppdatert = vedtak.utkastSistOppdatert,
        begrunnelse = vedtak.begrunnelse,
        veilederIdent = vedtak.veilederIdent,
        veilederNavn = vedtak.veilederNavn,
        oppfolgingsenhetId = vedtak.oppfolgingsenhetId,
        oppfolgingsenhetNavn = vedtak.oppfolgingsenhetNavn,
        beslutterIdent = vedtak.beslutterIdent,
        beslutterNavn = vedtak.beslutterNavn,
        opplysninger = vedtak.kilder?.map(KildeEntity::tekst),
        kilder = vedtak.kilder,
        beslutterProsessStatus = vedtak.beslutterProsessStatus,
        kanDistribueres = vedtak.kanDistribueres,
    )
}

fun toVedtakDTO(vedtak: Vedtak): VedtakDTO = VedtakDTO(
    id = vedtak.id,
    hovedmal = vedtak.hovedmal,
    innsatsgruppe = vedtak.innsatsgruppe,
    begrunnelse = vedtak.begrunnelse,
    veilederIdent = vedtak.veilederIdent,
    veilederNavn = vedtak.veilederNavn,
    oppfolgingsenhetId = vedtak.oppfolgingsenhetId,
    oppfolgingsenhetNavn = vedtak.oppfolgingsenhetNavn,
    beslutterIdent = vedtak.beslutterIdent,
    beslutterNavn = vedtak.beslutterNavn,
    opplysninger = vedtak.kilder?.map(KildeEntity::tekst) ?: emptyList(),
    kilder = vedtak.kilder,
    vedtakStatus = vedtak.vedtakStatus,
    utkastSistOppdatert = vedtak.utkastSistOppdatert,
    gjeldende = vedtak.isGjeldende,
    vedtakFattet = vedtak.vedtakFattet,
)
