package no.nav.veilarbvedtaksstotte.utils

import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class SingletonKafkaContainer {

    companion object {
        var kafkaContainer: KafkaContainer? = null

        fun init(): KafkaContainerWrapper {
            if (kafkaContainer == null) {
                kafkaContainer = KafkaContainer(DockerImageName.parse(ApplicationTestConfig.KAFKA_IMAGE))
                kafkaContainer?.start()
                setupShutdownHook()
            }
            return KafkaContainerWrapper(kafkaContainer!!)
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

// Wrapper KafkaContainer i egen type for å kontrollere start og stopp av container.
// En instans for alle tester i stede for ny instans for hver test.
data class KafkaContainerWrapper(val kafkaContainer: KafkaContainer)
