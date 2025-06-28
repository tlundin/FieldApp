package com.teraim.fieldapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Event;
import com.teraim.fieldapp.utils.Tools; // For WebLoaderCb, if you want to reuse the progress part

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Color;
import com.teraim.fieldapp.log.LogRepository;
public class GisViewModel extends ViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // LiveData for progress updates (to be observed by the Activity)
    private final MutableLiveData<ProgressState> _progressState = new MutableLiveData<>();
    public LiveData<ProgressState> getProgressState() {
        return _progressState;
    }

    // LiveData for the final result (to be observed by the Executor fragment)
    private final MutableLiveData<Event<GisResult>> _downloadResult = new MutableLiveData<>();
    public LiveData<Event<GisResult>> getDownloadResult() {
        return _downloadResult;
    }


    public void startGisDownload(List<String> picNames, String serverFileRootDir, String cacheFolder, String masterPicName, int blockIndex) {
        _downloadResult.setValue(null);
        _progressState.setValue(null);
        _progressState.postValue(new ProgressState(true, 0, "Starting download..."));


        // Execute download logic on a background thread
        executorService.execute(() -> {
            long totalBytesToDownload = calculateTotalSize(picNames, serverFileRootDir);
            long totalBytesRead = 0;

            for (int i = 0; i < picNames.size(); i++) {
                String picName = picNames.get(i);
                final int currentFileNumber = i + 1;

                updateProgress(totalBytesRead, totalBytesToDownload, "Downloading file " + currentFileNumber + "/" + picNames.size() + ": " + picName);

                long finalTotalBytesRead = totalBytesRead;
                long bytesReadForFile = downloadFile(serverFileRootDir, picName, cacheFolder,
                        bytesReadChunk -> {
                            long currentTotal = finalTotalBytesRead + bytesReadChunk;
                            updateProgress(currentTotal, totalBytesToDownload, "Downloading file " + currentFileNumber + "/" + picNames.size() + ": " + picName);
                        }
                );

                if (bytesReadForFile >= 0) { // Success
                    totalBytesRead += bytesReadForFile;
                } else {
                    // --- NEW ERROR HANDLING LOGIC ---
                    // A file failed to download. Instead of aborting, just log it and continue.
                    String errorMsg = "Failed to download image: " + serverFileRootDir + picName;
                    Log.e("GisViewModel", errorMsg);

                    // Log the error with red text to the in-app console
                    LogRepository.getInstance().addColorText(errorMsg, Color.RED);

                    // We do NOT abort the loop. We will continue trying to download the next image.
                }
            }

            // After attempting all downloads, signal completion to the Executor.
            // This will now happen even if some non-critical images failed.
            Log.d("GisViewModel", "All pictures have been processed.");
            _progressState.postValue(new ProgressState(false, 100, "Download complete"));
            Log.d("GisTrace", "ViewModel: Posting result with masterPicName: " + masterPicName);
            // Pass the blockIndex when creating the successful result
            _downloadResult.postValue(new Event<>(new GisResult(cacheFolder, masterPicName, blockIndex)));
        });
    }

    private void updateProgress(long bytesRead, long totalBytes, String message) {
        if (totalBytes > 0) {
            int percentage = (int) ((bytesRead * 100) / totalBytes);
            _progressState.postValue(new ProgressState(true, percentage, message));
        }
    }

    // A simplified version of your download logic, adapted for this ViewModel
    private long downloadFile(String serverUrl, String fileName, String cacheFolder, ProgressCallback progressCallback) {
        // This is a simplified combination of your DownloadTask and onLoadCacheImage logic
        // This should be expanded with your full error handling and file path logic.
        long totalBytesRead = 0;
        File file = new File(cacheFolder, fileName.replace("/", "|"));

        if(file.exists()) {
            Log.d("GisViewModel","File already cached: " + fileName);
            return file.length(); // Return file size if already exists
        }

        Tools.createFoldersIfMissing(file.getParentFile());

        try (BufferedInputStream in = new BufferedInputStream(new URL(serverUrl + fileName).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {

            byte[] dataBuffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 4096)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                progressCallback.onProgress(totalBytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            file.delete(); // Clean up failed download
            return -1; // Indicate failure
        }
        return totalBytesRead;
    }

    // You must implement a way to get total file size for accurate progress
    private long calculateTotalSize(List<String> picNames, String serverFileRootDir) {
        // TODO: For accurate percentage, you need to make HEAD requests to get
        // the Content-Length of each file and sum them up.
        // Returning a placeholder for now.
        return 1000000;
    }


    // --- Helper classes for state and result ---

    public static class ProgressState {
        public final boolean inProgress;
        public final int percent;
        public final String statusMessage;

        ProgressState(boolean inProgress, int percent, String statusMessage) {
            this.inProgress = inProgress;
            this.percent = percent;
            this.statusMessage = statusMessage;
        }
    }

    public static class GisResult {
        public final String cacheFolder;
        public final String masterPicName;
        public final Exception error;
        public final int blockIndex; // <-- ADD THIS LINE

        // Success constructor
        public GisResult(String cacheFolder, String masterPicName, int blockIndex) { // <-- ADD blockIndex
            this.cacheFolder = cacheFolder;
            this.masterPicName = masterPicName;
            this.blockIndex = blockIndex; // <-- ADD THIS LINE
            this.error = null;
        }

        // Failure constructor
        public GisResult(Exception error, int blockIndex) { // <-- ADD blockIndex
            this.cacheFolder = null;
            this.masterPicName = null;
            this.blockIndex = blockIndex; // <-- ADD THIS LINE
            this.error = error;
        }

        public boolean isSuccess() {
            return error == null;
        }
    }

    interface ProgressCallback {
        void onProgress(long bytesRead);
    }
}