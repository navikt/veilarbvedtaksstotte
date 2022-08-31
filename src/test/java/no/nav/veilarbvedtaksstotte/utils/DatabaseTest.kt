package no.nav.veilarbvedtaksstotte.utils

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate

abstract class DatabaseTest {

    companion object {
        lateinit var jdbcTemplate: JdbcTemplate
        lateinit var transactor: TransactionTemplate

        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            jdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
            transactor = TransactionTemplate(DataSourceTransactionManager(jdbcTemplate.dataSource!!))
        }

        @AfterAll
        @JvmStatic
        fun shutdownDatabase() {
            jdbcTemplate.dataSource?.connection?.close()
        }
    }

}
