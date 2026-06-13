package com.ujcms.cms.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.ujcms.cms.core.domain.generated.GeneratedPublishTask;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

/**
 * 预约发布任务实体类
 *
 * @author UJCMS
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties("handler")
public class PublishTask extends GeneratedPublishTask {

    /**
     * 状态：待执行
     */
    public static final short STATUS_PENDING = 0;
    /**
     * 状态：执行中
     */
    public static final short STATUS_EXECUTING = 1;
    /**
     * 状态：成功
     */
    public static final short STATUS_SUCCESS = 2;
    /**
     * 状态：失败
     */
    public static final short STATUS_FAILED = 3;
    /**
     * 状态：已取消
     */
    public static final short STATUS_CANCELLED = 4;

    /**
     * 内容类型：文章
     */
    public static final String CONTENT_ARTICLE = "article";
    /**
     * 内容类型：栏目
     */
    public static final String CONTENT_CHANNEL = "channel";

    public static final String NOT_FOUND = "PublishTask not found. ID: ";

    public PublishTask() {
    }

    /**
     * 是否待执行
     */
    public boolean isPending() {
        return getStatus() != null && getStatus() == STATUS_PENDING;
    }

    /**
     * 是否可执行（待执行且发布时间已过）
     */
    public boolean isExecutable() {
        return isPending()
                && getPublishDate() != null
                && !getPublishDate().isAfter(OffsetDateTime.now());
    }

    /**
     * 是否为文章类型
     */
    public boolean isArticleType() {
        return CONTENT_ARTICLE.equals(getContentType());
    }

    /**
     * 是否为栏目类型
     */
    public boolean isChannelType() {
        return CONTENT_CHANNEL.equals(getContentType());
    }

    /**
     * 源站点
     */
    @JsonIncludeProperties({"id", "name"})
    @Nullable
    private Site site;

    /**
     * 目标站点
     */
    @JsonIncludeProperties({"id", "name"})
    @Nullable
    private Site targetSite;

    /**
     * 操作用户
     */
    @JsonIncludeProperties({"id", "username", "nickname"})
    @Nullable
    private User user;

    /**
     * 预览令牌
     */
    @Nullable
    private PreviewToken previewToken;

    @Nullable
    public Site getSite() {
        return site;
    }

    public void setSite(@Nullable Site site) {
        this.site = site;
    }

    @Nullable
    public Site getTargetSite() {
        return targetSite;
    }

    public void setTargetSite(@Nullable Site targetSite) {
        this.targetSite = targetSite;
    }

    @Nullable
    public User getUser() {
        return user;
    }

    public void setUser(@Nullable User user) {
        this.user = user;
    }

    @Nullable
    public PreviewToken getPreviewToken() {
        return previewToken;
    }

    public void setPreviewToken(@Nullable PreviewToken previewToken) {
        this.previewToken = previewToken;
    }
}
