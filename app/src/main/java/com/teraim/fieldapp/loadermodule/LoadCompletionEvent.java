package com.teraim.fieldapp.loadermodule;

public record LoadCompletionEvent(LoadingStatus status, LoadStage stage) {
}
