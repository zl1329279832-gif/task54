package com.ujcms.cms.core;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.ujcms.cms.core.component.ContentStatCache;
import com.ujcms.cms.core.component.ViewCountService;
import com.ujcms.cms.core.domain.Article;
import com.ujcms.cms.core.domain.User;
import com.ujcms.cms.core.domain.cache.SiteSpringCache;
import com.ujcms.cms.core.generator.HtmlGenerator;
import com.ujcms.cms.core.service.ArticleService;
import com.ujcms.cms.core.service.ConfigService;
import com.ujcms.cms.core.service.PreviewTokenService;
import com.ujcms.cms.core.service.PublishTaskService;

/**
 * 定时任务 配置
 *
 * @author PONY
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
    private final ViewCountService viewCountService;

    public ScheduleConfig(ViewCountService viewCountService) {
        this.viewCountService = viewCountService;
    }

    @Scheduled(cron = "#{new java.util.Random().nextInt(25) + 5} * * * * ?")
    public void flushViewCountTask() {
        viewCountService.flushSiteViews();
        viewCountService.flushChannelViews();
        viewCountService.flushArticleViews();
    }

    /**
     * 更新访问统计信息。定时时间要比写入时间晚一些，以免数据未写入完成就开始统计。
     * <p>
     * 需要集群。只要在一台机器上执行即可，不需要在多台机器同时运行。
     */
    @Bean("updateViewsStatTrigger")
    public CronTriggerFactoryBean updateViewsStatTrigger(
            @Qualifier("updateViewsStatJobDetail") JobDetail updateViewsStatJobDetail) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(updateViewsStatJobDetail);
        factoryBean.setCronExpression("45 * * * * ?");
        return factoryBean;
    }

    /**
     * 更新统计任务
     */
    @Bean("updateViewsStatJobDetail")
    public JobDetailFactoryBean updateViewsStatJobDetail() {
        final JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(UpdateViewsStatJob.class);
        // 没有绑定触发器时，必须设置持久性为 true
        factoryBean.setDurability(true);
        return factoryBean;
    }

    /**
     * 更新访问统计任务类
     */
    @Component
    public static class UpdateViewsStatJob extends QuartzJobBean {
        @Nullable
        private ViewCountService viewCountService;

        @Autowired
        public void setViewCountService(ViewCountService viewCountService) {
            this.viewCountService = viewCountService;
        }

        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            Assert.notNull(viewCountService, "ViewCountService must not be null");
            viewCountService.updateViewsStat();
        }
    }

    /**
     * 更新文章状态 JobDetail
     */
    @Bean("updateArticleStatusJobDetail")
    public JobDetailFactoryBean updateArticleStatusJobDetail() {
        final JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(UpdateArticleStatusJob.class);
        // 没有绑定触发器时，必须设置持久性为 true
        factoryBean.setDurability(true);
        return factoryBean;
    }

    /**
     * 更新文章上下线状态 Trigger。包括文章置顶和文章上下线。每10分钟执行一次。
     * <p>
     * 需要集群。只要在一台机器上执行即可，不需要在多台机器同时运行。
     */
    @Bean("updateArticleStatusTrigger")
    public CronTriggerFactoryBean updateArticleStatusTrigger(
            @Qualifier("updateArticleStatusJobDetail") JobDetail updateArticleStatusJobDetail) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(updateArticleStatusJobDetail);
        factoryBean.setCronExpression("0 0/10 * * * ?");
        return factoryBean;
    }

    /**
     * 更新文章上下线状态 Job
     */
    @Component
    public static class UpdateArticleStatusJob extends QuartzJobBean {
        private final ArticleService articleService;
        private final HtmlGenerator htmlGenerator;
        private final ConfigService configService;

        public UpdateArticleStatusJob(ArticleService articleService, HtmlGenerator htmlGenerator,
                                      ConfigService configService) {
            this.articleService = articleService;
            this.htmlGenerator = htmlGenerator;
            this.configService = configService;
        }

        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            List<Article> stickyArticles = articleService.listAndUpdateStickyDate();
            List<Article> onlineArticles = articleService.listAndUpdateOnlineStatus();
            List<Article> offlineArticles = articleService.listAndUpdateOfflineStatus();
            List<Article> articles = Stream.of(stickyArticles, onlineArticles, offlineArticles)
                    .flatMap(Collection::stream).toList();
            for (Article article : articles) {
                article.adjustStatus();
                articleService.update(article);
            }
            if (!articles.isEmpty()) {
                SiteSpringCache.me().clear();
                ContentStatCache.me().clear();
            }
            Long siteId = configService.getUnique().getDefaultSiteId();
            String taskName = "task.html.articleRelated";
            htmlGenerator.updateArticleRelatedHtml(siteId, User.ANONYMOUS_ID, taskName, articles, null);
        }
    }

    /**
     * 预约发布任务 JobDetail
     */
    @Bean("publishTaskExecutionJobDetail")
    public JobDetailFactoryBean publishTaskExecutionJobDetail() {
        final JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PublishTaskExecutionJob.class);
        // 没有绑定触发器时，必须设置持久性为 true
        factoryBean.setDurability(true);
        return factoryBean;
    }

    /**
     * 预约发布任务 Trigger。每5分钟执行一次。
     * <p>
     * 需要集群。只要在一台机器上执行即可，不需要在多台机器同时运行。
     */
    @Bean("publishTaskExecutionTrigger")
    public CronTriggerFactoryBean publishTaskExecutionTrigger(
            @Qualifier("publishTaskExecutionJobDetail") JobDetail publishTaskExecutionJobDetail) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(publishTaskExecutionJobDetail);
        factoryBean.setCronExpression("0 0/5 * * * ?");
        return factoryBean;
    }

    /**
     * 预约发布任务执行 Job
     */
    @Component
    public static class PublishTaskExecutionJob extends QuartzJobBean {
        private final PublishTaskService publishTaskService;
        private final PreviewTokenService previewTokenService;

        public PublishTaskExecutionJob(PublishTaskService publishTaskService,
                                       PreviewTokenService previewTokenService) {
            this.publishTaskService = publishTaskService;
            this.previewTokenService = previewTokenService;
        }

        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            // 执行到期的预约发布任务
            publishTaskService.executePendingTasks();
            // 清理过期的预览令牌
            previewTokenService.cleanupExpiredTokens();
        }
    }
}
