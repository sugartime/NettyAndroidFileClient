package org.ggpol2.fileclient;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import java.util.concurrent.Callable;

/**
 * Created by End-User on 2015-06-09.
 */
public class AsyncExecutor <T> extends AsyncTask<Void, Void, T> {
    private static final String TAG = "AsyncExecutor";

    private AsyncCallback<T> callback;
    private Callable<T> callable;
    private Exception occuredException;

    public AsyncExecutor<T> setCallable(Callable<T> callable) {
        this.callable = callable;
        return this;
    }

    public AsyncExecutor<T> setCallback(AsyncCallback<T> callback) {
        this.callback = callback;
        processAsyncExecutorAware(callback);
        return this;
    }

    @SuppressWarnings("unchecked")
    private void processAsyncExecutorAware(AsyncCallback<T> callback) {
        if (callback instanceof AsyncExecutorAware) {
            ((AsyncExecutorAware<T>) callback).setAsyncExecutor(this);
        }
    }

    @Override
    protected T doInBackground(Void... params) {
        try {
            return callable.call();
        } catch (Exception ex) {
            Logger.t(TAG).d("exception occured while doing in background: " + ex.getMessage(), ex);
            this.occuredException = ex;
            return null;
        }
    }


    @Override
    protected void onPostExecute(T result) {
        if (isCancelled()) {
            notifyCanceled();
        }
        if (isExceptionOccured()) {
            notifyException();
            return;
        }
        notifyResult(result);
    }

//    @Override
//    protected void onPreExecute() {
//        mDlg = new ProgressDialog(mContext);
//        mDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        mDlg.setMessage("Start");
//        mDlg.show();
//
//
//        //mFileClientThread.start();
//        mRunnableFuture= pool.submit(mFileClientThread);
//
//
//        super.onPreExecute();
//    }
//
//    @Override
//    protected Integer doInBackground(Integer... params) {
//
//        final int taskCnt = params[0];
//        publishProgress("max", Integer.toString(taskCnt));
//
//        int progressBarStatus=0;
//
//        //Logger.d("~ mIsRun: "+mIsRun+" progressBarStatus" + progressBarStatus);
//
//        while( progressBarStatus < taskCnt){
//
//            if(mIsRun==false) return progressBarStatus;
//
//
//            progressBarStatus = mPercent;
//
//            Logger.d("~ mIsRun: " + mIsRun + " progressBarStatus " + progressBarStatus);
//
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            // 작업이 진행되면서 호출하며 화면의 업그레이드를 담당하게 된다
//
//            publishProgress("progress", Integer.toString(progressBarStatus),
//                    "Task " + Integer.toString(progressBarStatus));
//
//
//        }
//
//
//        // 수행이 끝나고 리턴하는 값은 다음에 수행될 onProgressUpdate 의 파라미터가 된다
//        return taskCnt;
//    }
//
//    @Override
//    protected void onProgressUpdate(String... progress) {
//        if (progress[0].equals("progress")) {
//            mDlg.setProgress(Integer.parseInt(progress[1]));
//            mDlg.setMessage(progress[2]);
//        } else if (progress[0].equals("max")) {
//            mDlg.setMax(Integer.parseInt(progress[1]));
//        }
//    }
//
//    @Override
//    protected void onPostExecute(Integer result) {
//
//        //Logger.d("mFileClientThread.isAlive()= "+mFileClientThread.isAlive());
//
//        mDlg.dismiss();
//        //pool.shutdown();
//        if(mRunnableFuture.isDone()){
//            Logger.d("Runnable is done !");
//
//        }else{
//            Logger.d("Runnable is not !");
//        }
//
//        String msg="";
//
//        if(mIsRun) msg="전송완료";
//        else msg="네트워크가 정상적이지 않습니다.";
//
//        Toast.makeText(mContext, Integer.toString(result) + msg,
//                Toast.LENGTH_SHORT).show();
//    }

    private void notifyCanceled() {
        if (callback != null)
            callback.cancelled();
    }

    private boolean isExceptionOccured() {
        return occuredException != null;
    }

    private void notifyException() {
        if (callback != null)
            callback.exceptionOccured(occuredException);
    }

    private void notifyResult(T result) {
        if (callback != null)
            callback.onResult(result);
    }
}
