package no.nav.veilarbvedtaksstotte.utils

import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class SingletonKafkaContainer {

    companion object {
        var kafkaContainer: KafkaContainer? = null

        fun init(): KafkaContainer {
            if (kafkaContainer == null) {
                kafkaContainer = KafkaContainer(DockerImageName.parse(ApplicationTestConfig.KAFKA_IMAGE))
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
