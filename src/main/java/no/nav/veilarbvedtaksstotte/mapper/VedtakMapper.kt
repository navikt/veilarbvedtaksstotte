package no.nav.veilarbvedtaksstotte.mapper

import no.nav.veilarbvedtaksstotte.controller.dto.VedtakUtkastDTO
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
        opplysninger = vedtak.opplysninger,
        beslutterProsessStatus = vedtak.beslutterProsessStatus,
        kanDistribueres = vedtak.kanDistribueres,
    )
}
