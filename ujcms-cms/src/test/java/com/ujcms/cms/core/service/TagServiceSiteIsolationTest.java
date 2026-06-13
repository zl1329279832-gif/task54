package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.Tag;
import com.ujcms.cms.core.mapper.ArticleTagMapper;
import com.ujcms.cms.core.mapper.TagMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 标签 Service 站点隔离测试
 */
@ExtendWith(MockitoExtension.class)
class TagServiceSiteIsolationTest {

    @Mock
    private ArticleTagMapper articleTagMapper;
    @Mock
    private TagMapper mapper;
    @Mock
    private SnowflakeSequence snowflakeSequence;

    private TagService tagService;

    private static final Long SITE_A = 1L;
    private static final Long SITE_B = 2L;
    private static final Long TAG_ID = 100L;

    @BeforeEach
    void setUp() {
        tagService = new TagService(articleTagMapper, mapper, snowflakeSequence);
    }

    @Test
    void selectBySiteId_sameSite_returnsTag() {
        Tag tag = new Tag();
        tag.setId(TAG_ID);
        tag.setSiteId(SITE_A);
        when(mapper.select(TAG_ID)).thenReturn(tag);

        Tag result = tagService.selectBySiteId(TAG_ID, SITE_A);

        assertNotNull(result);
        assertEquals(TAG_ID, result.getId());
        assertEquals(SITE_A, result.getSiteId());
    }

    @Test
    void selectBySiteId_differentSite_returnsNull() {
        Tag tag = new Tag();
        tag.setId(TAG_ID);
        tag.setSiteId(SITE_B);
        when(mapper.select(TAG_ID)).thenReturn(tag);

        Tag result = tagService.selectBySiteId(TAG_ID, SITE_A);

        assertNull(result, "标签属于站点B，站点A查询应返回null");
    }

    @Test
    void selectBySiteId_tagNotFound_returnsNull() {
        when(mapper.select(TAG_ID)).thenReturn(null);

        Tag result = tagService.selectBySiteId(TAG_ID, SITE_A);

        assertNull(result, "标签不存在时应返回null");
    }
}
