package com.ujcms.cms.core.web.api;

import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.Channel;
import com.ujcms.cms.core.domain.PreviewToken;
import com.ujcms.cms.core.domain.PublishTask;
import com.ujcms.cms.core.service.ArticleService;
import com.ujcms.cms.core.service.ChannelService;
import com.ujcms.cms.core.service.PreviewTokenService;
import com.ujcms.common.web.exception.Http403Exception;
import com.ujcms.common.web.exception.Http404Exception;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static com.ujcms.cms.core.support.UrlConstants.API;

/**
 * 预览接口（令牌验证，只读）
 *
 * @author UJCMS
 */
@Tag(name = "预览接口")
@RestController("apiPreviewController")
@RequestMapping(API + "/preview")
public class PreviewController {
    private final PreviewTokenService previewTokenService;
    private final ArticleService articleService;
    private final ChannelService channelService;

    public PreviewController(PreviewTokenService previewTokenService,
                             ArticleService articleService,
                             ChannelService channelService) {
        this.previewTokenService = previewTokenService;
        this.articleService = articleService;
        this.channelService = channelService;
    }

    @Operation(summary = "预览文章")
    @GetMapping("article/{id}")
    public Article previewArticle(@PathVariable Long id, @RequestParam String token) {
        Article article = articleService.select(id);
        if (article == null) {
            throw new Http404Exception("Article not found. ID: " + id);
        }
        validatePreviewToken(token, article.getSiteId(), PublishTask.CONTENT_ARTICLE, id);
        return article;
    }

    @Operation(summary = "预览栏目")
    @GetMapping("channel/{id}")
    public Channel previewChannel(@PathVariable Long id, @RequestParam String token) {
        Channel channel = channelService.select(id);
        if (channel == null) {
            throw new Http404Exception("Channel not found. ID: " + id);
        }
        validatePreviewToken(token, channel.getSiteId(), PublishTask.CONTENT_CHANNEL, id);
        return channel;
    }

    private void validatePreviewToken(String token, Long siteId, String contentType, Long contentId) {
        Optional<PreviewToken> result = previewTokenService.validateToken(token, siteId, contentType, contentId);
        if (result.isEmpty()) {
            throw new Http403Exception("Invalid or expired preview token");
        }
    }
}
