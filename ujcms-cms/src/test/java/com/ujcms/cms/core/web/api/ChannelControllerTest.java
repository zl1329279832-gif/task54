package com.ujcms.cms.core.web.api;

import com.ujcms.cms.core.component.ViewCountService;
import com.ujcms.cms.core.domain.Channel;
import com.ujcms.cms.core.domain.ChannelBuffer;
import com.ujcms.cms.core.domain.Site;
import com.ujcms.cms.core.service.ChannelBufferService;
import com.ujcms.cms.core.service.ChannelService;
import com.ujcms.cms.core.web.support.SiteResolver;
import com.ujcms.common.web.exception.Http400Exception;
import com.ujcms.common.web.exception.Http404Exception;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * API ChannelController 站点隔离测试
 */
@ExtendWith(MockitoExtension.class)
class ChannelControllerTest {

    @Mock
    private SiteResolver siteResolver;
    @Mock
    private ChannelService channelService;
    @Mock
    private ChannelBufferService bufferService;
    @Mock
    private ViewCountService viewCountService;
    @Mock
    private HttpServletRequest request;

    private ChannelController controller;

    private static final Long SITE_A = 1L;
    private static final Long SITE_B = 2L;
    private static final Long CHANNEL_ID = 100L;
    private static final String ALIAS = "news";

    @BeforeEach
    void setUp() {
        controller = new ChannelController(siteResolver, channelService, bufferService, viewCountService);
    }

    private Site createSite(Long id) {
        Site site = new Site();
        site.setId(id);
        return site;
    }

    private Channel createChannel(Long id, Long siteId) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setSiteId(siteId);
        return channel;
    }

    // --- show() tests ---

    @Test
    void show_channelInCurrentSite_returnsChannel() {
        Channel channel = createChannel(CHANNEL_ID, SITE_A);
        when(channelService.select(CHANNEL_ID)).thenReturn(channel);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));

        Channel result = controller.show(CHANNEL_ID, request);

        assertEquals(CHANNEL_ID, result.getId());
        assertEquals(SITE_A, result.getSiteId());
    }

    @Test
    void show_channelNotFound_throws404() {
        when(channelService.select(CHANNEL_ID)).thenReturn(null);

        assertThrows(Http404Exception.class, () -> controller.show(CHANNEL_ID, request));
    }

    @Test
    void show_channelInDifferentSite_throws400() {
        Channel channel = createChannel(CHANNEL_ID, SITE_B);
        when(channelService.select(CHANNEL_ID)).thenReturn(channel);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));

        assertThrows(Http400Exception.class, () -> controller.show(CHANNEL_ID, request));
    }

    // --- alias() tests ---

    @Test
    void alias_alwaysUsesResolvedSite_ignoresCallerSiteId() {
        Channel channelA = createChannel(CHANNEL_ID, SITE_A);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));
        when(channelService.findBySiteIdAndAlias(SITE_A, ALIAS)).thenReturn(channelA);

        // Caller passes SITE_B, but resolved site is SITE_A
        Channel result = controller.alias(ALIAS, SITE_B, request);

        assertEquals(SITE_A, result.getSiteId());
        // Verify the query was made with SITE_A, not SITE_B
        verify(channelService).findBySiteIdAndAlias(SITE_A, ALIAS);
        verify(channelService, never()).findBySiteIdAndAlias(eq(SITE_B), anyString());
    }

    @Test
    void alias_nullSiteIdParam_usesResolvedSite() {
        Channel channelA = createChannel(CHANNEL_ID, SITE_A);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));
        when(channelService.findBySiteIdAndAlias(SITE_A, ALIAS)).thenReturn(channelA);

        Channel result = controller.alias(ALIAS, null, request);

        assertEquals(SITE_A, result.getSiteId());
        verify(channelService).findBySiteIdAndAlias(SITE_A, ALIAS);
    }

    // --- view() tests ---

    @Test
    void view_channelInDifferentSite_throws400() {
        Channel channel = createChannel(CHANNEL_ID, SITE_B);
        when(channelService.select(CHANNEL_ID)).thenReturn(channel);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));

        assertThrows(Http400Exception.class, () -> controller.view(CHANNEL_ID, request));
    }

    @Test
    void view_channelNotFound_returnsZero() {
        when(channelService.select(CHANNEL_ID)).thenReturn(null);

        long result = controller.view(CHANNEL_ID, request);

        assertEquals(0, result);
    }

    @Test
    void view_channelInCurrentSite_incrementsAndReturns() {
        Channel channel = createChannel(CHANNEL_ID, SITE_A);
        when(channelService.select(CHANNEL_ID)).thenReturn(channel);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));
        when(viewCountService.viewChannel(CHANNEL_ID)).thenReturn(42L);

        long result = controller.view(CHANNEL_ID, request);

        assertEquals(42L, result);
        verify(viewCountService).viewChannel(CHANNEL_ID);
    }

    // --- buffer() tests ---

    @Test
    void buffer_channelInDifferentSite_throws400() {
        Channel channel = createChannel(CHANNEL_ID, SITE_B);
        when(channelService.select(CHANNEL_ID)).thenReturn(channel);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));

        assertThrows(Http400Exception.class, () -> controller.buffer(CHANNEL_ID.intValue(), request));
    }

    @Test
    void buffer_channelNotFound_throws404() {
        when(channelService.select(CHANNEL_ID)).thenReturn(null);

        assertThrows(Http404Exception.class, () -> controller.buffer(CHANNEL_ID.intValue(), request));
    }

    @Test
    void buffer_channelInCurrentSite_returnsBuffer() {
        Channel channel = createChannel(CHANNEL_ID, SITE_A);
        ChannelBuffer buf = new ChannelBuffer();
        when(channelService.select(CHANNEL_ID)).thenReturn(channel);
        when(siteResolver.resolve(request)).thenReturn(createSite(SITE_A));
        when(bufferService.select(CHANNEL_ID.intValue())).thenReturn(buf);

        ChannelBuffer result = controller.buffer(CHANNEL_ID.intValue(), request);

        assertSame(buf, result);
    }
}
