package no.nav.veilarbvedtaksstotte.utils

import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName

class SingletonKafkaContainer {

    companion object {
        var kafkaContainer: ConfluentKafkaContainer? = null

        fun init(): ConfluentKafkaContainer {
            if (kafkaContainer == null) {
                kafkaContainer = ConfluentKafkaContainer(DockerImageName.parse(ApplicationTestConfig.KAFKA_IMAGE))
                kafkaContainer?.start()
                setupShutdownHook()
            }
            return kafkaContainer!!
        }

        private fun setupShutdownHook() {
            Runtime.getRuntime().addShutdownHook(Thread {
                if (kafkaContainer != null) {
                    kafkaContainer?.stop()
                }
            })
        }
    }
}
