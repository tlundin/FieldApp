package com.teraim.fieldapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendLog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Button button1,button2;

        super.onCreate(savedInstanceState);
        //requestWindowFeature (Window.FEATURE_NO_TITLE); // make a dialog without a titlebar
        //setFinishOnTouchOutside (false); // prevent users from dismissing the dialog by tapping outside
        setContentView (R.layout.send_log);

        button1 = findViewById(R.id.sendErrorLogButton);

        button1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.d("abla","button1 clicked");
                sendLogFile();
               finish();
            }

        });

        button2 = findViewById(R.id.exitButton);

        button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                finish();
            }

        });
        Log.d("abla","survived create");




    }


    private String extractLogToFile()
    {
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo (this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e2) {
        }
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER))
            model = Build.MANUFACTURER + " " + model;

        // Make file name - file must be saved to external storage or it wont be readable by
        // the email app.
        String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
        String fullName = path + "/crashlog";

        // Extract to file.
        File file = new File (fullName);
        InputStreamReader reader = null;
        FileWriter writer = null;
        String userName="dummy";
        try
        {
            // For Android 4.0 and earlier, you will get all app's log output, so filter it to
            // mostly limit it to your app's output.  In later versions, the filtering isn't needed.
            String cmd = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) ?
                    "logcat -d -v time MyApp:v dalvikvm:v System.err:v *:s" :
                    "logcat -d -v time";

            // get input stream
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new InputStreamReader(process.getInputStream());
            Intent myIntent = getIntent();
            userName = myIntent.getStringExtra("user_name");
            // write output stream
            writer = new FileWriter(file);
            writer.write ("Android version: " +  Build.VERSION.SDK_INT + "\n");
            writer.write ("Device: " + model + "\n");
            writer.write ("App version: " + (info == null ? "(null)" : info.versionCode) + "\n");
            writer.write ("Vortex Version: " + myIntent.getStringExtra("program_version")+ "\n");
            writer.write ("App name: " + myIntent.getStringExtra("app_name")+ "\n");
            writer.write ("User name: " + userName + "\n");
            writer.write ("Team name: " + myIntent.getStringExtra("team_name")+ "\n");
            writer.write( "Free memory: " + Runtime.getRuntime().freeMemory()+"\n");

            char[] buffer = new char[10000];
            do
            {
                int n = reader.read (buffer, 0, buffer.length);
                if (n == -1)
                    break;
                writer.write (buffer, 0, n);
            } while (true);

            reader.close();
            writer.close();
        }
        catch (IOException e)
        {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e1) {
                }
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e1) {
                }

            // You might want to write a failure message to the log here.
            return null;
        }

        return "crashlog_"+userName+"_"+ Tools.getCurrentTime();
    }
    private void sendLogFile() {
        String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
        String fullName = path + "/crashlog";
        String exportName = extractLogToFile(); // This now provides the filename for the server

        Log.d("vortex", "Local file path is " + fullName);
        Log.d("vortex", "Exporting with name " + exportName);

        if (exportName == null) {
            Log.e("vortex", "Export name is null, aborting.");
            return;
        }

        File logFile = new File(fullName);

        // Ensure the local file exists before attempting to upload
        if (!logFile.exists()) {
            Log.e("vortex", "Log file does not exist at path: " + fullName);
            return;
        }

        OkHttpClient client = new OkHttpClient();

        // 2. Create the request body from the local file (logFile)
        RequestBody fileBody = RequestBody.create(logFile, MediaType.parse("text/plain"));

        // 3. Build the multipart body, providing the desired exportName as the filename
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // Use exportName as the filename in the POST request's "Content-Disposition" header
                .addFormDataPart("file", exportName, fileBody)
                .build();

        // 4. Build the POST request
        Request request = new Request.Builder()
                .url("https://synkserver.net/upload")
                .post(requestBody)
                .build();

        // 5. Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showSnackbar("Upload failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    showSnackbar("Log file sent successfully!");
                } else {
                    showSnackbar("Upload failed: " + response.code());
                }
                response.close();
            }
        });

    }
    // Helper method to show the Snackbar on the UI thread
    private void showSnackbar(String message) {
        // A Snackbar needs a view from your current layout to anchor itself.
        // android.R.id.content gives you the root view of the current activity's layout.
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            runOnUiThread(() ->
                    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
            );
        }
    }
//    private void sendLogFile ()
//    {
//        String fullName = extractLogToFile();
//        Log.d("vortex","full name is "+fullName);
//        if (fullName == null)
//            return;
//        File logFile = new File(fullName);
//
//        Uri logFileURI = FileProvider.getUriForFile(
//                this,
//                "com.teraim.fieldapp.fileprovider", logFile);
//        Intent intent = new Intent (Intent.ACTION_SEND);
//        intent.setType("plain/text");
//        intent.putExtra (Intent.EXTRA_EMAIL, new String[] {"logs@teraim.com"});
//        intent.putExtra(Intent.EXTRA_STREAM, logFileURI);
//        intent.putExtra (Intent.EXTRA_SUBJECT, "MyApp log file");
//        intent.putExtra (Intent.EXTRA_TEXT, "Log file attached."); // do this so some email clients don't complain about empty body.
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        startActivity (intent);
//    }


}
