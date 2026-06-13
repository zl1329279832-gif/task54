package com.ujcms.cms.core.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledPublishTest {

    @Test
    void testPreviewValid_pendingAndNotExpired_returnsTrue() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setStatus(ScheduledPublish.STATUS_PENDING);
        sp.setPreviewExpiry(OffsetDateTime.now().plusHours(1));
        assertTrue(sp.isPreviewValid());
    }

    @Test
    void testPreviewValid_expiredToken_returnsFalse() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setStatus(ScheduledPublish.STATUS_PENDING);
        sp.setPreviewExpiry(OffsetDateTime.now().minusHours(1));
        assertFalse(sp.isPreviewValid());
    }

    @Test
    void testPreviewValid_executedStatus_returnsFalse() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setStatus(ScheduledPublish.STATUS_EXECUTED);
        sp.setPreviewExpiry(OffsetDateTime.now().plusHours(1));
        assertFalse(sp.isPreviewValid());
    }

    @Test
    void testPreviewValid_cancelledStatus_returnsFalse() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setStatus(ScheduledPublish.STATUS_CANCELLED);
        sp.setPreviewExpiry(OffsetDateTime.now().plusHours(1));
        assertFalse(sp.isPreviewValid());
    }

    @Test
    void testHasRoleAccess_emptyRoles_allowsAll() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setAccessRoleIds(null);
        assertTrue(sp.hasRoleAccess(Collections.emptyList()));

        sp.setAccessRoleIds("");
        assertTrue(sp.hasRoleAccess(Collections.emptyList()));

        sp.setAccessRoleIds("  ");
        assertTrue(sp.hasRoleAccess(Collections.emptyList()));
    }

    @Test
    void testHasRoleAccess_matchingRole_returnsTrue() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setAccessRoleIds("100,200,300");
        assertTrue(sp.hasRoleAccess(Arrays.asList(200L, 400L)));
    }

    @Test
    void testHasRoleAccess_noMatchingRole_returnsFalse() {
        ScheduledPublish sp = new ScheduledPublish();
        sp.setAccessRoleIds("100,200,300");
        assertFalse(sp.hasRoleAccess(Arrays.asList(400L, 500L)));
    }
}
