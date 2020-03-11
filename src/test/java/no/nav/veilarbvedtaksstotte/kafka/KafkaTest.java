package no.nav.veilarbvedtaksstotte.kafka;

import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.KAFKA_BROKERS_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.springframework.kafka.test.utils.KafkaTestUtils.producerProps;

public abstract class KafkaTest {

    private static AnnotationConfigApplicationContext annotationConfigApplicationContext;

    protected static KafkaTemplate<String, String> kafkaTemplate;

    private static KafkaMessageListenerContainer<String, String> container;
    protected static BlockingQueue<ConsumerRecord<String, String>> records;

    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, KafkaTestConfig.KAFKA_TEST_TOPIC);

    @BeforeClass
    public static void configureKafkaBroker() throws Exception {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("template", "false", embeddedKafka);
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProperties);
        ContainerProperties containerProperties = new ContainerProperties(KafkaTestConfig.KAFKA_TEST_TOPIC);

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, String>) record -> records.add(record));
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());

        Map<String, Object> kafkaProps = producerProps(embeddedKafka);
        kafkaProps.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaProps));
        kafkaTemplate.setDefaultTopic(KafkaTestConfig.KAFKA_TEST_TOPIC);

        setProperty(KAFKA_BROKERS_URL_PROPERTY, embeddedKafka.getBrokersAsString(), PUBLIC);
    }

    @BeforeClass
    public static void setupFelles() {
        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(KafkaTestConfig.class);
        annotationConfigApplicationContext.start();
    }

    @Before
    public void injectAvhengigheter() {
        annotationConfigApplicationContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @AfterClass
    public static void stopSpringContext() {
        if (annotationConfigApplicationContext != null) {
            annotationConfigApplicationContext.stop();
        }
    }

    @After
    public void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @SneakyThrows
    protected void verifiserAsynkront(long timeout, TimeUnit unit, Runnable verifiser) {
        long timeoutMillis = unit.toMillis(timeout);
        boolean prosessert = false;
        boolean timedOut = false;
        long start = System.currentTimeMillis();
        while (!prosessert) {
            try {
                Thread.sleep(10);
                long current = System.currentTimeMillis();
                timedOut = current - start > timeoutMillis;
                verifiser.run();
                prosessert = true;
            } catch (Throwable a) {
                if (timedOut) {
                    throw a;
                }
            }
        }
    }
}
