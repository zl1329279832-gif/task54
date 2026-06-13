package com.ujcms.cms.core.service.args;

import com.ujcms.common.query.BaseQueryArgs;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 预约发布查询参数
 *
 * @author PONY
 */
public class ScheduledPublishArgs extends BaseQueryArgs {
    public ScheduledPublishArgs siteId(@Nullable Long siteId) {
        if (siteId != null) {
            queryMap.put("EQ_siteId_Long", siteId);
        }
        return this;
    }

    public ScheduledPublishArgs status(@Nullable Short status) {
        if (status != null) {
            queryMap.put("EQ_status_Short", status);
        }
        return this;
    }

    public ScheduledPublishArgs userId(@Nullable Long userId) {
        if (userId != null) {
            queryMap.put("EQ_userId_Long", userId);
        }
        return this;
    }

    public static ScheduledPublishArgs of() {
        return of(new HashMap<>(16));
    }

    public static ScheduledPublishArgs of(Map<String, Object> queryMap) {
        return new ScheduledPublishArgs(queryMap);
    }

    private ScheduledPublishArgs(Map<String, Object> queryMap) {
        super(queryMap);
    }
}
