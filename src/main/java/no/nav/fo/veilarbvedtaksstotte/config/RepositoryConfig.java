package no.nav.fo.veilarbvedtaksstotte.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
public class RepositoryConfig {

    @Bean
    public VedtaksstotteRepository vedtaksstotteRepository(JdbcTemplate jdbcTemplate) {
        return new VedtaksstotteRepository(jdbcTemplate);
    }

}
