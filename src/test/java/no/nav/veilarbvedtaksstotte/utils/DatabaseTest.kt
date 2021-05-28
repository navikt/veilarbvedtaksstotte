package no.nav.veilarbvedtaksstotte.utils

import org.junit.AfterClass
import org.junit.BeforeClass
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate

abstract class DatabaseTest {

    companion object {
        lateinit var jdbcTemplate: JdbcTemplate
        lateinit var transactor: TransactionTemplate

        @BeforeClass
        @JvmStatic
        fun setupDatabase() {
            jdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate()
            transactor = TransactionTemplate(DataSourceTransactionManager(jdbcTemplate.dataSource!!))
        }

        @AfterClass
        @JvmStatic
        fun shutdownDatabase() {
            jdbcTemplate.dataSource?.connection?.close()
        }
    }

}
