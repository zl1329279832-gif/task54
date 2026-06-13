package com.ujcms.cms.core.service.args;

import com.ujcms.common.query.BaseQueryArgs;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 预约发布任务查询参数
 *
 * @author UJCMS
 */
public class PublishTaskArgs extends BaseQueryArgs {
    public PublishTaskArgs siteId(@Nullable Long siteId) {
        if (siteId != null) {
            queryMap.put("EQ_siteId_Long", siteId);
        }
        return this;
    }

    public PublishTaskArgs status(@Nullable Short status) {
        if (status != null) {
            queryMap.put("EQ_status_Short", status);
        }
        return this;
    }

    public static PublishTaskArgs of() {
        return of(new HashMap<>(16));
    }

    public static PublishTaskArgs of(Map<String, Object> queryMap) {
        return new PublishTaskArgs(queryMap);
    }

    private PublishTaskArgs(Map<String, Object> queryMap) {
        super(queryMap);
    }
}
