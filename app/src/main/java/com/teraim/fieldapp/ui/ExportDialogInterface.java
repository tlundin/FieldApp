package com.teraim.fieldapp.ui;

public interface ExportDialogInterface {

    public void setGenerateStatus(String msg) ;
    public void setSendStatus(String msg) ;
    public void setBackupStatus(String msg) ;
    public void setCheckGenerate(boolean success) ;
    public void setCheckBackup(boolean success) ;
    public void setCheckSend(boolean success) ;
    public void setOutCome(String msg) ;
}
