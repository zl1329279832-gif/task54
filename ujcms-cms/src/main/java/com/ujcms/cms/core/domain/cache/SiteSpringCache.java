package com.ujcms.cms.core.domain.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * @author PONY
 */
@CacheConfig(cacheNames = SiteSpringCache.CACHE_NAME)
public class SiteSpringCache implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(SiteSpringCache.class);

    /**
     * 缓存名称
     */
    public static final String CACHE_NAME = "siteSpringCache";
    /**
     * 缓存过期时间。默认10分钟。
     */
    public static final int EXPIRES = 10;
    /**
     * 最大缓存数量。默认 50000
     */
    public static final int MAXIMUM_SIZE = 50000;

    @Cacheable
    @Nullable
    public Object getObject(Object key) {
        logger.debug("SiteSpringCache getObject({})", key);
        return null;
    }

    @CachePut(key = "#key")
    public Object putObject(Object key, Object value) {
        logger.debug("SiteSpringCache putObject({}, {})", key, value);
        return value;
    }

    @CacheEvict
    @Nullable
    public Object removeObject(Object key) {
        logger.debug("SiteSpringCache removeObject({})", key);
        return null;
    }

    @CacheEvict(allEntries = true)
    public void clear() {
        logger.debug("SiteSpringCache clear()");
    }

    /**
     * 按站点清除缓存。当前实现与 {@link #clear()} 等价（MyBatis L2 CacheKey 无法按站点精确前缀匹配），
     * 但方法签名预留了站点维度，后续切换到 Redis 等支持 key 前缀的缓存时可直接实现按站点清除。
     *
     * @param siteId 站点ID
     */
    @CacheEvict(allEntries = true)
    public void clearBySite(@SuppressWarnings("unused") Long siteId) {
        logger.debug("SiteSpringCache clearBySite({})", siteId);
    }

    public static SiteSpringCache me() {
        Objects.requireNonNull(applicationContext, "applicationContext must not be null");
        return applicationContext.getBean(SiteSpringCache.class);
    }

    @Nullable
    private static ApplicationContext applicationContext;

    @SuppressWarnings("java:S2696")
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SiteSpringCache.applicationContext = applicationContext;
    }
}
