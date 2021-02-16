package no.nav.veilarbvedtaksstotte;

import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@EnableAutoConfiguration
@Import(ApplicationTestConfig.class)
public class VeilarbvedtaksstotteTestApp {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VeilarbvedtaksstotteTestApp.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }

}
