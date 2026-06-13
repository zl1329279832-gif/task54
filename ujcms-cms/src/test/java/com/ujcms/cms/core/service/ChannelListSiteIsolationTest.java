package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.User;
import com.ujcms.cms.core.support.Contexts;
import com.ujcms.cms.core.web.directive.ArticleListDirective;
import com.ujcms.cms.core.service.args.ArticleArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 栏目列表/文章列表 isAllSite 限制测试
 */
@ExtendWith(MockitoExtension.class)
class ChannelListSiteIsolationTest {

    @Mock
    private ChannelService channelService;

    private static final Long SITE_A = 1L;

    @AfterEach
    void tearDown() {
        Contexts.clearCurrentUser();
    }

    @Test
    void assemble_isAllSite_anonymousUser_forcedToDefaultSite() {
        // 匿名用户
        Contexts.clearCurrentUser();

        ArticleArgs args = ArticleArgs.of();
        Map<String, Object> params = new HashMap<>();
        params.put("isAllSite", true);

        ArticleListDirective.assemble(args, params, SITE_A, channelService);

        // 匿名用户设 isAllSite=true 时应回退到默认站点
        Map<String, Object> queryMap = args.getQueryMap();
        assertTrue(queryMap.containsKey("EQ_siteId_Long"),
                "匿名用户设 isAllSite=true 时应强制使用默认站点ID");
        assertEquals(SITE_A, queryMap.get("EQ_siteId_Long"));
    }

    @Test
    void assemble_isAllSite_authenticatedUser_allowsAllSites() {
        // 模拟认证用户
        User user = new User();
        user.setId(1L);
        Contexts.setCurrentUser(user);

        ArticleArgs args = ArticleArgs.of();
        Map<String, Object> params = new HashMap<>();
        params.put("isAllSite", true);

        ArticleListDirective.assemble(args, params, SITE_A, channelService);

        // 认证用户设 isAllSite=true 时不应添加站点过滤
        Map<String, Object> queryMap = args.getQueryMap();
        assertFalse(queryMap.containsKey("EQ_siteId_Long"),
                "认证用户设 isAllSite=true 时不应添加站点过滤");
    }

    @Test
    void assemble_noSiteParam_defaultsToProvidedSiteId() {
        ArticleArgs args = ArticleArgs.of();
        Map<String, Object> params = new HashMap<>();

        ArticleListDirective.assemble(args, params, SITE_A, channelService);

        // 无站点参数时应使用传入的 defaultSiteId
        Map<String, Object> queryMap = args.getQueryMap();
        assertTrue(queryMap.containsKey("EQ_siteId_Long"),
                "无站点参数时应使用默认站点ID");
        assertEquals(SITE_A, queryMap.get("EQ_siteId_Long"));
    }

    @Test
    void assemble_explicitSiteId_usesProvidedSiteId() {
        Long explicitSiteId = 99L;
        ArticleArgs args = ArticleArgs.of();
        Map<String, Object> params = new HashMap<>();
        params.put("siteId", explicitSiteId);

        ArticleListDirective.assemble(args, params, SITE_A, channelService);

        // 显式指定站点时应使用指定的站点ID
        Map<String, Object> queryMap = args.getQueryMap();
        assertEquals(explicitSiteId, queryMap.get("EQ_siteId_Long"));
    }
}
