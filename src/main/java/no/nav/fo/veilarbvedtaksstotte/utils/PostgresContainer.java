package no.nav.fo.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;

import javax.activation.DataSource;

public class PostgresContainer {

    private final static String DB_IMAGE = "postgres:11.5";
    private final static String DB_USER = "postgres";
    private final static int DB_PORT = 5432;

    private DataSorce dataSource;

    private GenericContainer container = new GenericContainer(DB_IMAGE);

    private String getDbContainerUrl() {
        String containerIp = container.getContainerIpAddress();
        String containerPort = container.getFirstMappedPort().toString();
        return String.format("jdbc:postgresql://%s:%s/postgres", containerIp, containerPort);
    }

    public GenericContainer getContainer() {
        return container;
    }

    public DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(getDbContainerUrl());
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
            config.setUsername(DB_USER);

            dataSource = new HikariDataSource(config);
        }
    }

    public JdbcTemplate getDb() {
        return new JdbcTemplate(getDataSource());
    }
}
