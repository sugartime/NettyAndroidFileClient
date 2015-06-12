package org.ggpol2.fileclient;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by End-User on 2015-06-02.
 */
public class ProgressBarDlg extends AsyncTask<Integer, String, Integer> {

    private ProgressDialog mDlg;
    private Context mContext;

    private Thread mFileClientThread;
    private Runnable mRunnable=null;

    //private static ExecutorService pool = Executors.newFixedThreadPool(1);
    private static ExecutorService pool = Executors.newCachedThreadPool();
    Future<?> mRunnableFuture;

    private boolean mIsRun;

    //콜백 리턴값
    private int mPercent=0;
    private boolean mComplete;





    /*실행순서
     onPreExecute() -> doInBackground() -> publishProgress() 를 통해서 넘어온 값으로 onProgressUpdate()가 실행 -> onPostExecute()
    */

    public ProgressBarDlg(Context context) {
        mContext = context;
        mIsRun=true;
        mFileClientThread =new Thread( new Runnable(){
                @Override
                public void run() {
                    try {
                        new FileClient(mContext, false).start();
                    } catch (Exception e) {
                        Logger.e("!! Error!!!!!");
                        mIsRun = false;
                        e.printStackTrace();
                    }
                }
        });


        FileClientHandler.setAsyncCallBack(fileAsyncCallBack);

    }

    //콜백연렬
    private FileAsyncCallBack fileAsyncCallBack = new FileAsyncCallBack() {

        @Override
        public  int onResult(int nPercent) {
            mPercent=nPercent;
            return mPercent;
        }

        @Override
        public boolean onStart(boolean bStart) {
            return bStart;
        }

        @Override
        public boolean onComplete(boolean bComp) {
            return bComp;
        }
    };



    @Override
    protected void onPreExecute() {
        mDlg = new ProgressDialog(mContext);
        mDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDlg.setMessage("Start");
        mDlg.show();


        //mFileClientThread.start();
        mRunnableFuture= pool.submit(mFileClientThread);


        super.onPreExecute();
    }



    @Override
    protected Integer doInBackground(Integer... params) {

        final int taskCnt = params[0];
        publishProgress("max", Integer.toString(taskCnt));

        int progressBarStatus=0;

        //Logger.d("~ mIsRun: "+mIsRun+" progressBarStatus" + progressBarStatus);

        while( progressBarStatus < taskCnt){

            if(mIsRun==false) return progressBarStatus;


                progressBarStatus = mPercent;

                Logger.d("~ mIsRun: " + mIsRun + " progressBarStatus " + progressBarStatus);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 작업이 진행되면서 호출하며 화면의 업그레이드를 담당하게 된다

                publishProgress("progress", Integer.toString(progressBarStatus),
                        "Task " + Integer.toString(progressBarStatus));


        }


        // 수행이 끝나고 리턴하는 값은 다음에 수행될 onProgressUpdate 의 파라미터가 된다
        return taskCnt;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        if (progress[0].equals("progress")) {
            mDlg.setProgress(Integer.parseInt(progress[1]));
            mDlg.setMessage(progress[2]);
        } else if (progress[0].equals("max")) {
            mDlg.setMax(Integer.parseInt(progress[1]));
        }
    }

    @Override
    protected void onPostExecute(Integer result) {

        //Logger.d("mFileClientThread.isAlive()= "+mFileClientThread.isAlive());

        mDlg.dismiss();
        //pool.shutdown();
        if(mRunnableFuture.isDone()){
            Logger.d("Runnable is done !");

        }else{
            Logger.d("Runnable is not !");
        }

        String msg="";

        if(mIsRun) msg="전송완료";
        else msg="네트워크가 정상적이지 않습니다.";

        Toast.makeText(mContext, Integer.toString(result) + msg,
                Toast.LENGTH_SHORT).show();
    }
}
