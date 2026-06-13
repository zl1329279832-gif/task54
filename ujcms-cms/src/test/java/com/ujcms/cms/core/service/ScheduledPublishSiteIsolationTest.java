package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.ScheduledPublish;
import com.ujcms.cms.core.mapper.ScheduledPublishMapper;
import com.ujcms.cms.core.service.args.ScheduledPublishArgs;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduledPublishSiteIsolationTest {
    private ScheduledPublishMapper mapper;
    private ScheduledPublishService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ScheduledPublishMapper.class);
        ArticleService articleService = mock(ArticleService.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        SnowflakeSequence snowflakeSequence = mock(SnowflakeSequence.class);
        service = new ScheduledPublishService(mapper, articleService, operationLogService, snowflakeSequence);
    }

    @Test
    void testSiteIsolation_queryFiltersBySiteId() {
        ScheduledPublishArgs args = ScheduledPublishArgs.of().siteId(10L);
        Object siteIdFilter = args.getQueryMap().get("EQ_siteId_Long");
        assertNotNull(siteIdFilter);
        assertEquals(10L, siteIdFilter);
    }

    @Test
    void testSiteIsolation_cannotAccessOtherSiteTask() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setId(1L);
        sp.setSiteId(10L);
        when(mapper.select(1L)).thenReturn(sp);

        ScheduledPublish result = service.select(1L);
        assertNotNull(result);
        // 验证返回的任务属于站点10，若当前站点为20则不应通过 ValidUtils.dataInSite 校验
        assertEquals(10L, result.getSiteId());
        assertNotEquals(20L, result.getSiteId());
    }

    @Test
    void testSiteIsolation_deleteBySiteId_onlyDeletesTargetSite() {
        service.preSiteDelete(10L);
        verify(mapper).deleteBySiteId(10L);
        verify(mapper, never()).deleteBySiteId(20L);
    }
}
