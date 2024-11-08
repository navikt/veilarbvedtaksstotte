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

    public static final String ARENA_BRUKER_CACHE_NAME = "arena-bruker";
    public static final String VEILEDER_ENHETER_CACHE_NAME = "veileder-enheter";
    public static final String VEILEDER_CACHE_NAME = "veileder";
    public static final String VEILEDER_NAVN_CACHE_NAME = "veileder-navn";
    public static final String ENHET_NAVN_CACHE_NAME = "enhet-navn";
    public static final String GJELDENDE_OPPFOLGINGPERIODE_CACHE_NAME = "gjeldende-oppfolgingperiode";
    public static final String OPPFOLGINGPERIODER_CACHE_NAME = "oppfolgingperioder";
    public static final String NORG2_ENHET_KONTAKTINFO_CACHE_NAME = "enhet-kontaktinfo";
    public static final String NORG2_ENHET_ORGANISERING_CACHE_NAME = "enhet-organisering";

    @Bean
    public Cache arenaBrukerCache() {
        return new CaffeineCache(ARENA_BRUKER_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
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
    public Cache veilederNavnCache() {
        return new CaffeineCache(VEILEDER_NAVN_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(10000)
                .build());
    }

    @Bean
    public Cache gjeldendeOppfolgingperiodeCache() {
        return new CaffeineCache(GJELDENDE_OPPFOLGINGPERIODE_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build());
    }

    @Bean
    public Cache oppfolgingperioderCache() {
        return new CaffeineCache(OPPFOLGINGPERIODER_CACHE_NAME, Caffeine.newBuilder()
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

    @Bean
    public Cache norg2EnhetKontaktinfoCache() {
        return new CaffeineCache(NORG2_ENHET_KONTAKTINFO_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build());
    }

    @Bean
    public Cache norg2EnhetOrganiseringCache() {
        return new CaffeineCache(NORG2_ENHET_ORGANISERING_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build());
    }
}
