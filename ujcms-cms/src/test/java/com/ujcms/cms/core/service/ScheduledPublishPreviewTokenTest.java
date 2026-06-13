package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.ScheduledPublish;
import com.ujcms.cms.core.mapper.ScheduledPublishMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduledPublishPreviewTokenTest {
    private ScheduledPublishMapper mapper;
    private ScheduledPublishService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ScheduledPublishMapper.class);
        ArticleService articleService = mock(ArticleService.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        SnowflakeSequence snowflakeSequence = mock(SnowflakeSequence.class);
        when(snowflakeSequence.nextId()).thenReturn(1L);
        service = new ScheduledPublishService(mapper, articleService, operationLogService, snowflakeSequence);
    }

    @Test
    void testInsert_generatesUniquePreviewToken() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setPublishDate(OffsetDateTime.now().plusDays(1));
        sp.setPreviewExpiry(OffsetDateTime.now().plusDays(1));
        sp.setSiteId(1L);
        sp.setTargetSiteId(1L);
        sp.setUserId(1L);

        service.insert(sp);

        ArgumentCaptor<ScheduledPublish> captor = ArgumentCaptor.forClass(ScheduledPublish.class);
        verify(mapper).insert(captor.capture());
        ScheduledPublish captured = captor.getValue();
        assertNotNull(captured.getPreviewToken());
        assertFalse(captured.getPreviewToken().isEmpty());
        assertEquals(32, captured.getPreviewToken().length());
    }

    @Test
    void testSelectByToken_returnsCorrectTask() {
        ScheduledPublish expected = new ScheduledPublish();
        expected.setId(100L);
        expected.setPreviewToken("abc123");
        when(mapper.selectByToken("abc123")).thenReturn(expected);

        ScheduledPublish result = service.selectByToken("abc123");

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("abc123", result.getPreviewToken());
    }

    @Test
    void testPreviewExpiry_beforeExpiry_isValid() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setStatus(ScheduledPublish.STATUS_PENDING);
        sp.setPreviewExpiry(OffsetDateTime.now().plusHours(2));
        assertTrue(sp.isPreviewValid());
    }

    @Test
    void testPreviewExpiry_afterExpiry_isInvalid() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setStatus(ScheduledPublish.STATUS_PENDING);
        sp.setPreviewExpiry(OffsetDateTime.now().minusMinutes(1));
        assertFalse(sp.isPreviewValid());
    }
}
