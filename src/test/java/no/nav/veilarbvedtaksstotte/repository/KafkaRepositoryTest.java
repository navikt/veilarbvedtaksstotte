package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.kafka.KafkaTopics;
import no.nav.veilarbvedtaksstotte.repository.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.repository.domain.MeldingType;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.TEST_AKTOR_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaRepositoryTest {

    private static JdbcTemplate db;
    private static KafkaRepository kafkaRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        kafkaRepository = new KafkaRepository(db);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void skal_lage_og_hente_feilet_melding() {
        String key = TEST_AKTOR_ID;
        String jsonPayload = "{}";

        kafkaRepository.lagreFeiletProdusertKafkaMelding(KafkaTopics.Topic.VEDTAK_SENDT, key, jsonPayload);

        List<FeiletKafkaMelding> feiledeVedtakSendt = kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.PRODUCED);

        assertEquals(1, feiledeVedtakSendt.size());
        assertEquals(feiledeVedtakSendt.get(0).getKey(), key);
        assertEquals(feiledeVedtakSendt.get(0).getJsonPayload(), jsonPayload);
    }

    @Test
    public void skal_slette_feilet_melding() {
        kafkaRepository.lagreFeiletProdusertKafkaMelding(KafkaTopics.Topic.VEDTAK_SENDT, TEST_AKTOR_ID, "{}");
        List<FeiletKafkaMelding> feiletKafkaMeldinger = kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.PRODUCED);
        kafkaRepository.slettFeiletKafkaMelding(feiletKafkaMeldinger.get(0).getId());

        assertTrue(kafkaRepository.hentFeiledeKafkaMeldinger(MeldingType.PRODUCED).isEmpty());
    }

}
