package com.bookhaven.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying the Observer pattern event notification engine.
 */
public class ReadingEventPublisherTest {

    private ReadingEventPublisher publisher;

    @BeforeEach
    public void setUp() {
        publisher = new ReadingEventPublisher();
        publisher.clearListeners();
    }

    @Test
    public void testObserverNotificationOnPageChange() {
        List<ReadingEvent> receivedEvents = new ArrayList<>();
        ReadingEventListener listener = receivedEvents::add;

        publisher.subscribe(listener);
        assertEquals(1, publisher.getListenerCount());

        ReadingEvent event = new ReadingEvent(ReadingEventType.PAGE_CHANGED, 1, 50, 4, 10, 0);
        publisher.publishSync(event);

        assertEquals(1, receivedEvents.size());
        ReadingEvent received = receivedEvents.get(0);
        assertEquals(ReadingEventType.PAGE_CHANGED, received.getEventType());
        assertEquals(1, received.getUserId());
        assertEquals(50, received.getBookId());
        assertEquals(4, received.getCurrentPage());
        assertEquals(10, received.getTotalPages());
        assertFalse(received.isCompleted());
    }

    @Test
    public void testBookCompletionEventDetection() {
        List<ReadingEvent> receivedEvents = new ArrayList<>();
        publisher.subscribe(receivedEvents::add);

        ReadingEvent finalPageEvent = new ReadingEvent(ReadingEventType.PAGE_CHANGED, 2, 88, 10, 10, 0);
        publisher.publishSync(finalPageEvent);

        assertEquals(1, receivedEvents.size());
        assertTrue(receivedEvents.get(0).isCompleted(), "Reaching final page should flag completion");
    }

    @Test
    public void testUnsubscribeListener() {
        List<ReadingEvent> receivedEvents = new ArrayList<>();
        ReadingEventListener listener = receivedEvents::add;

        publisher.subscribe(listener);
        assertEquals(1, publisher.getListenerCount());

        publisher.unsubscribe(listener);
        assertEquals(0, publisher.getListenerCount());

        publisher.publishSync(new ReadingEvent(ReadingEventType.PAGE_CHANGED, 1, 1, 2, 5, 0));
        assertTrue(receivedEvents.isEmpty(), "Unsubscribed listener should not receive events");
    }
}
