package com.teraim.fieldapp.loadermodule;

public class LoadCompletionEvent {
    public final LoadingStatus status;
    public final LoadStage stage;

    public LoadCompletionEvent(LoadingStatus status, LoadStage stage) {
        this.status = status;
        this.stage = stage;
    }
}
