package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerRegisteretService;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ProfileringResponse;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ProfilertTil;
import no.nav.veilarbvedtaksstotte.client.dokument.MalType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MalTypeService {

    private final ArbeidssoekerRegisteretService arbeidssoekerRegisteretService;

    @Autowired
    public MalTypeService(ArbeidssoekerRegisteretService arbeidssoekerRegisteretService) {
        this.arbeidssoekerRegisteretService = arbeidssoekerRegisteretService;
    }

    public MalType utledMalTypeFraVedtak(Vedtak vedtak, Fnr fnr) {
        Innsatsgruppe innsatsgruppe = vedtak.getInnsatsgruppe();
        Hovedmal hovedmal = vedtak.getHovedmal();

        if (Innsatsgruppe.STANDARD_INNSATS.equals(innsatsgruppe) && Hovedmal.SKAFFE_ARBEID.equals(hovedmal)) {
            OpplysningerOmArbeidssoekerMedProfilering opplysningerOmArbeidssoeker = arbeidssoekerRegisteretService.hentSisteOpplysningerOmArbeidssoekerMedProfilering(fnr);

            if (opplysningerOmArbeidssoeker != null && opplysningerOmArbeidssoeker.getProfilering() != null) {
                ProfileringResponse profilering = opplysningerOmArbeidssoeker.getProfilering();

                if (profilering != null && profilering.getProfilertTil() == ProfilertTil.ANTATT_GODE_MULIGHETER) {
                    return MalType.STANDARD_INNSATS_SKAFFE_ARBEID_PROFILERING;
                }
            }
        }

        return utledMalType(innsatsgruppe, hovedmal);
    }

    private MalType utledMalType(Innsatsgruppe innsatsgruppe, Hovedmal hovedmal) {

        switch (innsatsgruppe) {
            case STANDARD_INNSATS:
                return hovedmal == Hovedmal.SKAFFE_ARBEID ? MalType.STANDARD_INNSATS_SKAFFE_ARBEID
                        : MalType.STANDARD_INNSATS_BEHOLDE_ARBEID;
            case SITUASJONSBESTEMT_INNSATS:
                return hovedmal == Hovedmal.SKAFFE_ARBEID ? MalType.SITUASJONSBESTEMT_INNSATS_SKAFFE_ARBEID
                        : MalType.SITUASJONSBESTEMT_INNSATS_BEHOLDE_ARBEID;
            case SPESIELT_TILPASSET_INNSATS:
                return hovedmal == Hovedmal.SKAFFE_ARBEID ? MalType.SPESIELT_TILPASSET_INNSATS_SKAFFE_ARBEID
                        : MalType.SPESIELT_TILPASSET_INNSATS_BEHOLDE_ARBEID;
            case GRADERT_VARIG_TILPASSET_INNSATS:
                return MalType.GRADERT_VARIG_TILPASSET_INNSATS;
            case VARIG_TILPASSET_INNSATS:
                return MalType.VARIG_TILPASSET_INNSATS;
        }

        throw new IllegalStateException("Klarte ikke å mappe vedtak til mal type");
    }

}
