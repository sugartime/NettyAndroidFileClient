package org.ggpol2.fileclient;

/**
 * Created by End-User on 2015-06-09.
 */
public interface AsyncExecutorAware <T> {
    public void setAsyncExecutor(AsyncExecutor<T> asyncExecutor);
}
