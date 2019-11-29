package no.nav.fo.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class DbUtils {

    public static DataSource createAdminDataSource(String dbUrl) {
        // Admin data source does not need to be refreshed
        HikariConfig config = createDataSourceConfig(dbUrl);
        return createNormalDataSource(config);
    }

    public static DataSource createUserDataSource(String dbUrl) {
        HikariConfig config = createDataSourceConfig(dbUrl);
        return createVaultRefreshDataSource(config, DbRole.ADMIN);
    }

    @SneakyThrows
    public static void migrateAndClose(DataSource dataSource, DbRole dbRole) {
        migrate(dataSource, dbRole);
        dataSource.getConnection().close();
    }

    public static void migrate(DataSource dataSource, DbRole dbRole) {
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .initSql(String.format("SET ROLE \"%s\"", toDbRoleStr(dbRole)))
                .load()
                .migrate();
    }

    private static HikariConfig createDataSourceConfig(String dbUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        return config;
    }

    public static DataSource createNormalDataSource(HikariConfig config) {
        return new HikariDataSource(config);
    }

    @SneakyThrows
    public static DataSource createVaultRefreshDataSource(HikariConfig config, DbRole dbRole) {
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, getMountPath(), toDbRoleStr(dbRole));
    }

    private static String getMountPath() {
        return "postgresql/" + getClusterName().orElseThrow(() -> new RuntimeException("Klarte ikke å hente cluster name"));
    }

    public static String toDbRoleStr(DbRole dbRole) {
        String namespace = requireNamespace();
        String environment = "default".equals(namespace) ? "p" : namespace;
        String role = EnumUtils.getName(dbRole).toLowerCase();

        return String.join("-", APPLICATION_NAME, environment, role);
    }

}
