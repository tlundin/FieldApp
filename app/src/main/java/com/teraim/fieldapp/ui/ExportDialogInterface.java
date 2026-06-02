package com.teraim.fieldapp.ui;

public interface ExportDialogInterface {

    void setGenerateStatus(String msg) ;
    void setSendStatus(String msg) ;
    void setBackupStatus(String msg) ;
    void setCheckGenerate(boolean success) ;
    void setCheckBackup(boolean success) ;
    void setCheckSend(int status) ;
    void setOutCome(String msg) ;
}
