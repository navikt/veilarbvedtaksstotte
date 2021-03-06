package no.nav.veilarbvedtaksstotte.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String BRUKER_ENHET_CACHE_NAME = "bruker-enhet";
    public static final String VEILEDER_ENHETER_CACHE_NAME = "veileder-enheter";
    public static final String VEILEDER_CACHE_NAME = "veileder";
    public static final String ENHET_NAVN_CACHE_NAME = "enhet-navn";
    public static final String REGISTRERING_CACHE_NAME = "registrering";
    public static final String OPPFOLGING_CACHE_NAME = "oppfolging";
    public static final String OPPFOLGINGPERIODE_CACHE_NAME = "oppfolgingperiode";


    @Bean
    public Cache brukerEnhetCache() {
        return new CaffeineCache(BRUKER_ENHET_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(5000)
                .build());
    }

    @Bean
    public Cache veilederOgEnheterCache() {
        return new CaffeineCache(VEILEDER_ENHETER_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(5000)
                .build());
    }

    @Bean
    public Cache veilederCache() {
        return new CaffeineCache(VEILEDER_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(10000)
                .build());
    }

    @Bean
    public Cache registreringCache() {
        return new CaffeineCache(REGISTRERING_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());
    }

    @Bean
    public Cache oppfolgingCache() {
        return new CaffeineCache(OPPFOLGING_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());
    }

    @Bean
    public Cache oppfolgingperiodeCache() {
        return new CaffeineCache(OPPFOLGINGPERIODE_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());
    }

    @Bean
    public Cache enhetNavnCache() {
        return new CaffeineCache(ENHET_NAVN_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(2000)
                .build());
    }

}
