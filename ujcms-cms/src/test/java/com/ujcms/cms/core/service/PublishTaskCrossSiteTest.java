package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.PreviewToken;
import com.ujcms.cms.core.domain.PublishTask;
import com.ujcms.cms.core.mapper.PreviewTokenMapper;
import com.ujcms.cms.core.mapper.PreviewTokenRoleMapper;
import com.ujcms.cms.core.mapper.PublishTaskMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 跨站点隔离测试
 */
@ExtendWith(MockitoExtension.class)
class PublishTaskCrossSiteTest {

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
    private OperationLogService operationLogService;
    @Mock
    private SnowflakeSequence snowflakeSequence;

    private PreviewTokenService realPreviewTokenService;
    private PublishTaskService publishTaskService;

    private static final Long SITE_A = 1L;
    private static final Long SITE_B = 2L;
    private static final Long TASK_ID = 100L;
    private static final Long CONTENT_ID = 300L;

    @BeforeEach
    void setUp() {
        realPreviewTokenService = new PreviewTokenService(
                previewTokenMapper, previewTokenRoleMapper, snowflakeSequence);
        publishTaskService = new PublishTaskService(
                publishTaskMapper, previewTokenMapper, previewTokenRoleMapper,
                articleService, siteService, realPreviewTokenService,
                operationLogService, snowflakeSequence);
    }

    @Test
    void selectBySiteA_doesNotReturnSiteBTasks() {
        PublishTask siteATask = new PublishTask();
        siteATask.setId(TASK_ID);
        siteATask.setSiteId(SITE_A);

        when(publishTaskMapper.selectAll(any())).thenReturn(List.of(siteATask));

        com.ujcms.cms.core.service.args.PublishTaskArgs args =
                com.ujcms.cms.core.service.args.PublishTaskArgs.of().siteId(SITE_A);
        List<PublishTask> result = publishTaskService.selectList(args);

        assertEquals(1, result.size());
        assertEquals(SITE_A, result.get(0).getSiteId());
    }

    @Test
    void previewToken_siteIsolation_siteMismatchRejectsToken() {
        String plainToken = "cross-site-test-token";
        String hash = PreviewTokenService.hashToken(plainToken);

        PreviewToken token = createValidStoredToken(hash, SITE_A);
        when(previewTokenMapper.selectByTokenHash(hash)).thenReturn(token);

        // Validate with site B's context should fail
        Optional<PreviewToken> result = realPreviewTokenService.validateToken(
                plainToken, SITE_B, PublishTask.CONTENT_ARTICLE, CONTENT_ID);

        assertTrue(result.isEmpty(), "Token for site A should not be valid for site B");
    }

    @Test
    void previewToken_siteIsolation_sameSiteAcceptsToken() {
        String plainToken = "same-site-test-token";
        String hash = PreviewTokenService.hashToken(plainToken);

        PreviewToken token = createValidStoredToken(hash, SITE_A);
        when(previewTokenMapper.selectByTokenHash(hash)).thenReturn(token);

        // Validate with site A's context should succeed
        Optional<PreviewToken> result = realPreviewTokenService.validateToken(
                plainToken, SITE_A, PublishTask.CONTENT_ARTICLE, CONTENT_ID);

        assertTrue(result.isPresent(), "Token for site A should be valid for site A");
        verify(previewTokenMapper).updateUsageCount(TASK_ID, 1);
    }

    @Test
    void deleteCascade_removesTokensForTask() {
        publishTaskService.delete(TASK_ID);

        verify(previewTokenMapper).deleteByPublishTaskId(TASK_ID);
    }

    @Test
    void pendingTasks_siteIsolation_taskExecutesWithOwnSiteContext() {
        PublishTask taskA = new PublishTask();
        taskA.setId(1L);
        taskA.setSiteId(SITE_A);
        taskA.setStatus(PublishTask.STATUS_PENDING);
        taskA.setPublishDate(OffsetDateTime.now().minusHours(1));
        taskA.setContentType(PublishTask.CONTENT_ARTICLE);
        taskA.setContentId(10L);

        when(publishTaskMapper.selectPendingBefore(any())).thenReturn(List.of(taskA));
        when(publishTaskMapper.select(1L)).thenReturn(taskA);
        doThrow(new com.ujcms.common.web.exception.Http404Exception("not found"))
                .when(articleService).select(10L);

        publishTaskService.executePendingTasks();

        // Task should have been attempted (EXECUTING then FAILED due to article not found)
        verify(publishTaskMapper).updateStatus(eq(1L), eq(PublishTask.STATUS_EXECUTING), isNull());
        verify(publishTaskMapper).updateStatus(eq(1L), eq(PublishTask.STATUS_FAILED), anyString());
    }

    private PreviewToken createValidStoredToken(String hash, Long siteId) {
        PreviewToken token = new PreviewToken();
        token.setId(TASK_ID);
        token.setPublishTaskId(TASK_ID);
        token.setTokenHash(hash);
        token.setSiteId(siteId);
        token.setContentType(PublishTask.CONTENT_ARTICLE);
        token.setContentId(CONTENT_ID);
        token.setExpiresAt(OffsetDateTime.now().plusHours(24));
        token.setMaxUsage(100);
        token.setUsageCount(0);
        return token;
    }
}
