package org.ggpol2.fileclient;

/**
 * Created by End-User on 2015-06-09.
 */
public interface AsyncCallback<T>{
    public void onResult(T result);

    public void exceptionOccured(Exception e);

    public void cancelled();
}
