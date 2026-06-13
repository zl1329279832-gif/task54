package com.ujcms.cms.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ujcms.cms.core.domain.generated.GeneratedPreviewToken;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 预览令牌实体类
 *
 * @author UJCMS
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties("handler")
public class PreviewToken extends GeneratedPreviewToken {

    public static final String NOT_FOUND = "PreviewToken not found. ID: ";

    /**
     * 明文令牌（仅创建时可用）
     */
    @JsonIgnore
    @Nullable
    private String plainToken;

    /**
     * 关联的预约发布任务
     */
    @Nullable
    private PublishTask publishTask;

    /**
     * 可访问角色列表
     */
    @Nullable
    private List<Role> roles;

    /**
     * 是否已过期
     */
    public boolean isExpired() {
        return getExpiresAt() != null && getExpiresAt().isBefore(OffsetDateTime.now());
    }

    /**
     * 是否已达到最大使用次数
     */
    public boolean isUsageExhausted() {
        return getMaxUsage() != null && getMaxUsage() > 0 && getUsageCount() >= getMaxUsage();
    }

    /**
     * 是否有效（未过期且未超出使用次数）
     */
    public boolean isValid() {
        return !isExpired() && !isUsageExhausted();
    }

    /**
     * 递增使用次数
     */
    public void incrementUsage() {
        if (getUsageCount() == null) {
            setUsageCount(0);
        }
        setUsageCount(getUsageCount() + 1);
    }

    @Nullable
    public String getPlainToken() {
        return plainToken;
    }

    public void setPlainToken(@Nullable String plainToken) {
        this.plainToken = plainToken;
    }

    @Nullable
    public PublishTask getPublishTask() {
        return publishTask;
    }

    public void setPublishTask(@Nullable PublishTask publishTask) {
        this.publishTask = publishTask;
    }

    @Nullable
    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(@Nullable List<Role> roles) {
        this.roles = roles;
    }
}
