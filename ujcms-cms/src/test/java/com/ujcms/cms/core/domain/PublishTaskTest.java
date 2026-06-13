package com.ujcms.cms.core.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PublishTask 领域逻辑测试
 */
class PublishTaskTest {

    @Test
    void statusConstants() {
        assertEquals(0, PublishTask.STATUS_PENDING);
        assertEquals(1, PublishTask.STATUS_EXECUTING);
        assertEquals(2, PublishTask.STATUS_SUCCESS);
        assertEquals(3, PublishTask.STATUS_FAILED);
        assertEquals(4, PublishTask.STATUS_CANCELLED);
    }

    @Test
    void contentTypeConstants() {
        assertEquals("article", PublishTask.CONTENT_ARTICLE);
        assertEquals("channel", PublishTask.CONTENT_CHANNEL);
    }

    @Test
    void isPending_whenStatusIsPending_returnsTrue() {
        PublishTask task = new PublishTask();
        task.setStatus(PublishTask.STATUS_PENDING);
        assertTrue(task.isPending());
    }

    @Test
    void isPending_whenStatusIsNotPending_returnsFalse() {
        PublishTask task = new PublishTask();
        task.setStatus(PublishTask.STATUS_SUCCESS);
        assertFalse(task.isPending());
    }

    @Test
    void isPending_whenStatusIsNull_returnsFalse() {
        PublishTask task = new PublishTask();
        task.setStatus(null);
        assertFalse(task.isPending());
    }

    @Test
    void isExecutable_whenPendingAndPublishDateInPast_returnsTrue() {
        PublishTask task = new PublishTask();
        task.setStatus(PublishTask.STATUS_PENDING);
        task.setPublishDate(OffsetDateTime.now().minusHours(1));
        assertTrue(task.isExecutable());
    }

    @Test
    void isExecutable_whenPendingAndPublishDateInFuture_returnsFalse() {
        PublishTask task = new PublishTask();
        task.setStatus(PublishTask.STATUS_PENDING);
        task.setPublishDate(OffsetDateTime.now().plusHours(1));
        assertFalse(task.isExecutable());
    }

    @Test
    void isExecutable_whenNotPending_returnsFalse() {
        PublishTask task = new PublishTask();
        task.setStatus(PublishTask.STATUS_SUCCESS);
        task.setPublishDate(OffsetDateTime.now().minusHours(1));
        assertFalse(task.isExecutable());
    }

    @Test
    void isExecutable_whenPublishDateIsNull_returnsFalse() {
        PublishTask task = new PublishTask();
        task.setStatus(PublishTask.STATUS_PENDING);
        task.setPublishDate(null);
        assertFalse(task.isExecutable());
    }

    @Test
    void isArticleType_whenContentTypeIsArticle_returnsTrue() {
        PublishTask task = new PublishTask();
        task.setContentType(PublishTask.CONTENT_ARTICLE);
        assertTrue(task.isArticleType());
    }

    @Test
    void isArticleType_whenContentTypeIsChannel_returnsFalse() {
        PublishTask task = new PublishTask();
        task.setContentType(PublishTask.CONTENT_CHANNEL);
        assertFalse(task.isArticleType());
    }

    @Test
    void isChannelType_whenContentTypeIsChannel_returnsTrue() {
        PublishTask task = new PublishTask();
        task.setContentType(PublishTask.CONTENT_CHANNEL);
        assertTrue(task.isChannelType());
    }

    @Test
    void isChannelType_whenContentTypeIsArticle_returnsFalse() {
        PublishTask task = new PublishTask();
        task.setContentType(PublishTask.CONTENT_ARTICLE);
        assertFalse(task.isChannelType());
    }
}
