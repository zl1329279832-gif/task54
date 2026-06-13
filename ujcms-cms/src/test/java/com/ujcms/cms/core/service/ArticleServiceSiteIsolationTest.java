package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.mapper.ArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 文章 Service 站点隔离测试
 */
@ExtendWith(MockitoExtension.class)
class ArticleServiceSiteIsolationTest {

    @Mock
    private ArticleMapper mapper;

    private ArticleService articleService;

    private static final Long SITE_A = 1L;
    private static final Long SITE_B = 2L;
    private static final Long ARTICLE_ID = 100L;

    @BeforeEach
    void setUp() {
        // 手动构造，仅注入 mapper，其他依赖传 null（测试方法不使用它们）
        articleService = new ArticleService(
                null, null, null, null, null, null,
                null, mapper, null, null, null, null);
    }

    @Test
    void selectBySiteId_sameSite_returnsArticle() {
        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setSiteId(SITE_A);
        when(mapper.select(ARTICLE_ID)).thenReturn(article);

        Article result = articleService.selectBySiteId(ARTICLE_ID, SITE_A);

        assertNotNull(result);
        assertEquals(ARTICLE_ID, result.getId());
        assertEquals(SITE_A, result.getSiteId());
    }

    @Test
    void selectBySiteId_differentSite_returnsNull() {
        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setSiteId(SITE_B);
        when(mapper.select(ARTICLE_ID)).thenReturn(article);

        Article result = articleService.selectBySiteId(ARTICLE_ID, SITE_A);

        assertNull(result, "文章属于站点B，站点A查询应返回null");
    }

    @Test
    void selectBySiteId_articleNotFound_returnsNull() {
        when(mapper.select(ARTICLE_ID)).thenReturn(null);

        Article result = articleService.selectBySiteId(ARTICLE_ID, SITE_A);

        assertNull(result, "文章不存在时应返回null");
    }
}
