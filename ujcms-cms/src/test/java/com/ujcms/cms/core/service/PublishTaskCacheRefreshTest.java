package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.PublishTask;
import com.ujcms.cms.core.domain.cache.SiteSpringCache;
import com.ujcms.cms.core.mapper.PreviewTokenMapper;
import com.ujcms.cms.core.mapper.PreviewTokenRoleMapper;
import com.ujcms.cms.core.mapper.PublishTaskMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import com.ujcms.common.web.exception.Http404Exception;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.*;

/**
 * 发布任务缓存刷新测试
 */
@ExtendWith(MockitoExtension.class)
class PublishTaskCacheRefreshTest {

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
    @Mock
    private SiteSpringCache siteSpringCache;

    private PreviewTokenService previewTokenService;
    private PublishTaskService publishTaskService;

    private static final Long SITE_A = 1L;
    private static final Long TASK_ID = 100L;
    private static final Long ARTICLE_ID = 300L;

    @BeforeEach
    void setUp() {
        previewTokenService = new PreviewTokenService(
                previewTokenMapper, previewTokenRoleMapper, snowflakeSequence);
        publishTaskService = new PublishTaskService(
                publishTaskMapper, previewTokenMapper, previewTokenRoleMapper,
                articleService, siteService, previewTokenService,
                operationLogService, snowflakeSequence);
    }

    @Test
    void execute_clearsCacheAfterPublish() {
        PublishTask task = createSuccessTask();
        when(publishTaskMapper.select(TASK_ID)).thenReturn(task);
        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setSiteId(SITE_A);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);

        try (MockedStatic<SiteSpringCache> cacheMock = mockStatic(SiteSpringCache.class)) {
            cacheMock.when(SiteSpringCache::me).thenReturn(siteSpringCache);

            publishTaskService.execute(TASK_ID);

            // 验证缓存被清除
            verify(siteSpringCache).clearBySite(SITE_A);
        }
    }

    @Test
    void execute_clearsCacheForCorrectSite() {
        Long siteB = 2L;
        PublishTask task = createSuccessTask();
        task.setSiteId(siteB);
        when(publishTaskMapper.select(TASK_ID)).thenReturn(task);
        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setSiteId(siteB);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);

        try (MockedStatic<SiteSpringCache> cacheMock = mockStatic(SiteSpringCache.class)) {
            cacheMock.when(SiteSpringCache::me).thenReturn(siteSpringCache);

            publishTaskService.execute(TASK_ID);

            // 验证清除的是任务所属站点的缓存
            verify(siteSpringCache).clearBySite(siteB);
            verify(siteSpringCache, never()).clearBySite(SITE_A);
        }
    }

    @Test
    void execute_onFailure_doesNotClearCache() {
        PublishTask task = createSuccessTask();
        when(publishTaskMapper.select(TASK_ID)).thenReturn(task);
        // 文章不存在，导致发布失败
        when(articleService.select(ARTICLE_ID)).thenReturn(null);

        try (MockedStatic<SiteSpringCache> cacheMock = mockStatic(SiteSpringCache.class)) {
            cacheMock.when(SiteSpringCache::me).thenReturn(siteSpringCache);

            try {
                publishTaskService.execute(TASK_ID);
            } catch (Exception ignored) {
                // 预期抛出异常
            }

            // 发布失败时不应清除缓存
            verify(siteSpringCache, never()).clearBySite(anyLong());
            verify(siteSpringCache, never()).clear();
        }
    }

    private PublishTask createSuccessTask() {
        PublishTask task = new PublishTask();
        task.setId(TASK_ID);
        task.setSiteId(SITE_A);
        task.setStatus(PublishTask.STATUS_PENDING);
        task.setPublishDate(OffsetDateTime.now().minusHours(1));
        task.setContentType(PublishTask.CONTENT_ARTICLE);
        task.setContentId(ARTICLE_ID);
        return task;
    }
}
