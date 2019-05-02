package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.client.ModiaContextClient;
import no.nav.fo.veilarbvedtaksstotte.client.PersonClient;
import no.nav.fo.veilarbvedtaksstotte.client.SAFClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import({ DokumentClient.class, PersonClient.class, ModiaContextClient.class, SAFClient.class})
public class ClientConfig {}
