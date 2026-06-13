package com.ujcms.cms.core.web.api;

import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.Channel;
import com.ujcms.cms.core.domain.ScheduledPublish;
import com.ujcms.cms.core.domain.User;
import com.ujcms.cms.core.service.ScheduledPublishService;
import com.ujcms.cms.core.support.Contexts;
import com.ujcms.common.web.exception.Http401Exception;
import com.ujcms.common.web.exception.Http403Exception;
import com.ujcms.common.web.exception.Http404Exception;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ujcms.cms.core.support.UrlConstants.API;
import static com.ujcms.cms.core.support.UrlConstants.FRONTEND_API;

/**
 * 预约发布预览 接口
 *
 * @author PONY
 */
@Tag(name = "预约发布预览接口")
@RestController
@RequestMapping({API + "/scheduled-publish", FRONTEND_API + "/scheduled-publish"})
public class ScheduledPublishController {
    private final ScheduledPublishService service;

    public ScheduledPublishController(ScheduledPublishService service) {
        this.service = service;
    }

    @Operation(summary = "预览预约发布内容")
    @GetMapping("/preview/{token}")
    public Map<String, Object> preview(
            @Parameter(description = "预览令牌") @PathVariable String token) {
        ScheduledPublish scheduledPublish = service.selectByToken(token);
        if (scheduledPublish == null) {
            throw new Http404Exception("ScheduledPublish not found. Token: " + token);
        }
        if (!scheduledPublish.isPreviewValid()) {
            throw new Http403Exception("Preview token expired or task already executed. Token: " + token);
        }
        // 检查角色权限
        String accessRoleIds = scheduledPublish.getAccessRoleIds();
        if (accessRoleIds != null && !accessRoleIds.isBlank()) {
            User user = Contexts.findCurrentUser();
            if (user == null) {
                throw new Http401Exception();
            }
            if (!scheduledPublish.hasRoleAccess(user.fetchRoleIds())) {
                throw new Http403Exception("No preview permission. Token: " + token);
            }
        }
        // 返回预览内容
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scheduledPublish", scheduledPublish);
        Article article = scheduledPublish.getArticle();
        if (article != null) {
            result.put("article", article);
        }
        Channel channel = scheduledPublish.getChannel();
        if (channel != null) {
            result.put("channel", channel);
        }
        return result;
    }
}
