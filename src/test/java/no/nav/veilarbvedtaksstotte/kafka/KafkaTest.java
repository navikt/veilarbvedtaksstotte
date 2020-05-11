package no.nav.veilarbvedtaksstotte.kafka;

import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class KafkaTest {

    private static AnnotationConfigApplicationContext annotationConfigApplicationContext;

    protected static KafkaTemplate<String, String> kafkaTemplate;

    private static KafkaMessageListenerContainer<String, String> container;
    protected static BlockingQueue<ConsumerRecord<String, String>> records;

    private static String[] topics = KafkaTestConfig.TOPICS.toArray(new String[0]);

//    @ClassRule
//    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, topics);

    @BeforeClass
    public static void configureKafkaBroker() throws Exception {
//        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("template", "false", embeddedKafka);
//        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProperties);
//        ContainerProperties containerProperties = new ContainerProperties(topics);
//
//        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
//        records = new LinkedBlockingQueue<>();
//        container.setupMessageListener((MessageListener<String, String>) record -> records.add(record));
//        container.start();
//        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic() * topics.length);
//
//        Map<String, Object> kafkaProps = producerProps(embeddedKafka);
//        kafkaProps.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//
//        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaProps));
//
//        setProperty(KAFKA_BROKERS_URL_PROPERTY, embeddedKafka.getBrokersAsString(), PUBLIC);
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
