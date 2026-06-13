package com.ujcms.cms.core.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.OperationLog;
import com.ujcms.cms.core.domain.OperationLogExt;
import com.ujcms.cms.core.domain.PreviewToken;
import com.ujcms.cms.core.domain.PublishTask;
import com.ujcms.cms.core.domain.Site;
import com.ujcms.cms.core.domain.User;
import com.ujcms.cms.core.domain.cache.SiteSpringCache;
import com.ujcms.cms.core.domain.generated.GeneratedPublishTask;
import com.ujcms.cms.core.listener.SiteDeleteListener;
import com.ujcms.cms.core.listener.UserDeleteListener;
import com.ujcms.cms.core.mapper.PreviewTokenMapper;
import com.ujcms.cms.core.mapper.PreviewTokenRoleMapper;
import com.ujcms.cms.core.mapper.PublishTaskMapper;
import com.ujcms.cms.core.service.args.PublishTaskArgs;
import com.ujcms.cms.core.support.Contexts;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import com.ujcms.common.query.QueryInfo;
import com.ujcms.common.query.QueryParser;
import com.ujcms.common.web.exception.Http400Exception;
import com.ujcms.common.web.exception.Http404Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 预约发布任务 Service
 *
 * @author UJCMS
 */
@Service
public class PublishTaskService implements UserDeleteListener, SiteDeleteListener {
    private static final Logger logger = LoggerFactory.getLogger(PublishTaskService.class);

    private final PublishTaskMapper mapper;
    private final PreviewTokenMapper previewTokenMapper;
    private final PreviewTokenRoleMapper previewTokenRoleMapper;
    private final ArticleService articleService;
    private final SiteService siteService;
    private final PreviewTokenService previewTokenService;
    private final OperationLogService operationLogService;
    private final SnowflakeSequence snowflakeSequence;

    public PublishTaskService(PublishTaskMapper mapper,
                              PreviewTokenMapper previewTokenMapper,
                              PreviewTokenRoleMapper previewTokenRoleMapper,
                              ArticleService articleService,
                              SiteService siteService,
                              PreviewTokenService previewTokenService,
                              OperationLogService operationLogService,
                              SnowflakeSequence snowflakeSequence) {
        this.mapper = mapper;
        this.previewTokenMapper = previewTokenMapper;
        this.previewTokenRoleMapper = previewTokenRoleMapper;
        this.articleService = articleService;
        this.siteService = siteService;
        this.previewTokenService = previewTokenService;
        this.operationLogService = operationLogService;
        this.snowflakeSequence = snowflakeSequence;
    }

