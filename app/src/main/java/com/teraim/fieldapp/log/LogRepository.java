package com.teraim.fieldapp.log;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class LogRepository {

    // Enum to define the different levels of logging sensitivity.
    public enum LogLevel {
        NORMAL,
        CRITICAL
    }

    // 1. Singleton instance and log buffer constants
    private static final LogRepository INSTANCE = new LogRepository();
    private static final int MAX_LOG_LENGTH = 30000; // Prevents the log from growing forever

    // 2. The log data, which is the shared state we need to protect
    // Holds ALL log entries.
    private final SpannableStringBuilder allLogContent = new SpannableStringBuilder();
    // Holds ONLY critical log entries.
    private final SpannableStringBuilder criticalLogContent = new SpannableStringBuilder();

    private final MutableLiveData<CharSequence> logContentLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasNewCriticalEvent = new MutableLiveData<>(false);

    // Holds the current log level state, defaulting to NORMAL.
    private LogLevel currentLogLevel = LogLevel.NORMAL;

    // Private constructor to enforce singleton pattern
    private LogRepository() {}

    // Public getter for the singleton instance
    public static LogRepository getInstance() {
        return INSTANCE;
    }

    // --- LiveData Getters ---

    public LiveData<Boolean> getHasNewCriticalEvent() {
        return hasNewCriticalEvent;
    }

    public LiveData<CharSequence> getLogContent() {
        return logContentLiveData;
    }


    // --- SYNCHRONIZED PUBLIC METHODS ---

    /**
     * Sets the current log sensitivity level.
     * When the level changes, it updates the LiveData to reflect the correct log content.
     * @param newLevel The new LogLevel to use.
     */
    public synchronized void setLogLevel(LogLevel newLevel) {
        if (this.currentLogLevel != newLevel) {
            this.currentLogLevel = newLevel;
            updateLiveData();
        }
    }

    /**
     * Appends text in the default color to the main log.
     * @param text The text to append.
     */
    public synchronized void addText(String text) {
        if (text != null && !text.isEmpty()) {
            trimLog(allLogContent); // Trim before adding new content
            String stringToAppend = allLogContent.length() > 0 ? "\n" + text : text;
            allLogContent.append(stringToAppend);
            updateLiveData();
        }
    }

    /**
     * Appends critical text in RED. This text is added to BOTH the main log and the critical-only log.
     * @param text The text to append.
     */
    public synchronized void addCriticalText(String text) {
        Log.d("CRIT", text);
        hasNewCriticalEvent.postValue(true);
        // Append to both logs
        appendTextWithColor(text, Color.RED, true);
    }

    /**
     * Appends text in GREEN to the main log.
     * @param text The text to append.
     */
    public synchronized void addGreenText(String text) {
        appendTextWithColor(text, Color.GREEN, false);
    }

    /**
     * Appends text in YELLOW to the main log.
     * @param text The text to append.
     */
    public synchronized void addYellowText(String text) {
        appendTextWithColor(text, Color.YELLOW, false);
    }

    /**
     * Appends text in a specific color to the main log.
     * @param text The text to append.
     * @param color The color integer, e.g., Color.CYAN.
     */
    public synchronized void addColorText(String text, int color) {
        appendTextWithColor(text, color, false);
    }


    /**
     * Clears both the main and critical log content.
     */
    public synchronized void clear() {
        allLogContent.clear();
        criticalLogContent.clear();
        updateLiveData();
    }

    /**
     * Consumes the critical event flag.
     */
    public synchronized void consumeNewCriticalEventFlag() {
        hasNewCriticalEvent.postValue(false);
    }

    // --- Private Helper Methods ---

    /**
     * Private helper to trim a SpannableStringBuilder from the beginning if it exceeds the max length.
     * This version is safe and will not corrupt multi-byte characters like emojis.
     * @param stringBuilder The builder to trim.
     */
    private void trimLog(SpannableStringBuilder stringBuilder) {
        if (stringBuilder.length() > MAX_LOG_LENGTH) {
            int overflow = stringBuilder.length() - MAX_LOG_LENGTH;
            // Find the first newline character at or after the overflow point.
            // We convert to string to use the efficient indexOf method.
            String content = stringBuilder.toString();
            int firstNewline = content.indexOf('\n', overflow);

            if (firstNewline != -1) {
                // Delete up to and including the newline to ensure we remove a full line.
                // This prevents cutting multi-byte characters (like emoji) in half.
                stringBuilder.delete(0, firstNewline + 1);
            } else {
                // Fallback for the rare case where the entire log is one long line with no newlines.
                // In this case, we just delete the overflow from the beginning.
                stringBuilder.delete(0, overflow);
            }
        }
    }

    /**
     * Updates the LiveData with the content corresponding to the current log level.
     */
    private void updateLiveData() {
        if (currentLogLevel == LogLevel.CRITICAL) {
            logContentLiveData.postValue(criticalLogContent);
        } else {
            logContentLiveData.postValue(allLogContent);
        }
    }

    /**
     * Private helper that handles appending text with color.
     * @param text The text to append.
     * @param color The color to use for the text.
     * @param isCritical If true, the text is also added to the critical-only log.
     */
    private void appendTextWithColor(String text, int color, boolean isCritical) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // Always add to the main log
        trimLog(allLogContent);
        String stringToAppendAll = allLogContent.length() > 0 ? "\n" + text : text;
        int startAll = allLogContent.length();
        allLogContent.append(stringToAppendAll);
        allLogContent.setSpan(new ForegroundColorSpan(color), startAll, allLogContent.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // If the message is critical, also add it to the critical log
        if (isCritical) {
            trimLog(criticalLogContent);
            String stringToAppendCrit = criticalLogContent.length() > 0 ? "\n" + text : text;
            int startCrit = criticalLogContent.length();
            criticalLogContent.append(stringToAppendCrit);
            criticalLogContent.setSpan(new ForegroundColorSpan(color), startCrit, criticalLogContent.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        updateLiveData();
    }
}
