package com.ujcms.cms.core.component;

import com.ujcms.cms.core.service.ArticleService;
import com.ujcms.cms.core.service.AttachmentService;
import com.ujcms.cms.core.service.ChannelService;
import com.ujcms.cms.core.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ContentStatCache 缓存驱逐方法与 ApplicationContextAware 测试
 */
@ExtendWith(MockitoExtension.class)
class ContentStatCacheEvictionTest {

    @Mock
    private ArticleService articleService;
    @Mock
    private ChannelService channelService;
    @Mock
    private UserService userService;
    @Mock
    private AttachmentService attachmentService;
    @Mock
    private ApplicationContext applicationContext;

    private ContentStatCache cache;

    @BeforeEach
    void setUp() {
        cache = new ContentStatCache(articleService, channelService, userService, attachmentService);
    }

    @Test
    void setApplicationContext_enablesMeAccessor() {
        when(applicationContext.getBean(ContentStatCache.class)).thenReturn(cache);

        cache.setApplicationContext(applicationContext);
        ContentStatCache result = ContentStatCache.me();

        assertSame(cache, result);
    }

    @Test
    void me_withoutContext_throwsNullPointerException() {
        // Reset static state - set context to null via a fresh instance
        ContentStatCache fresh = new ContentStatCache(articleService, channelService, userService, attachmentService);
        fresh.setApplicationContext(null);

        assertThrows(NullPointerException.class, ContentStatCache::me);
    }

    @Test
    void evictArticleStat_doesNotThrow() {
        // evictArticleStat is a cache-eviction proxy method; without Spring proxy it's a no-op
        assertDoesNotThrow(() -> cache.evictArticleStat(1L));
    }

    @Test
    void clear_doesNotThrow() {
        // clear is a cache-eviction proxy method; without Spring proxy it's a no-op
        assertDoesNotThrow(() -> cache.clear());
    }

    @Test
    void articleStat_queriesWithSiteId() {
        when(articleService.countByPublishDate(eq(1L), any(), anyList())).thenReturn(10);

        var result = cache.articleStat(1L);

        assertNotNull(result);
        assertEquals(10, result.get("total"));
        verify(articleService, times(2)).countByPublishDate(eq(1L), any(), anyList());
    }

    @Test
    void userStat_isGlobal_noSiteIdRequired() {
        when(userService.countByCreated(any())).thenReturn(5);

        var result = cache.userStat();

        assertNotNull(result);
        assertEquals(5, result.get("total"));
        // User stats are global - no siteId in the query
        verify(userService, times(2)).countByCreated(any());
    }
}
