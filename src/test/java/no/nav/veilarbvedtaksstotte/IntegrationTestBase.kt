package no.nav.veilarbvedtaksstotte

import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [ApplicationTestConfig::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class IntegrationTestBase {
    companion object {
        @BeforeAll
        @JvmStatic
        fun initSystemProperties() {
            System.setProperty("KAFKA_SCHEMA_REGISTRY", "kafka_schema_registry")
            System.setProperty("KAFKA_SCHEMA_REGISTRY_USER", "kafka_schema_registry_user")
            System.setProperty("KAFKA_SCHEMA_REGISTRY_PASSWORD", "kafka_schema_registry_password")
        }
    }
}