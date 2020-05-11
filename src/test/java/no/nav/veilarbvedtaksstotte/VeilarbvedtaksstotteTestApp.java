package no.nav.veilarbvedtaksstotte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VeilarbvedtaksstotteTestApp {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VeilarbvedtaksstotteTestApp.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }

}
