package no.nav.fo.veilarbvedtaksstotte.config;


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

    public static final String VEILEDER = "veileder";

    private static final int ONE_DAY = 60 * 60 * 24 * 1000;

    private static final CacheConfiguration VEILEDER_CACHE =
            new CacheConfiguration(VEILEDER, 10000)
                    .memoryStoreEvictionPolicy(LRU)
                    .timeToIdleSeconds(ONE_DAY)
                    .timeToLiveSeconds(ONE_DAY);

    @Bean
    public CacheManager cacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(ABAC_CACHE);
        config.addCache(AKTOR_ID_FROM_FNR_CACHE);
        config.addCache(FNR_FROM_AKTOR_ID_CACHE);
        config.addCache(VEILEDER_CACHE);
		return new EhCacheCacheManager(net.sf.ehcache.CacheManager.newInstance(config));
    }

}
