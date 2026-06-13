package com.ujcms.cms.core.web.backendapi;

import com.ujcms.cms.core.aop.annotations.OperationLog;
import com.ujcms.cms.core.aop.enums.OperationType;
import com.ujcms.cms.core.domain.PreviewToken;
import com.ujcms.cms.core.domain.PublishTask;
import com.ujcms.cms.core.service.PreviewTokenService;
import com.ujcms.cms.core.service.PublishTaskService;
import com.ujcms.cms.core.service.args.PublishTaskArgs;
import com.ujcms.cms.core.support.Contexts;
import com.ujcms.cms.core.web.support.ValidUtils;
import com.ujcms.common.web.Entities;
import com.ujcms.common.web.Responses;
import com.ujcms.common.web.Responses.Body;
import com.ujcms.common.web.exception.Http404Exception;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ujcms.cms.core.support.Constants.validPage;
import static com.ujcms.cms.core.support.Constants.validPageSize;
import static com.ujcms.cms.core.support.UrlConstants.BACKEND_API;
import static com.ujcms.common.db.MyBatis.springPage;
import static com.ujcms.common.query.QueryUtils.getQueryMap;

/**
 * 预约发布任务 Controller
 *
 * @author UJCMS
 */
@RestController("backendPublishTaskController")
@RequestMapping(BACKEND_API + "/core/publish-task")
public class PublishTaskController {
    private final PublishTaskService service;
    private final PreviewTokenService previewTokenService;

    public PublishTaskController(PublishTaskService service, PreviewTokenService previewTokenService) {
        this.service = service;
        this.previewTokenService = previewTokenService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('publish_task:list','*')")
    public Page<PublishTask> list(Integer page, Integer pageSize, HttpServletRequest request) {
        PublishTaskArgs args = PublishTaskArgs.of(getQueryMap(request.getQueryString()))
                .siteId(Contexts.getCurrentSiteId());
        return springPage(service.selectPage(args, validPage(page), validPageSize(pageSize)));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('publish_task:show','*')")
    public PublishTask show(@PathVariable Long id) {
        PublishTask bean = service.select(id);
        if (bean == null) {
            throw new Http404Exception(PublishTask.NOT_FOUND + id);
        }
        ValidUtils.dataInSite(bean.getSiteId(), Contexts.getCurrentSiteId());
        // 加载预览令牌
        List<PreviewToken> tokens = previewTokenService.selectByPublishTaskId(id);
        if (!tokens.isEmpty()) {
            bean.setPreviewToken(tokens.get(0));
        }
        return bean;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('publish_task:create','*')")
    @OperationLog(module = "publish_task", operation = "create", type = OperationType.CREATE)
    public ResponseEntity<Body> create(@RequestBody PublishTask bean,
                                       @RequestParam(required = false) @Nullable List<Long> roleIds) {
        PublishTask task = new PublishTask();
        Entities.copy(bean, task, "siteId", "userId", "status", "created");
        task.setSiteId(Contexts.getCurrentSiteId());
        task.setUserId(Contexts.getCurrentUser().getId());
        service.insert(task, roleIds);
        return Responses.ok();
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('publish_task:update','*')")
    @OperationLog(module = "publish_task", operation = "update", type = OperationType.UPDATE)
    public ResponseEntity<Body> update(@RequestBody PublishTask bean) {
        PublishTask task = service.select(bean.getId());
        if (task == null) {
            return Responses.notFound(PublishTask.NOT_FOUND + bean.getId());
        }
        ValidUtils.dataInSite(task.getSiteId(), Contexts.getCurrentSiteId());
        Entities.copy(bean, task, "siteId", "userId", "status", "created");
        service.update(task);
        return Responses.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('publish_task:delete','*')")
    @OperationLog(module = "publish_task", operation = "delete", type = OperationType.DELETE)
    public ResponseEntity<Body> delete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            PublishTask task = service.select(id);
            if (task == null) {
                return Responses.notFound(PublishTask.NOT_FOUND + id);
            }
            ValidUtils.dataInSite(task.getSiteId(), Contexts.getCurrentSiteId());
            service.delete(id);
        }
        return Responses.ok();
    }

    @PostMapping("{id}/cancel")
    @PreAuthorize("hasAnyAuthority('publish_task:update','*')")
    @OperationLog(module = "publish_task", operation = "cancel", type = OperationType.UPDATE)
    public ResponseEntity<Body> cancel(@PathVariable Long id) {
        PublishTask task = service.select(id);
        if (task == null) {
            return Responses.notFound(PublishTask.NOT_FOUND + id);
        }
        ValidUtils.dataInSite(task.getSiteId(), Contexts.getCurrentSiteId());
        service.cancel(id);
        return Responses.ok();
    }

    @PostMapping("{id}/generate-token")
    @PreAuthorize("hasAnyAuthority('publish_task:update','*')")
    @OperationLog(module = "publish_task", operation = "generateToken", type = OperationType.UPDATE)
    public ResponseEntity<Body> generateToken(@PathVariable Long id) {
        PublishTask task = service.select(id);
        if (task == null) {
            return Responses.notFound(PublishTask.NOT_FOUND + id);
        }
        ValidUtils.dataInSite(task.getSiteId(), Contexts.getCurrentSiteId());

        // 删除旧令牌
        previewTokenService.selectByPublishTaskId(id).forEach(t -> previewTokenService.delete(t.getId()));

        // 生成新令牌
        int expireHours = task.getPreviewExpireHours() != null ? task.getPreviewExpireHours() : 24;
        PreviewToken token = previewTokenService.createToken(
                task.getId(), task.getSiteId(), task.getContentType(),
                task.getContentId(), task.getPublishDate().plusHours(expireHours), 100);

        Map<String, Object> result = new HashMap<>(4);
        result.put("token", token.getPlainToken());
        result.put("expiresAt", token.getExpiresAt());
        return Responses.ok(result);
    }
}
