package org.ggpol2.fileclient;

/**
 * Created by End-User on 2015-06-02.
 */
public interface FileAsyncCallBack {

    boolean onStart(boolean bStart);

    void onStop(String filePathName);

    void onResult(FileNameStatus fileNameStatus);

    boolean onComplete(boolean bComp);
}
