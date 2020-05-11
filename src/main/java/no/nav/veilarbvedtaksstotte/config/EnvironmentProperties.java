package no.nav.veilarbvedtaksstotte.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.env")
public class EnvironmentProperties {

    private String openAmDiscoveryUrl;

    private String openAmClientId;

    private String stsDiscoveryUrl;

    private String abacUrl;

    private String aktorregisterUrl;

    private String dbUrl;

}
