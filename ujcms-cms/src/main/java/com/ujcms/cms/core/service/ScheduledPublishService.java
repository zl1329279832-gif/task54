package com.ujcms.cms.core.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.OperationLog;
import com.ujcms.cms.core.domain.OperationLogExt;
import com.ujcms.cms.core.domain.ScheduledPublish;
import com.ujcms.cms.core.domain.cache.SiteSpringCache;
import com.ujcms.cms.core.domain.generated.GeneratedScheduledPublish;
import com.ujcms.cms.core.listener.SiteDeleteListener;
import com.ujcms.cms.core.listener.UserDeleteListener;
import com.ujcms.cms.core.mapper.ScheduledPublishMapper;
import com.ujcms.cms.core.service.args.ScheduledPublishArgs;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import com.ujcms.common.query.QueryInfo;
import com.ujcms.common.query.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 预约发布 Service
 *
 * @author PONY
 */
@Service
public class ScheduledPublishService implements UserDeleteListener, SiteDeleteListener {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledPublishService.class);

    private final ScheduledPublishMapper mapper;
    private final ArticleService articleService;
    private final OperationLogService operationLogService;
    private final SnowflakeSequence snowflakeSequence;

    public ScheduledPublishService(ScheduledPublishMapper mapper, ArticleService articleService,
                                   OperationLogService operationLogService, SnowflakeSequence snowflakeSequence) {
        this.mapper = mapper;
        this.articleService = articleService;
        this.operationLogService = operationLogService;
        this.snowflakeSequence = snowflakeSequence;
    }

    @Transactional(rollbackFor = Exception.class)
    public void insert(ScheduledPublish bean) {
        bean.setId(snowflakeSequence.nextId());
        bean.setPreviewToken(UUID.randomUUID().toString().replace("-", ""));
        mapper.insert(bean);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ScheduledPublish bean) {
        mapper.update(bean);
    }

    @Transactional(rollbackFor = Exception.class)
    public int delete(Long id) {
        return mapper.delete(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int delete(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).mapToInt(this::delete).sum();
    }

    @Nullable
    public ScheduledPublish select(Long id) {
        return mapper.select(id);
    }

    @Nullable
    public ScheduledPublish selectByToken(String token) {
        return mapper.selectByToken(token);
    }

    public List<ScheduledPublish> selectList(ScheduledPublishArgs args) {
        QueryInfo queryInfo = QueryParser.parse(args.getQueryMap(), GeneratedScheduledPublish.TABLE_NAME, "id_desc");
        return mapper.selectAll(queryInfo);
    }

    public List<ScheduledPublish> selectList(ScheduledPublishArgs args, int offset, int limit) {
        return PageMethod.offsetPage(offset, limit, false).doSelectPage(() -> selectList(args));
    }

    public Page<ScheduledPublish> selectPage(ScheduledPublishArgs args, int page, int pageSize) {
        return PageMethod.startPage(page, pageSize).doSelectPage(() -> selectList(args));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        ScheduledPublish bean = mapper.select(id);
        if (bean != null && bean.getStatus() == ScheduledPublish.STATUS_PENDING) {
            bean.setStatus(ScheduledPublish.STATUS_CANCELLED);
            mapper.update(bean);
        }
    }

    /**
     * 执行所有到期的预约发布任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void executePendingTasks() {
        List<ScheduledPublish> tasks = mapper.selectPendingBefore(OffsetDateTime.now());
        for (ScheduledPublish task : tasks) {
            try {
                executePublish(task);
                task.setStatus(ScheduledPublish.STATUS_EXECUTED);
                task.setExecutedDate(OffsetDateTime.now());
                mapper.update(task);
            } catch (Exception e) {
                logger.error("Failed to execute scheduled publish. ID: {}", task.getId(), e);
                task.setError(e);
                mapper.update(task);
            }
        }
    }

    private void executePublish(ScheduledPublish task) {
        Long articleId = task.getArticleId();
        if (articleId != null) {
            Article article = articleService.select(articleId);
            if (article != null) {
                article.setStatus(Article.STATUS_PUBLISHED);
                article.setPublishDate(OffsetDateTime.now());
                articleService.update(article);
            }
        }
        // 清除站点缓存
        SiteSpringCache.me().clear();
        // 记录操作日志
        OperationLog operationLog = new OperationLog();
        operationLog.setSiteId(task.getSiteId());
        operationLog.setUserId(task.getUserId());
        operationLog.setName("scheduledPublish.execute");
        operationLog.setModule("scheduledPublish");
        operationLog.setRequestMethod("SYSTEM");
        operationLog.setIp("127.0.0.1");
        operationLog.setType((short) 0);
        operationLog.setStatus(OperationLog.STATUS_SUCCESS);
        OperationLogExt operationLogExt = new OperationLogExt();
        operationLogExt.setRequestUrl("/system/scheduled-publish/" + task.getId());
        operationLogService.asyncInsert(operationLog, operationLogExt);
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
