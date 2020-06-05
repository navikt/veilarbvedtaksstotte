package no.nav.veilarbvedtaksstotte;

import no.nav.common.utils.SslUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VeilarbvedtaksstotteApp {

    public static void main(String... args) {
        SslUtils.setupTruststore();
        SpringApplication.run(VeilarbvedtaksstotteApp.class, args);
    }

}
