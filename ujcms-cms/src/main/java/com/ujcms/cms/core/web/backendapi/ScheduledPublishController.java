package com.ujcms.cms.core.web.backendapi;

import com.ujcms.cms.core.aop.annotations.OperationLog;
import com.ujcms.cms.core.aop.enums.OperationType;
import com.ujcms.cms.core.domain.ScheduledPublish;
import com.ujcms.cms.core.service.ScheduledPublishService;
import com.ujcms.cms.core.service.args.ScheduledPublishArgs;
import com.ujcms.cms.core.support.Contexts;
import com.ujcms.cms.core.web.support.ValidUtils;
import com.ujcms.common.web.Entities;
import com.ujcms.common.web.Responses;
import com.ujcms.common.web.Responses.Body;
import com.ujcms.common.web.exception.Http404Exception;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

import static com.ujcms.cms.core.support.Constants.validPage;
import static com.ujcms.cms.core.support.Constants.validPageSize;
import static com.ujcms.cms.core.support.UrlConstants.BACKEND_API;
import static com.ujcms.common.db.MyBatis.springPage;
import static com.ujcms.common.query.QueryUtils.getQueryMap;

/**
 * 预约发布 Controller
 *
 * @author PONY
 */
@RestController("backendScheduledPublishController")
@RequestMapping(BACKEND_API + "/core/scheduled-publish")
public class ScheduledPublishController {
    private final ScheduledPublishService service;

    public ScheduledPublishController(ScheduledPublishService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('scheduledPublish:list','*')")
    public Page<ScheduledPublish> list(Integer page, Integer pageSize, HttpServletRequest request) {
        ScheduledPublishArgs args = ScheduledPublishArgs.of(getQueryMap(request.getQueryString()))
                .siteId(Contexts.getCurrentSiteId());
        return springPage(service.selectPage(args, validPage(page), validPageSize(pageSize)));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('scheduledPublish:show','*')")
    public ScheduledPublish show(@PathVariable Long id) {
        ScheduledPublish bean = service.select(id);
        if (bean == null) {
            throw new Http404Exception(ScheduledPublish.NOT_FOUND + id);
        }
        ValidUtils.dataInSite(bean.getSiteId(), Contexts.getCurrentSiteId());
        return bean;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('scheduledPublish:create','*')")
    @OperationLog(module = "scheduledPublish", operation = "create", type = OperationType.CREATE)
    public ResponseEntity<Body> create(@RequestBody ScheduledPublish bean) {
        ScheduledPublish scheduledPublish = new ScheduledPublish();
        Entities.copy(bean, scheduledPublish, "siteId");
        scheduledPublish.setSiteId(Contexts.getCurrentSiteId());
        scheduledPublish.setUserId(Contexts.getCurrentUser().getId());
        scheduledPublish.setCreated(OffsetDateTime.now());
        service.insert(scheduledPublish);
        return Responses.ok();
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('scheduledPublish:update','*')")
    @OperationLog(module = "scheduledPublish", operation = "update", type = OperationType.UPDATE)
    public ResponseEntity<Body> update(@RequestBody ScheduledPublish bean) {
        ScheduledPublish scheduledPublish = service.select(bean.getId());
        if (scheduledPublish == null) {
            return Responses.notFound(ScheduledPublish.NOT_FOUND + bean.getId());
        }
        ValidUtils.dataInSite(scheduledPublish.getSiteId(), Contexts.getCurrentSiteId());
        Entities.copy(bean, scheduledPublish, "siteId", "userId", "previewToken", "status", "created",
                "executedDate", "errorInfo");
        service.update(scheduledPublish);
        return Responses.ok();
    }

    @PutMapping("cancel")
    @PreAuthorize("hasAnyAuthority('scheduledPublish:update','*')")
    @OperationLog(module = "scheduledPublish", operation = "cancel", type = OperationType.UPDATE)
    public ResponseEntity<Body> cancel(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            ScheduledPublish bean = service.select(id);
            if (bean == null) {
                return Responses.notFound(ScheduledPublish.NOT_FOUND + id);
            }
            ValidUtils.dataInSite(bean.getSiteId(), Contexts.getCurrentSiteId());
            service.cancel(id);
        }
        return Responses.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('scheduledPublish:delete','*')")
    @OperationLog(module = "scheduledPublish", operation = "delete", type = OperationType.DELETE)
    public ResponseEntity<Body> delete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            ScheduledPublish bean = service.select(id);
            if (bean == null) {
                return Responses.notFound(ScheduledPublish.NOT_FOUND + id);
            }
            ValidUtils.dataInSite(bean.getSiteId(), Contexts.getCurrentSiteId());
            service.delete(id);
        }
        return Responses.ok();
    }
}
