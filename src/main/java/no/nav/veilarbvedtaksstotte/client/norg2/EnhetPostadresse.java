package no.nav.veilarbvedtaksstotte.client.norg2;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EnhetPostboksadresse.class, name = "postboksadresse"),
        @JsonSubTypes.Type(value = EnhetStedsadresse.class, name = "stedsadresse")
})
public abstract class EnhetPostadresse {
}
