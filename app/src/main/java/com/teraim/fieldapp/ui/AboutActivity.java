package com.teraim.fieldapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;


public class AboutActivity extends Activity {


    private boolean gone = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.fragment_about);

        super.onCreate(savedInstanceState);
       if (GlobalState.getInstance()!=null) {
            final TextView logView = findViewById(R.id.LoadDetails);
            logView.setVisibility(View.GONE);
            CharSequence log = GlobalState.getInstance().getLogTxt();
            if (log!=null)
                logView.setText(log);
            else
                Log.e("bortex","LogTxt Null..!");

            String bName = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);
            float version = GlobalState.getInstance().getPreferences().getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
            TextView ver = findViewById(R.id.BundleNameAndVersion);
            ver.setText(bName+" "+((version==-1)?"":version));
           Button feedbackButton = findViewById(R.id.feedbackButton);
           final Button buttonDetails = findViewById(R.id.buttonDetails);
           ((TextView)findViewById(R.id.fieldpad)).setText("FIELD PAD");
           ((TextView)findViewById(R.id.fieldpad_version)).setText("Version "+Constants.VORTEX_VERSION);

           feedbackButton.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   endMail();
               }
           });
           buttonDetails.setText(R.string.details);
           buttonDetails.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   if (gone) {
                       gone=false;
                       logView.setVisibility(View.VISIBLE);
                       buttonDetails.setText(R.string.hide_details);
                   } else {
                       gone=true;
                       logView.setVisibility(View.GONE);
                       buttonDetails.setText(R.string.details);
                   }


               }
           });

       }
    }


    private void endMail() {
        /* Create the Intent */
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

/* Fill it with Data */
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"info@teraim.com"});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");
/* Send it off to the Activity-Chooser */
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }









}
