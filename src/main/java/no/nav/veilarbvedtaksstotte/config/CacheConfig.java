package no.nav.veilarbvedtaksstotte.config;


import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOR_ID_FROM_FNR_CACHE;
import static no.nav.dialogarena.aktor.AktorConfig.FNR_FROM_AKTOR_ID_CACHE;
import static no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext.ABAC_CACHE;


@Configuration
@EnableCaching
public class CacheConfig {

    public static final String VEILEDER_ENHETER_CACHE_NAME = "veileder-enheter";
    public static final String VEILEDER_CACHE_NAME = "veileder";
    public static final String ENHET_NAVN_CACHE_NAME = "enhet-navn";
    public static final String REGISTRERING_CACHE_NAME = "registrering";
    public static final String OPPFOLGING_CACHE_NAME = "oppfolging";

    private static final int ONE_DAY_IN_SECONDS = 60 * 60 * 24;
    private static final int FIVE_MINUTES_IN_SECONDS = 60 * 5;

    private static final CacheConfiguration VEILEDER_ENHETER_CACHE =
            new CacheConfiguration(VEILEDER_ENHETER_CACHE_NAME, 5000)
                    .memoryStoreEvictionPolicy(LRU)
                    .timeToIdleSeconds(ONE_DAY_IN_SECONDS)
                    .timeToLiveSeconds(ONE_DAY_IN_SECONDS);


    private static final CacheConfiguration VEILEDER_CACHE =
            new CacheConfiguration(VEILEDER_CACHE_NAME, 10000)
                    .memoryStoreEvictionPolicy(LRU)
                    .timeToIdleSeconds(ONE_DAY_IN_SECONDS)
                    .timeToLiveSeconds(ONE_DAY_IN_SECONDS);

    private static final CacheConfiguration REGISTRERING_CACHE =
            new CacheConfiguration(REGISTRERING_CACHE_NAME, 1000)
                    .memoryStoreEvictionPolicy(LRU)
                    .timeToIdleSeconds(FIVE_MINUTES_IN_SECONDS)
                    .timeToLiveSeconds(FIVE_MINUTES_IN_SECONDS);

    private static final CacheConfiguration OPPFOLGING_CACHE =
            new CacheConfiguration(OPPFOLGING_CACHE_NAME, 1000)
                    .memoryStoreEvictionPolicy(LRU)
                    .timeToIdleSeconds(FIVE_MINUTES_IN_SECONDS)
                    .timeToLiveSeconds(FIVE_MINUTES_IN_SECONDS);

    private static final CacheConfiguration ENHET_NAVN_CACHE =
            new CacheConfiguration(ENHET_NAVN_CACHE_NAME, 2000)
                    .memoryStoreEvictionPolicy(LRU)
                    .timeToIdleSeconds(ONE_DAY_IN_SECONDS)
                    .timeToLiveSeconds(ONE_DAY_IN_SECONDS);

    @Bean
    public CacheManager cacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(ABAC_CACHE);
        config.addCache(AKTOR_ID_FROM_FNR_CACHE);
        config.addCache(FNR_FROM_AKTOR_ID_CACHE);
        config.addCache(VEILEDER_CACHE);
        config.addCache(REGISTRERING_CACHE);
        config.addCache(OPPFOLGING_CACHE);
        config.addCache(ENHET_NAVN_CACHE);
        config.addCache(VEILEDER_ENHETER_CACHE);
		return new EhCacheCacheManager(net.sf.ehcache.CacheManager.newInstance(config));
    }

}
