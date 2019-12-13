package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient;
import no.nav.fo.veilarbvedtaksstotte.domain.RegistreringData;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.MalType;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class MalTypeService {

    private RegistreringClient registreringClient;

    @Inject
    public MalTypeService(RegistreringClient registreringClient){
        this.registreringClient = registreringClient;
    }

    public MalType utledMalTypeFraVedtak(Vedtak vedtak, String fnr) {

        Innsatsgruppe innsatsgruppe = vedtak.getInnsatsgruppe();
        Hovedmal hovedmal = vedtak.getHovedmal();

        if (Innsatsgruppe.STANDARD_INNSATS.equals(innsatsgruppe) && Hovedmal.SKAFFE_ARBEID.equals(hovedmal)) {
            RegistreringData registreringData = registreringClient.hentRegistreringData(fnr);

            if (registreringData != null) {
                RegistreringData.Profilering profilering = registreringData.registrering.profilering;

                // Sykmeldte brukere har ikke profilering
                if (profilering != null && profilering.innsatsgruppe == RegistreringData.ProfilertInnsatsgruppe.STANDARD_INNSATS) {
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
