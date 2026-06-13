package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.Channel;
import com.ujcms.cms.core.mapper.ChannelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 栏目 Service 站点隔离测试
 */
@ExtendWith(MockitoExtension.class)
class ChannelServiceSiteIsolationTest {

    @Mock
    private ChannelMapper mapper;

    private ChannelService channelService;

    private static final Long SITE_A = 1L;
    private static final Long SITE_B = 2L;
    private static final Long CHANNEL_ID = 100L;

    @BeforeEach
    void setUp() {
        // 手动构造，仅注入 mapper，其他依赖传 null（测试方法不使用它们）
        channelService = new ChannelService(
                null, null, null, null, mapper, null, null,
                null, null, null, null, null, null, null);
    }

    @Test
    void selectBySiteId_sameSite_returnsChannel() {
        Channel channel = new Channel();
        channel.setId(CHANNEL_ID);
        channel.setSiteId(SITE_A);
        when(mapper.select(CHANNEL_ID)).thenReturn(channel);

        Channel result = channelService.selectBySiteId(CHANNEL_ID, SITE_A);

        assertNotNull(result);
        assertEquals(CHANNEL_ID, result.getId());
        assertEquals(SITE_A, result.getSiteId());
    }

    @Test
    void selectBySiteId_differentSite_returnsNull() {
        Channel channel = new Channel();
        channel.setId(CHANNEL_ID);
        channel.setSiteId(SITE_B);
        when(mapper.select(CHANNEL_ID)).thenReturn(channel);

        Channel result = channelService.selectBySiteId(CHANNEL_ID, SITE_A);

        assertNull(result, "栏目属于站点B，站点A查询应返回null");
    }

    @Test
    void selectBySiteId_channelNotFound_returnsNull() {
        when(mapper.select(CHANNEL_ID)).thenReturn(null);

        Channel result = channelService.selectBySiteId(CHANNEL_ID, SITE_A);

        assertNull(result, "栏目不存在时应返回null");
    }

    @Test
    void findBySiteIdAndAlias_sameAliasDifferentSite_selectBySiteIdIsolates() {
        // 验证 selectBySiteId 在同别名场景下的隔离效果
        // （findBySiteIdAndAlias 依赖 PageHelper 拦截器，无法在纯单元测试中运行）
        Channel channelA = new Channel();
        channelA.setId(100L);
        channelA.setSiteId(SITE_A);
        channelA.setAlias("news");
        channelA.setName("站点A新闻");

        Channel channelB = new Channel();
        channelB.setId(200L);
        channelB.setSiteId(SITE_B);
        channelB.setAlias("news");
        channelB.setName("站点B新闻");

        // 站点A查询 ID=200（站点B的栏目）应返回 null
        when(mapper.select(200L)).thenReturn(channelB);
        assertNull(channelService.selectBySiteId(200L, SITE_A),
                "站点B的栏目不应被站点A查到");

        // 站点B查询 ID=100（站点A的栏目）应返回 null
        when(mapper.select(100L)).thenReturn(channelA);
        assertNull(channelService.selectBySiteId(100L, SITE_B),
                "站点A的栏目不应被站点B查到");

        // 各自站点查自己的栏目应成功
        when(mapper.select(100L)).thenReturn(channelA);
        Channel resultA = channelService.selectBySiteId(100L, SITE_A);
        assertNotNull(resultA);
        assertEquals("站点A新闻", resultA.getName());

        when(mapper.select(200L)).thenReturn(channelB);
        Channel resultB = channelService.selectBySiteId(200L, SITE_B);
        assertNotNull(resultB);
        assertEquals("站点B新闻", resultB.getName());
    }
}
