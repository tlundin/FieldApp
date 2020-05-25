package com.teraim.fieldapp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.teraim.fieldapp.R;

/**
 * Created by Terje on 2016-10-07.
 */

public class ExportDialog extends DialogFragment implements ExportDialogInterface {

    AlertDialog.Builder builder;
    private TextView progressTextView;
    private TextView sendTextView;
    private TextView backupTextView;
    private TextView outcome;
    private TextView backupHeader;
    private TextView sendHeader;
    private ImageView checkGenerate;
    private ImageView checkSend;
    private ImageView checkBackup;
    private Button closeButton;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = getActivity().getLayoutInflater().inflate(R.layout.export_dialog, null);
        builder.setView(view);
        progressTextView = view.findViewById(R.id.progress);
        sendTextView = view.findViewById(R.id.send);
        backupTextView = view.findViewById(R.id.backup);

        backupHeader= view.findViewById(R.id.backupHeader);

        sendHeader= view.findViewById(R.id.sendHeader);

        checkGenerate = view.findViewById(R.id.checkGenerate);
        checkSend = view.findViewById(R.id.checkSend);
        checkBackup= view.findViewById(R.id.checkBackup);


        checkSend.setVisibility(View.GONE);
        checkBackup.setVisibility(View.GONE);

        outcome = view.findViewById(R.id.outcome);

        closeButton = view.findViewById(R.id.closeButton);

        closeButton.setVisibility(View.GONE);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExportDialog.this.dismiss();
            }
        });

        return builder.create();

    }



    public void setGenerateStatus(String msg) {
        if (progressTextView !=null)
            progressTextView.setText(msg);
    }

    public void setSendStatus(String msg) {
        if (sendTextView !=null)
            sendTextView.setText(msg);
    }

    public void setBackupStatus(String msg) {
        if (backupTextView !=null)
            backupTextView.setText(msg);
    }


    public void setCheckGenerate(boolean success) {
        checkBackup.setVisibility(View.VISIBLE);
        backupHeader.setEnabled(true);
        if (success)
            checkGenerate.setImageResource(R.drawable.checkmark);
        else
            checkGenerate.setImageResource(R.drawable.warning);

    }

    public void setCheckBackup(boolean success) {

        if (success)
            checkBackup.setImageResource(R.drawable.checkmark);
        else
            checkBackup.setImageResource(R.drawable.warning);
    }

    public void setCheckSend(boolean success) {
        checkSend.setVisibility(View.VISIBLE);
        sendHeader.setVisibility(View.VISIBLE);
        if (success)
            checkSend.setImageResource(R.drawable.checkmark);
        else
            checkSend.setImageResource(R.drawable.warning);
    }

    public void setOutCome(String msg) {
        outcome.setVisibility(View.VISIBLE);
        closeButton.setVisibility(View.VISIBLE);
       if (outcome!=null)
           outcome.setText(msg);

    }
}
