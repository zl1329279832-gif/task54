package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.OperationLog;
import com.ujcms.cms.core.domain.OperationLogExt;
import com.ujcms.cms.core.domain.ScheduledPublish;
import com.ujcms.cms.core.domain.cache.SiteSpringCache;
import com.ujcms.cms.core.mapper.ScheduledPublishMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduledPublishLogTest {
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
    void testExecutePublish_createsOperationLog() {
        ScheduledPublish sp = createPendingTask();
        Article article = new Article();
        article.setId(50L);

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));
        when(articleService.select(50L)).thenReturn(article);

        SiteSpringCache mockCache = mock(SiteSpringCache.class);
        try (MockedStatic<SiteSpringCache> staticMock = mockStatic(SiteSpringCache.class)) {
            staticMock.when(SiteSpringCache::me).thenReturn(mockCache);
            service.executePendingTasks();
        }

        verify(operationLogService).asyncInsert(any(OperationLog.class), any(OperationLogExt.class));
    }

    @Test
    void testExecutePublish_logContainsCorrectModule() {
        ScheduledPublish sp = createPendingTask();
        Article article = new Article();
        article.setId(50L);

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));
        when(articleService.select(50L)).thenReturn(article);

        SiteSpringCache mockCache = mock(SiteSpringCache.class);
        try (MockedStatic<SiteSpringCache> staticMock = mockStatic(SiteSpringCache.class)) {
            staticMock.when(SiteSpringCache::me).thenReturn(mockCache);
            service.executePendingTasks();
        }

        ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).asyncInsert(logCaptor.capture(), any(OperationLogExt.class));
        assertEquals("scheduledPublish", logCaptor.getValue().getModule());
    }

    @Test
    void testExecutePublish_logContainsCorrectSiteId() {
        ScheduledPublish sp = createPendingTask();
        Article article = new Article();
        article.setId(50L);

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));
        when(articleService.select(50L)).thenReturn(article);

        SiteSpringCache mockCache = mock(SiteSpringCache.class);
        try (MockedStatic<SiteSpringCache> staticMock = mockStatic(SiteSpringCache.class)) {
            staticMock.when(SiteSpringCache::me).thenReturn(mockCache);
            service.executePendingTasks();
        }

        ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogService).asyncInsert(logCaptor.capture(), any(OperationLogExt.class));
        assertEquals(10L, logCaptor.getValue().getSiteId());
    }

    @Test
    void testExecutePublish_failedTaskLogsError() {
        ScheduledPublish sp = createPendingTask();

        when(mapper.selectPendingBefore(any())).thenReturn(List.of(sp));
        when(articleService.select(50L)).thenThrow(new RuntimeException("Connection refused"));

        service.executePendingTasks();

        // 失败时不应调用操作日志（日志在 executePublish 内部，异常被捕获前已跳过）
        verify(operationLogService, never()).asyncInsert(any(), any());
        // 但任务应被标记为失败
        ArgumentCaptor<ScheduledPublish> captor = ArgumentCaptor.forClass(ScheduledPublish.class);
        verify(mapper).update(captor.capture());
        assertEquals(ScheduledPublish.STATUS_FAILED, captor.getValue().getStatus());
        assertTrue(captor.getValue().getErrorInfo().contains("Connection refused"));
    }

    private ScheduledPublish createPendingTask() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setId(1L);
        sp.setSiteId(10L);
        sp.setUserId(100L);
        sp.setArticleId(50L);
        sp.setStatus(ScheduledPublish.STATUS_PENDING);
        sp.setPublishDate(OffsetDateTime.now().minusMinutes(5));
        return sp;
    }
}
