package no.nav.fo.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class DbUtils {

    public static DataSource createAdminDataSource() {
        String dbUrl = getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY);
        return createDataSource(DbRole.ADMIN, dbUrl);
    }

    @SneakyThrows
    public static void migrateAndClose(DataSource dataSource) {
        migrate(dataSource);
        dataSource.getConnection().close();
    }

    public static void migrate(DataSource dataSource) {
        String dbUser = getDbuserForRole(DbRole.ADMIN);
        Flyway.configure()
                .dataSource(dataSource)
                .initSql(String.format("SET ROLE \"%s\"", dbUser))
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    public static long nesteFraSekvens(JdbcTemplate db, String sekvensNavn) {
        return db.queryForObject(String.format("select %s.nextval from dual", sekvensNavn), Long.class);
    }

    @SneakyThrows
    public static HikariDataSource createDataSource(DbRole dbRole, String dbUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, getMountPath(), getDbuserForRole(dbRole));
    }

    public static String getMountPath() {
        return "postgresql/" + getClusterName().orElseThrow(() -> new RuntimeException("Klarte ikke å hente cluster name"));
    }

    public static String getDbuserForRole(DbRole dbRole) {
        String namespace = requireNamespace();
        String environment = "default".equals(namespace) ? "p" : namespace;
        String role = EnumUtils.getName(dbRole).toLowerCase();

        return String.join("-", APPLICATION_NAME, environment, role);
    }

}
