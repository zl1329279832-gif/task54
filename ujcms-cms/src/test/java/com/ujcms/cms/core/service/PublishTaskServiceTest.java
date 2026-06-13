package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.PreviewToken;
import com.ujcms.cms.core.domain.PublishTask;
import com.ujcms.cms.core.domain.Site;
import com.ujcms.cms.core.mapper.PreviewTokenMapper;
import com.ujcms.cms.core.mapper.PreviewTokenRoleMapper;
import com.ujcms.cms.core.mapper.PublishTaskMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import com.ujcms.common.web.exception.Http400Exception;
import com.ujcms.common.web.exception.Http404Exception;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PublishTaskService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PublishTaskServiceTest {

    @Mock
    private PublishTaskMapper publishTaskMapper;
    @Mock
    private PreviewTokenMapper previewTokenMapper;
    @Mock
    private PreviewTokenRoleMapper previewTokenRoleMapper;
    @Mock
    private ArticleService articleService;
    @Mock
    private SiteService siteService;
    @Mock
    private PreviewTokenService previewTokenService;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private SnowflakeSequence snowflakeSequence;

    @InjectMocks
    private PublishTaskService publishTaskService;

    private static final Long SITE_ID = 100L;
    private static final Long USER_ID = 200L;
    private static final Long ARTICLE_ID = 300L;
    private static final Long TASK_ID = 400L;
    private static final Long TARGET_SITE_ID = 100L;

    @Test
    void insert_withValidArticle_generatesIdAndPersists() {
        PublishTask task = createValidPublishTask();
        Article article = new Article();
        article.setId(ARTICLE_ID);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteService.select(TARGET_SITE_ID)).thenReturn(new Site());
        when(snowflakeSequence.nextId()).thenReturn(TASK_ID);

        publishTaskService.insert(task, null);

        assertEquals(TASK_ID, task.getId());
        assertEquals(PublishTask.STATUS_PENDING, task.getStatus());
        verify(publishTaskMapper).insert(task);
    }

    @Test
    void insert_withPreviewEnabled_createsPreviewToken() {
        PublishTask task = createValidPublishTask();
        task.setPreviewEnabled(true);
        task.setPreviewExpireHours(48);
        Article article = new Article();
        article.setId(ARTICLE_ID);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteService.select(TARGET_SITE_ID)).thenReturn(new Site());
        when(snowflakeSequence.nextId()).thenReturn(TASK_ID);
        PreviewToken token = new PreviewToken();
        token.setId(500L);
        when(previewTokenService.createToken(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(token);

        publishTaskService.insert(task, null);

        verify(previewTokenService).createToken(eq(TASK_ID), eq(SITE_ID),
                eq(PublishTask.CONTENT_ARTICLE), eq(ARTICLE_ID), any(), eq(100));
    }

    @Test
    void insert_withRoleIds_createsTokenRoleAssociations() {
        PublishTask task = createValidPublishTask();
        task.setPreviewEnabled(true);
        Article article = new Article();
        article.setId(ARTICLE_ID);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteService.select(TARGET_SITE_ID)).thenReturn(new Site());
        when(snowflakeSequence.nextId()).thenReturn(TASK_ID);
        PreviewToken token = new PreviewToken();
        token.setId(500L);
        when(previewTokenService.createToken(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(token);

        List<Long> roleIds = List.of(1L, 2L, 3L);
        publishTaskService.insert(task, roleIds);

        verify(previewTokenRoleMapper).insert(500L, 1L);
        verify(previewTokenRoleMapper).insert(500L, 2L);
        verify(previewTokenRoleMapper).insert(500L, 3L);
    }

    @Test
    void insert_whenArticleNotFound_throws404() {
        PublishTask task = createValidPublishTask();
        when(articleService.select(ARTICLE_ID)).thenReturn(null);

        assertThrows(Http404Exception.class, () -> publishTaskService.insert(task, null));
    }

    @Test
    void insert_whenTargetSiteNotFound_throws404() {
        PublishTask task = createValidPublishTask();
        Article article = new Article();
        article.setId(ARTICLE_ID);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteService.select(TARGET_SITE_ID)).thenReturn(null);

        assertThrows(Http404Exception.class, () -> publishTaskService.insert(task, null));
    }

    @Test
    void insert_whenPublishDateInPast_throws400() {
        PublishTask task = createValidPublishTask();
        task.setPublishDate(OffsetDateTime.now().minusHours(1));
        Article article = new Article();
        article.setId(ARTICLE_ID);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteService.select(TARGET_SITE_ID)).thenReturn(new Site());

        assertThrows(Http400Exception.class, () -> publishTaskService.insert(task, null));
    }

    @Test
    void insert_whenPublishDateIsNull_throws400() {
        PublishTask task = createValidPublishTask();
        task.setPublishDate(null);
        Article article = new Article();
        article.setId(ARTICLE_ID);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteService.select(TARGET_SITE_ID)).thenReturn(new Site());

        assertThrows(Http400Exception.class, () -> publishTaskService.insert(task, null));
    }

    @Test
    void cancel_whenTaskIsPending_setsCancelledAndDeletesTokens() {
        PublishTask task = createValidPublishTask();
        task.setId(TASK_ID);
        task.setStatus(PublishTask.STATUS_PENDING);
        when(publishTaskMapper.select(TASK_ID)).thenReturn(task);

        publishTaskService.cancel(TASK_ID);

        verify(publishTaskMapper).updateStatus(TASK_ID, PublishTask.STATUS_CANCELLED, null);
        verify(previewTokenMapper).deleteByPublishTaskId(TASK_ID);
    }

    @Test
    void cancel_whenTaskNotPending_throws400() {
        PublishTask task = createValidPublishTask();
        task.setId(TASK_ID);
        task.setStatus(PublishTask.STATUS_SUCCESS);
        when(publishTaskMapper.select(TASK_ID)).thenReturn(task);

        assertThrows(Http400Exception.class, () -> publishTaskService.cancel(TASK_ID));
    }

    @Test
    void cancel_whenTaskNotFound_throws404() {
        when(publishTaskMapper.select(TASK_ID)).thenReturn(null);

        assertThrows(Http404Exception.class, () -> publishTaskService.cancel(TASK_ID));
    }

    @Test
    void executePendingTasks_whenNoPending_doesNothing() {
        when(publishTaskMapper.selectPendingBefore(any())).thenReturn(Collections.emptyList());

        publishTaskService.executePendingTasks();

        verify(publishTaskMapper, never()).updateStatus(any(), anyShort(), any());
    }

    @Test
    void select_returnsMapperResult() {
        PublishTask expected = createValidPublishTask();
        expected.setId(TASK_ID);
        when(publishTaskMapper.select(TASK_ID)).thenReturn(expected);

        PublishTask result = publishTaskService.select(TASK_ID);

        assertEquals(expected, result);
    }

    @Test
    void select_whenNotFound_returnsNull() {
        when(publishTaskMapper.select(TASK_ID)).thenReturn(null);

        assertNull(publishTaskService.select(TASK_ID));
    }

    private PublishTask createValidPublishTask() {
        PublishTask task = new PublishTask();
        task.setSiteId(SITE_ID);
        task.setUserId(USER_ID);
        task.setTargetSiteId(TARGET_SITE_ID);
        task.setContentType(PublishTask.CONTENT_ARTICLE);
        task.setContentId(ARTICLE_ID);
        task.setPublishDate(OffsetDateTime.now().plusHours(24));
        task.setPreviewEnabled(false);
        return task;
    }
}
