package com.ujcms.cms.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.ujcms.cms.core.domain.generated.GeneratedScheduledPublish;
import org.springframework.lang.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 预约发布实体类
 *
 * @author PONY
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties("handler")
public class ScheduledPublish extends GeneratedScheduledPublish {

    public ScheduledPublish() {
    }

    /**
     * 预览令牌是否有效：状态为待执行且未过期
     */
    public boolean isPreviewValid() {
        return getStatus() == STATUS_PENDING && OffsetDateTime.now().isBefore(getPreviewExpiry());
    }

    /**
     * 检查角色是否有预览权限。accessRoleIds 为空表示所有角色均可访问。
     */
    public boolean hasRoleAccess(Collection<Long> roleIds) {
        String accessRoles = getAccessRoleIds();
        if (accessRoles == null || accessRoles.isBlank()) {
            return true;
        }
        Collection<Long> allowedIds = Arrays.stream(accessRoles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        return !Collections.disjoint(allowedIds, roleIds);
    }

    /**
     * 设置错误信息
     */
    public void setError(Exception e) {
        StringWriter errorWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(errorWriter);
        e.printStackTrace(printWriter);
        setStatus(STATUS_FAILED);
        setErrorInfo(errorWriter.toString());
    }

    // region Associations

    @JsonIncludeProperties({"id", "username", "nickname"})
    private User user = new User();

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @JsonIncludeProperties({"id", "name"})
    private Site site = new Site();

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    @JsonIncludeProperties({"id", "name"})
    private Site targetSite = new Site();

    public Site getTargetSite() {
        return targetSite;
    }

    public void setTargetSite(Site targetSite) {
        this.targetSite = targetSite;
    }

    @Nullable
    @JsonIncludeProperties({"id", "name", "alias"})
    private Channel channel;

    @Nullable
    public Channel getChannel() {
        return channel;
    }

    public void setChannel(@Nullable Channel channel) {
        this.channel = channel;
    }

    @Nullable
    @JsonIncludeProperties({"id", "title"})
    private Article article;

    @Nullable
    public Article getArticle() {
        return article;
    }

    public void setArticle(@Nullable Article article) {
        this.article = article;
    }

    // endregion

    // region Constants

    /**
     * 状态：待执行
     */
    public static final short STATUS_PENDING = 0;
    /**
     * 状态：已执行
     */
    public static final short STATUS_EXECUTED = 1;
    /**
     * 状态：已取消
     */
    public static final short STATUS_CANCELLED = 2;
    /**
     * 状态：失败
     */
    public static final short STATUS_FAILED = 3;

    public static final String NOT_FOUND = "ScheduledPublish not found. ID: ";

    // endregion
}
