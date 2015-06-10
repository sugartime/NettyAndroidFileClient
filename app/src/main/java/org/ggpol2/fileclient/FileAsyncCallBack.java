package org.ggpol2.fileclient;

/**
 * Created by End-User on 2015-06-02.
 */
public interface FileAsyncCallBack {

    boolean onStart(boolean bStart);

    int onResult(int nPercent);

    boolean onComplete(boolean bComp);
}