    @Transactional(rollbackFor = Exception.class)
    public void insert(PublishTask bean, @Nullable List<Long> roleIds) {
        // 校验内容存在
        if (bean.isArticleType()) {
            Article article = articleService.select(bean.getContentId());
            if (article == null) {
                throw new Http404Exception("Article not found. ID: " + bean.getContentId());
            }
        }
        // 校验目标站点存在
        Site targetSite = siteService.select(bean.getTargetSiteId());
        if (targetSite == null) {
            throw new Http404Exception("Site not found. ID: " + bean.getTargetSiteId());
        }
        // 校验发布时间在未来
        if (bean.getPublishDate() == null || !bean.getPublishDate().isAfter(OffsetDateTime.now())) {
            throw new Http400Exception("Publish date must be in the future");
        }

        bean.setId(snowflakeSequence.nextId());
        bean.setStatus(PublishTask.STATUS_PENDING);
        bean.setCreated(OffsetDateTime.now());
        if (bean.getPreviewEnabled() == null) {
            bean.setPreviewEnabled(false);
        }
        if (bean.getPreviewExpireHours() == null) {
            bean.setPreviewExpireHours(24);
        }
        mapper.insert(bean);

        // 创建预览令牌
        if (Boolean.TRUE.equals(bean.getPreviewEnabled())) {
            int expireHours = bean.getPreviewExpireHours() != null ? bean.getPreviewExpireHours() : 24;
            OffsetDateTime expiresAt = bean.getPublishDate().plusHours(expireHours);
            PreviewToken token = previewTokenService.createToken(
                    bean.getId(), bean.getSiteId(), bean.getContentType(),
                    bean.getContentId(), expiresAt, 100);
            // 添加角色限制
            if (roleIds != null) {
                for (Long roleId : roleIds) {
                    previewTokenRoleMapper.insert(token.getId(), roleId);
                }
            }
        }

        logger.info("Created publish task: id={}, content={}:{}, publishDate={}",
                bean.getId(), bean.getContentType(), bean.getContentId(), bean.getPublishDate());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(PublishTask bean) {
        mapper.update(bean);
    }

    @Transactional(rollbackFor = Exception.class)
    public int delete(Long id) {
        previewTokenRoleMapper.deleteByTokenId(id);
        previewTokenMapper.deleteByPublishTaskId(id);
        return mapper.delete(id);
    }

    @Nullable
    public PublishTask select(Long id) {
        return mapper.select(id);
    }

    public List<PublishTask> selectList(PublishTaskArgs args) {
        QueryInfo queryInfo = QueryParser.parse(args.getQueryMap(), GeneratedPublishTask.TABLE_NAME, "id_desc");
        return mapper.selectAll(queryInfo);
    }

    public List<PublishTask> selectList(PublishTaskArgs args, int offset, int limit) {
        return PageMethod.offsetPage(offset, limit, false).doSelectPage(() -> selectList(args));
    }

    public Page<PublishTask> selectPage(PublishTaskArgs args, int page, int pageSize) {
        return PageMethod.startPage(page, pageSize).doSelectPage(() -> selectList(args));
    }

    /**
     * 取消预约发布任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        PublishTask task = mapper.select(id);
        if (task == null) {
            throw new Http404Exception(PublishTask.NOT_FOUND + id);
        }
        if (!task.isPending()) {
            throw new Http400Exception("Can only cancel pending tasks");
        }
        mapper.updateStatus(id, PublishTask.STATUS_CANCELLED, null);
        // 删除关联的预览令牌
        previewTokenMapper.deleteByPublishTaskId(id);
        logger.info("Cancelled publish task: id={}", id);
    }

    /**
     * 执行预约发布任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long id) {
        PublishTask task = mapper.select(id);
        if (task == null) {
            throw new Http404Exception(PublishTask.NOT_FOUND + id);
        }
        if (!task.isExecutable()) {
            throw new Http400Exception("Task is not ready for execution");
        }

        // 标记为执行中
        mapper.updateStatus(id, PublishTask.STATUS_EXECUTING, null);

        try {
            if (task.isArticleType()) {
                publishArticle(task);
            }

            // 刷新站点缓存（按站点维度清除）
            SiteSpringCache.me().clearBySite(task.getSiteId());

            // 标记为成功
            mapper.updateStatus(id, PublishTask.STATUS_SUCCESS, null);

            // 清理预览令牌
            previewTokenMapper.deleteByPublishTaskId(id);

            // 记录操作日志
            logPublishOperation(task, OperationLog.STATUS_SUCCESS, null);

            logger.info("Successfully executed publish task: id={}", id);
        } catch (Exception e) {
            mapper.updateStatus(id, PublishTask.STATUS_FAILED, e.getMessage());
            logPublishOperation(task, OperationLog.STATUS_FAILURE, e.getMessage());
            logger.error("Failed to execute publish task: id={}", id, e);
            throw new Http400Exception("Publish task execution failed: " + e.getMessage());
        }
    }

    private void publishArticle(PublishTask task) {
        Article article = articleService.select(task.getContentId());
        if (article == null) {
            throw new Http404Exception("Article not found. ID: " + task.getContentId());
        }
        article.setStatus(Article.STATUS_PUBLISHED);
        article.adjustStatus();
        articleService.update(article);
    }

    /**
     * 执行所有到期的待发布任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void executePendingTasks() {
        List<PublishTask> tasks = mapper.selectPendingBefore(OffsetDateTime.now());
        if (tasks.isEmpty()) {
            return;
        }
        logger.info("Found {} pending publish tasks to execute", tasks.size());
        for (PublishTask task : tasks) {
            try {
                execute(task.getId());
            } catch (Exception e) {
                logger.error("Failed to execute publish task: id={}", task.getId(), e);
            }
        }
    }

    private void logPublishOperation(PublishTask task, short status, @Nullable String errorMessage) {
        OperationLog log = new OperationLog();
        log.setSiteId(task.getSiteId());
        log.setUserId(task.getUserId());
        log.setModule("publish_task");
        log.setName("publish_task.execute");
        log.setType(com.ujcms.cms.core.aop.enums.OperationType.OTHER.getType());
        log.setRequestMethod("SYSTEM");
        log.setIp("127.0.0.1");
        log.setAudit(false);
        log.setStatus(status);
        log.setRequestUrl("scheduled://publish-task/" + task.getId());

        OperationLogExt ext = log.getExt();
        if (errorMessage != null) {
            ext.setResponseEntity(errorMessage);
        } else {
            ext.setResponseEntity("Published " + task.getContentType() + " " + task.getContentId());
        }
        operationLogService.asyncInsert(log, ext);
    }

    @Override
    public void preUserDelete(Long userId) {
        mapper.deleteByUserId(userId);
    }

    @Override
    public void preSiteDelete(Long siteId) {
        mapper.deleteBySiteId(siteId);
    }

    @Override
    public int deleteListenerOrder() {
        return 100;
    }
}
