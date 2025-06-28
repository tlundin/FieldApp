package com.teraim.fieldapp.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

public class LogRepository {

    // 1. Singleton instance and log buffer constants
    private static final LogRepository INSTANCE = new LogRepository();
    private static final int MAX_LOG_LENGTH = 30000; // Prevents the log from growing forever

    // 2. The log data, which is the shared state we need to protect
    private final SpannableStringBuilder logContent = new SpannableStringBuilder();
    private final MutableLiveData<CharSequence> logContentLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasNewCriticalEvent = new MutableLiveData<>(false);

    // Private constructor to enforce singleton pattern
    private LogRepository() {}

    // Public getter for the singleton instance
    public static LogRepository getInstance() {
        return INSTANCE;
    }

    // --- LiveData Getters (These do not need to be synchronized as they only return the LiveData object) ---

    public LiveData<Boolean> getHasNewCriticalEvent() {
        return hasNewCriticalEvent;
    }

    public LiveData<CharSequence> getLogContent() {
        return logContentLiveData;
    }


    // --- Private Helper Methods ---

    /**
     * Private helper to trim the log from the beginning if it exceeds the max length.
     * This is called before any new text is appended.
     */
    private void trimLog() {
        if (logContent.length() > MAX_LOG_LENGTH) {
            int overflow = logContent.length() - MAX_LOG_LENGTH;
            logContent.delete(0, overflow);
        }
    }


    // --- SYNCHRONIZED PUBLIC METHODS ---
    // By adding "synchronized", we ensure only one thread can modify the log at a time.

    /**
     * Appends text in the default color. This method is now thread-safe.
     * @param text The text to append.
     */
    public synchronized void addText(String text) {
        if (text != null && !text.isEmpty()) {
            trimLog(); // Trim before adding new content
            String stringToAppend = logContent.length() > 0 ? "\n" + text : text;
            logContent.append(stringToAppend);
            logContentLiveData.postValue(logContent);
        }
    }

    /**
     * Appends critical text in RED. This method is now thread-safe.
     * @param text The text to append.
     */
    public synchronized void addCriticalText(String text) {
        Log.d("CRIT", text);
        hasNewCriticalEvent.postValue(true);
        appendTextWithColor(text, Color.RED);
    }

    /**
     * Appends text in GREEN. This method is now thread-safe.
     * @param text The text to append.
     */
    public synchronized void addGreenText(String text) {
        appendTextWithColor(text, Color.GREEN);
    }

    /**
     * Appends text in YELLOW. This method is now thread-safe.
     * @param text The text to append.
     */
    public synchronized void addYellowText(String text) {
        appendTextWithColor(text, Color.YELLOW);
    }

    /**
     * Appends text in a specific color. This method is now thread-safe.
     * @param text The text to append.
     * @param color The color integer, e.g., Color.CYAN.
     */
    public synchronized void addColorText(String text, int color) {
        appendTextWithColor(text, color);
    }

    /**
     * Private helper that is only called by synchronized public methods.
     * It handles appending text with any color.
     */
    private void appendTextWithColor(String text, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        trimLog(); // Trim before adding new content

        String stringToAppend = logContent.length() > 0 ? "\n" + text : text;
        int start = logContent.length();
        logContent.append(stringToAppend);
        int end = logContent.length();

        logContent.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        logContentLiveData.postValue(logContent);
    }


    /**
     * Clears the entire log content. This method is now thread-safe.
     */
    public synchronized void clear() {
        logContent.clear();
        logContentLiveData.postValue(logContent);
    }

    /**
     * Consumes the critical event flag. This method is now thread-safe.
     */
    public synchronized void consumeNewCriticalEventFlag() {
        hasNewCriticalEvent.postValue(false);
    }
}