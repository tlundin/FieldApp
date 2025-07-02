package com.teraim.fieldapp.dynamic.types;
import androidx.annotation.Nullable;

/**
 * A wrapper for data that is exposed via a LiveData that represents an event.
 * This prevents the event from being handled multiple times on configuration changes.
 */
public class Event<T> {

    private boolean hasBeenHandled = false;
    private final T content;

    public Event(T content) {
        this.content = content;
    }

    /**
     * Returns the content and prevents its use again.
     */
    @Nullable
    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null;
        } else {
            hasBeenHandled = true;
            return content;
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    public T peekContent() {
        return content;
    }

    public boolean hasBeenHandled() {
        return hasBeenHandled;
    }
}