package no.nav.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import lombok.SneakyThrows;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class DbUtils {

    public static DataSource createDataSource(String dbUrl, DbRole dbRole) {
        HikariConfig config = createDataSourceConfig(dbUrl);
        return createVaultRefreshDataSource(config, dbRole);
    }

    @SneakyThrows
    public static void migrateAndClose(DataSource dataSource, DbRole dbRole) {
        migrate(dataSource, dbRole);
        dataSource.getConnection().close();
    }

    public static void migrate(DataSource dataSource, DbRole dbRole) {
        Flyway.configure()
                .dataSource(dataSource)
                .initSql(String.format("SET ROLE \"%s\"", toDbRoleStr(dbRole)))
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    public static HikariConfig createDataSourceConfig(String dbUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        return config;
    }

    @SneakyThrows
    private static DataSource createVaultRefreshDataSource(HikariConfig config, DbRole dbRole) {
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, getMountPath(), toDbRoleStr(dbRole));
    }

    private static String getMountPath() {
        boolean isProd = getEnvironmentClass() == EnviromentClass.P;
        return "postgresql/" + (isProd ? "prod-fss" : "preprod-fss");
    }

    public static String toDbRoleStr(DbRole dbRole) {
        String namespace = requireNamespace();
        String environment = "default".equals(namespace) ? "p" : namespace;
        String role = EnumUtils.getName(dbRole).toLowerCase();

        return String.join("-", APPLICATION_NAME, environment, role);
    }

}
