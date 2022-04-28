package no.nav.veilarbvedtaksstotte.client.norg2;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.Value;

import java.time.LocalDate;

@Value
public class EnhetOrganisering {
    String orgType;
    @JsonDeserialize(using = LocalDateDeserializer.class)
    LocalDate fra;
    @JsonDeserialize(using = LocalDateDeserializer.class)
    LocalDate til;
    EnhetOrganiserer organiserer;
}
