package com.bookhaven.event;

/**
 * Observer contract for components interested in reading engine state updates.
 */
@FunctionalInterface
public interface ReadingEventListener {
    void onReadingEvent(ReadingEvent event);
}
