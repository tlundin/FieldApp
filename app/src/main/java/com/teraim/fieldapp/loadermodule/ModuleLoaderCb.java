package com.teraim.fieldapp.loadermodule;

// Simplified callback interface as requested
public interface ModuleLoaderCb {
    /**
     * Called when any load, freeze, or thaw operation completes successfully.
     * The specific outcome is determined by the ErrorCode in the LoadResult.
     */
    void onFileLoaded(LoadResult result);

    /**
     * Called when any operation fails.
     */
    void onError(LoadResult result);
}
