package com.ujcms.cms.core.web.api;

import com.ujcms.cms.core.component.ViewCountService;
import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.ArticleBuffer;
import com.ujcms.cms.core.domain.Site;
import com.ujcms.cms.core.service.ActionService;
import com.ujcms.cms.core.service.ArticleBufferService;
import com.ujcms.cms.core.service.ArticleService;
import com.ujcms.cms.core.service.ChannelService;
import com.ujcms.cms.core.service.GroupService;
import com.ujcms.cms.core.service.OrgService;
import com.ujcms.cms.core.support.Props;
import com.ujcms.cms.core.web.support.SiteResolver;
import com.ujcms.common.web.exception.Http400Exception;
import com.ujcms.common.web.exception.Http404Exception;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * API ArticleController 站点隔离测试
 */
@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    @Mock
    private SiteResolver siteResolver;
    @Mock
    private ActionService actionService;
    @Mock
    private GroupService groupService;
    @Mock
    private ChannelService channelService;
    @Mock
    private ArticleService articleService;
    @Mock
    private ArticleBufferService bufferService;
    @Mock
    private ViewCountService viewCountService;
    @Mock
    private Props props;
    @Mock
    private OrgService orgService;
    @Mock
    private HttpServletRequest request;

    private ArticleController controller;

    private static final Long SITE_A = 1L;
    private static final Long SITE_B = 2L;
    private static final Long ARTICLE_ID = 200L;

    @BeforeEach
    void setUp() {
        controller = new ArticleController(siteResolver, actionService, groupService,
                channelService, articleService, bufferService, viewCountService, props, orgService);
    }

    private Site createSite(Long id) {
        Site site = new Site();
        site.setId(id);
        return site;
    }

    private Article createArticle(Long id, Long siteId) {
        Article article = new Article();
        article.setId(id);
        article.setSiteId(siteId);
        article.setStatus(Article.STATUS_PUBLISHED);
        return article;
    }

    // --- show() tests ---

    @Test
    void show_articleInDifferentSite_throws400() {
        Article article = createArticle(ARTICLE_ID, SITE_B);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));

        assertThrows(Http400Exception.class, () -> controller.show(ARTICLE_ID, false, request));
    }

    @Test
    void show_articleNotFound_throws404() {
        when(articleService.select(ARTICLE_ID)).thenReturn(null);

        assertThrows(Http404Exception.class, () -> controller.show(ARTICLE_ID, false, request));
    }

    @Test
    void show_articleInCurrentSite_returnsArticle() {
        Article article = createArticle(ARTICLE_ID, SITE_A);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));
        // checkAccessPermission needs anonymous group for non-preview
        when(groupService.getAnonymous()).thenReturn(new com.ujcms.cms.core.domain.Group());

        Article result = controller.show(ARTICLE_ID, false, request);

        assertEquals(ARTICLE_ID, result.getId());
        assertEquals(SITE_A, result.getSiteId());
    }

    // --- view() tests ---

    @Test
    void view_articleInDifferentSite_throws400() {
        Article article = createArticle(ARTICLE_ID, SITE_B);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));

        assertThrows(Http400Exception.class, () -> controller.view(ARTICLE_ID, request));
    }

    @Test
    void view_articleNotFound_returnsZero() {
        when(articleService.select(ARTICLE_ID)).thenReturn(null);

        long result = controller.view(ARTICLE_ID, request);

        assertEquals(0, result);
    }

    @Test
    void view_articleInCurrentSite_incrementsAndReturns() {
        Article article = createArticle(ARTICLE_ID, SITE_A);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));
        when(viewCountService.viewArticle(ARTICLE_ID)).thenReturn(55L);

        long result = controller.view(ARTICLE_ID, request);

        assertEquals(55L, result);
        verify(viewCountService).viewArticle(ARTICLE_ID);
    }

    // --- buffer() tests ---

    @Test
    void buffer_articleInDifferentSite_throws400() {
        Article article = createArticle(ARTICLE_ID, SITE_B);
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));

        assertThrows(Http400Exception.class, () -> controller.buffer(ARTICLE_ID, request));
    }

    @Test
    void buffer_articleNotFound_throws404() {
        when(articleService.select(ARTICLE_ID)).thenReturn(null);

        assertThrows(Http404Exception.class, () -> controller.buffer(ARTICLE_ID, request));
    }

    @Test
    void buffer_articleInCurrentSite_returnsBuffer() {
        Article article = createArticle(ARTICLE_ID, SITE_A);
        ArticleBuffer buf = new ArticleBuffer();
        when(articleService.select(ARTICLE_ID)).thenReturn(article);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));
        when(bufferService.select(ARTICLE_ID)).thenReturn(buf);

        ArticleBuffer result = controller.buffer(ARTICLE_ID, request);

        assertSame(buf, result);
    }
}
