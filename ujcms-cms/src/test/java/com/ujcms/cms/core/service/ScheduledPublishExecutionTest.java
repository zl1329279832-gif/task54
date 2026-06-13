package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.ScheduledPublish;
import com.ujcms.cms.core.domain.cache.SiteSpringCache;
import com.ujcms.cms.core.mapper.ScheduledPublishMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduledPublishExecutionTest {
    private ScheduledPublishMapper mapper;
    private ArticleService articleService;
    private OperationLogService operationLogService;
    private ScheduledPublishService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ScheduledPublishMapper.class);
        articleService = mock(ArticleService.class);
        operationLogService = mock(OperationLogService.class);
        SnowflakeSequence snowflakeSequence = mock(SnowflakeSequence.class);
        when(snowflakeSequence.nextId()).thenReturn(1L);
        service = new ScheduledPublishService(mapper, articleService, operationLogService, snowflakeSequence);
    }

    @Test
    void testExecutePendingTasks_publishesArticle() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setId(1L);
        sp.setSiteId(10L);
        sp.setUserId(100L);
        sp.setArticleId(50L);
        sp.setStatus(ScheduledPublish.STATUS_PENDING);
        sp.setPublishDate(OffsetDateTime.now().minusMinutes(5));

        Article article = new Article();
        article.setId(50L);
        article.setStatus(Article.STATUS_DRAFT);

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));
        when(articleService.select(50L)).thenReturn(article);

        SiteSpringCache mockCache = mock(SiteSpringCache.class);
        try (MockedStatic<SiteSpringCache> staticMock = mockStatic(SiteSpringCache.class)) {
            staticMock.when(SiteSpringCache::me).thenReturn(mockCache);
            service.executePendingTasks();
        }

        assertEquals(Article.STATUS_PUBLISHED, article.getStatus());
        verify(articleService).update(any(Article.class));
    }

    @Test
    void testExecutePendingTasks_skipsNotYetDueTasks() {
        when(mapper.selectPendingBefore(any())).thenReturn(Collections.emptyList());

        SiteSpringCache mockCache = mock(SiteSpringCache.class);
        try (MockedStatic<SiteSpringCache> staticMock = mockStatic(SiteSpringCache.class)) {
            staticMock.when(SiteSpringCache::me).thenReturn(mockCache);
            service.executePendingTasks();
        }

        verify(articleService, never()).update(any(Article.class));
        verify(mapper, never()).update(any());
    }

    @Test
    void testExecutePendingTasks_setsStatusToExecuted() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setId(1L);
        sp.setSiteId(10L);
        sp.setUserId(100L);
        sp.setArticleId(50L);
        sp.setStatus(ScheduledPublish.STATUS_PENDING);

        Article article = new Article();
        article.setId(50L);

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));
        when(articleService.select(50L)).thenReturn(article);

        SiteSpringCache mockCache = mock(SiteSpringCache.class);
        try (MockedStatic<SiteSpringCache> staticMock = mockStatic(SiteSpringCache.class)) {
            staticMock.when(SiteSpringCache::me).thenReturn(mockCache);
            service.executePendingTasks();
        }

        ArgumentCaptor<ScheduledPublish> captor = ArgumentCaptor.forClass(ScheduledPublish.class);
        verify(mapper).update(captor.capture());
        assertEquals(ScheduledPublish.STATUS_EXECUTED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getExecutedDate());
    }

    @Test
    void testExecutePendingTasks_setsStatusToFailedOnError() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setId(1L);
        sp.setSiteId(10L);
        sp.setUserId(100L);
        sp.setArticleId(50L);
        sp.setStatus(ScheduledPublish.STATUS_PENDING);

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));
        when(articleService.select(50L)).thenThrow(new RuntimeException("DB error"));

        service.executePendingTasks();

        ArgumentCaptor<ScheduledPublish> captor = ArgumentCaptor.forClass(ScheduledPublish.class);
        verify(mapper).update(captor.capture());
        assertEquals(ScheduledPublish.STATUS_FAILED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getErrorInfo());
        assertTrue(captor.getValue().getErrorInfo().contains("DB error"));
    }

    @Test
    void testExecutePendingTasks_evictsSiteCache() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setId(1L);
        sp.setSiteId(10L);
        sp.setUserId(100L);
        sp.setArticleId(50L);
        sp.setStatus(ScheduledPublish.STATUS_PENDING);

        Article article = new Article();
        article.setId(50L);

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));
        when(articleService.select(50L)).thenReturn(article);

        SiteSpringCache mockCache = mock(SiteSpringCache.class);
        try (MockedStatic<SiteSpringCache> staticMock = mockStatic(SiteSpringCache.class)) {
            staticMock.when(SiteSpringCache::me).thenReturn(mockCache);
            service.executePendingTasks();
            verify(mockCache).clear();
        }
    }

    @Test
    void testExecutePublish_channelOnlyTask() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setId(1L);
        sp.setSiteId(10L);
        sp.setUserId(100L);
        sp.setChannelId(30L);
        sp.setArticleId(null);
        sp.setStatus(ScheduledPublish.STATUS_PENDING);

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));

        SiteSpringCache mockCache = mock(SiteSpringCache.class);
        try (MockedStatic<SiteSpringCache> staticMock = mockStatic(SiteSpringCache.class)) {
            staticMock.when(SiteSpringCache::me).thenReturn(mockCache);
            service.executePendingTasks();
        }

        // 无文章ID时不应调用 articleService
        verify(articleService, never()).select(any());
        verify(articleService, never()).update(any(Article.class));
        // 但任务应被标记为已执行
        ArgumentCaptor<ScheduledPublish> captor = ArgumentCaptor.forClass(ScheduledPublish.class);
        verify(mapper).update(captor.capture());
        assertEquals(ScheduledPublish.STATUS_EXECUTED, captor.getValue().getStatus());
    }
}
