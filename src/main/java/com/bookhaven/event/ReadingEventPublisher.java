package com.bookhaven.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Subject/Publisher in the Observer pattern.
 * Manages event listener subscriptions and dispatches reading events asynchronously.
 */
public class ReadingEventPublisher {

    private static final Logger LOGGER = Logger.getLogger(ReadingEventPublisher.class.getName());
    private static volatile ReadingEventPublisher instance;

    private final List<ReadingEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "reading-event-worker");
        thread.setDaemon(true);
        return thread;
    });

    public ReadingEventPublisher() {
    }

    /**
     * Retrieves global Singleton instance.
     */
    public static ReadingEventPublisher getInstance() {
        if (instance == null) {
            synchronized (ReadingEventPublisher.class) {
                if (instance == null) {
                    instance = new ReadingEventPublisher();
                }
            }
        }
        return instance;
    }

    /**
     * Subscribes an observer listener.
     */
    public void subscribe(ReadingEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unsubscribes an observer listener.
     */
    public void unsubscribe(ReadingEventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Publishes an event asynchronously to all registered subscribers.
     */
    public void publish(ReadingEvent event) {
        if (event == null) return;

        executorService.submit(() -> {
            for (ReadingEventListener listener : listeners) {
                try {
                    listener.onReadingEvent(event);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error in ReadingEventListener during event dispatch", e);
                }
            }
        });
    }

    /**
     * Synchronous publish (useful for unit testing and deterministic checks).
     */
    public void publishSync(ReadingEvent event) {
        if (event == null) return;
        for (ReadingEventListener listener : listeners) {
            try {
                listener.onReadingEvent(event);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in ReadingEventListener during sync event dispatch", e);
            }
        }
    }

    /**
     * Clears all registered listeners.
     */
    public void clearListeners() {
        listeners.clear();
    }

    /**
     * Returns count of active listeners.
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
