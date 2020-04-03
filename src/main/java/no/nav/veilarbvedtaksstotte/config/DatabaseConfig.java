package no.nav.veilarbvedtaksstotte.config;

import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.jdbc.Database;
import no.nav.sbl.jdbc.Transactor;
import no.nav.veilarbvedtaksstotte.utils.DbRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbvedtaksstotte.utils.DbUtils.createDataSource;

@Configuration
public class DatabaseConfig {
    public static final String VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY = "VEILARBVEDTAKSSTOTTE_DB_URL";

    @Bean
    public DataSource dataSource() {
        String dbUrl = getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY);
        return createDataSource(dbUrl, DbRole.USER);
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public Database database(JdbcTemplate jdbcTemplate) {
        return new Database(jdbcTemplate);
    }

    @Bean
    public Transactor transactor(PlatformTransactionManager platformTransactionManager) {
        return new Transactor(platformTransactionManager);
    }

    @Bean
    public Pingable dbPinger(JdbcTemplate db) {
        HelsesjekkMetadata metadata = new HelsesjekkMetadata("db",
                "Database: " + getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY),
                "Database for veilarbvedtaksstotte",
                true);

        return () -> {
            try {
                db.execute("SELECT 1");
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }
}
