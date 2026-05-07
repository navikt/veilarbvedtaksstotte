package no.nav.veilarbvedtaksstotte.client.norg2;

import lombok.Value;

import java.time.LocalDate;

@Value
public class EnhetOrganisering {
    String orgType;
    LocalDate fra;
    LocalDate til;
    EnhetOrganiserer organiserer;
}
