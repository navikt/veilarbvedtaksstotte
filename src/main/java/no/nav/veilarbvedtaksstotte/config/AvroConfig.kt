package no.nav.veilarbvedtaksstotte.config

import org.apache.avro.util.ClassSecurityValidator
import org.springframework.context.annotation.Configuration

@Configuration("avroConfig")
class AvroConfig {

    init {
        val trustedPackages = listOf(
            "no.nav.pto_schema.kafka.avro.",
            "no.nav.person.pdl.aktor.v2.",
        )

        ClassSecurityValidator.setGlobal(
            ClassSecurityValidator.composite(
                ClassSecurityValidator.DEFAULT,
                { clazz -> trustedPackages.any { clazz.name.startsWith(it) } }
            )
        )
    }
}
