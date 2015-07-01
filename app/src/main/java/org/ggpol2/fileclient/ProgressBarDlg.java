package org.ggpol2.fileclient;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by End-User on 2015-06-02.
 * Params, Progress, Result
 */
public class ProgressBarDlg extends AsyncTask<Integer, String, Integer> {

    private ProgressDialog mDlg;
    private Context mContext;

    //업로드할 파일 경로및 이름
    private String mFilePathName;
    private ArrayList<FileNameStatus> mArrFileList;

    private Thread mFileClientThread;
    private Runnable mRunnable=null;

    //private static ExecutorService pool = Executors.newFixedThreadPool(1);
    private static ExecutorService pool = Executors.newCachedThreadPool();
    Future<?> mRunnableFuture;

    private boolean mIsRun;

    //
    private Callable<FileClient> mCallable;

    //콜백 리턴값
    private int mPercent=0;
    private boolean mComplete;





    /*실행순서
     onPreExecute() -> doInBackground() -> publishProgress() 를 통해서 넘어온 값으로 onProgressUpdate()가 실행 -> onPostExecute()
    */

    public ProgressBarDlg(Context context,ArrayList<FileNameStatus>arrFileList) {
        mContext = context;
        mArrFileList=arrFileList;
        mIsRun=true;

        /*
        mFileClientThread =new Thread( new Runnable(){
                @Override
                public void run() {
                    try {
                        new FileClient(mContext, mFilePathName).start();
                    } catch (Exception e) {
                        Logger.e("!! Error!!!!!");
                        mIsRun = false;
                        e.printStackTrace();
                    }
                }
        });
        */




        /*
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        */


        //콜백연결
        //FileClientHandler.setAsyncCallBack(fileAsyncCallBack);

    }

    //콜백
    private FileAsyncCallBack fileAsyncCallBack = new FileAsyncCallBack() {

        /*
        @Override
        public  int onResult(int nPercent) {
            mPercent=nPercent;
            return mPercent;
        }*/



        @Override
        public boolean onStart(boolean bStart) {
            return false;
        }

        @Override
        public void onStop(String filePathName) {
            for(FileNameStatus obj : mArrFileList){
                if(obj.getStrFilePathName().equals(filePathName)){
                    obj.setnFilePercent(100);
                }
            }
        }

        @Override
        public void onResult(FileNameStatus fileNameStatus) {
            for(FileNameStatus obj : mArrFileList){
                if(obj.getStrFilePathName().equals(fileNameStatus.getStrFilePathName())){
                    obj.setnFilePercent(fileNameStatus.getnFilePercent());
                }
            }
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


        //Future<FileClient> future= pool.submit(mCallable);

        /*
        try {
            future.get();//하면안됨 ....
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        */


        //mRunnableFuture= pool.submit(mFileClientThread);





        super.onPreExecute();
    }



    // UI 스레드에서 AsynchTask객체.execute(...) 명령으로 실행되는 callback
    @Override
    protected Integer doInBackground(Integer... params) {
        //Background에서 수행할 작업을 구현 ProgressbarDlg.execute(…) 메소드에 입력된 인자들을 전달 받음.

        final int taskCnt = params[0];
        publishProgress("max", Integer.toString(taskCnt));

        //콜백연결
        FileClientHandler.setAsyncCallBack(fileAsyncCallBack);

        ExecutorService executor = Executors.newCachedThreadPool();

        for(final FileNameStatus obj : mArrFileList) {
            Callable<FileClient> callable = new Callable<FileClient>() {
                @Override
                public FileClient call() throws Exception {
                    try {
                        return new FileClient(mContext, obj.getStrFilePathName()).start();
                    } catch (ConnectException e) {
                        e.printStackTrace();
                        return null;
                    }

                }
            };

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {

                    //final int taskCnt = 100;
                    int progressBarStatus = 0;
                    String fileNmae = obj.getStrFilePathName();

                    while (progressBarStatus < taskCnt) {


                        progressBarStatus = obj.getnFilePercent();

                        //if(mThreadStop)progressBarStatus=100;

                        //if(mThreadStop) return;

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Logger.d("Thread.currentThread().isInterrupted()["+Thread.currentThread().isInterrupted()+"] fileName["+fileNmae+"] progressBarStatus ["+progressBarStatus+"%]");

                        // 작업이 진행되면서 호출하며 화면의 업그레이드를 담당하게 된다
                        //publishProgress("progress", Integer.toString(progressBarStatus),
                        //        "Task " + Integer.toString(progressBarStatus));

                        publishProgress("progress", Integer.toString(progressBarStatus), fileNmae);


                    }

                }
            });

            t.start();

            Future<FileClient> future = executor.submit(callable);

            try {
                if(future.get()==null){
                    //t.interrupt();
                    fileAsyncCallBack.onStop(obj.getStrFilePathName());
                } else{
                    t.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }


        }//mArrayList

        executor.shutdown();


        Logger.d("doInBackground... End");


        // 수행이 끝나고 리턴하는 값은 다음에 수행될 onProgressUpdate 의 파라미터가 된다
        return taskCnt;
    }

    // onInBackground(...)에서 publishProgress(...)를 사용하면
    // 자동 호출되는 callback으로
    // 이곳에서 ProgressBar를 증가 시키고, text 정보를 update하는 등의
    // background 작업 진행 상황을 UI에 표현함.
    @Override
    protected void onProgressUpdate(String... progress) {
        if (progress[0].equals("progress")) {
            mDlg.setProgress(Integer.parseInt(progress[1]));
            mDlg.setMessage(progress[2]);
        } else if (progress[0].equals("max")) {
            mDlg.setMax(Integer.parseInt(progress[1]));
        }
    }

    // doInBackground(...)가 완료되면 자동으로 실행되는 callback
    // 이곳에서 onInBackground가 리턴한 정보를 UI위젯에 표시 하는 등의 작업을 수행함.
    // (예제에서는 작업에 걸린 총 시간을 UI위젯 중 TextView에 표시함)
    @Override
    protected void onPostExecute(Integer result) {

        //Logger.d("mFileClientThread.isAlive()= "+mFileClientThread.isAlive());

        mDlg.dismiss();
        //pool.shutdown();

        /*
        if(mRunnableFuture.isDone()){
            Logger.d("Runnable is done !");

        }else{
            Logger.d("Runnable is not !");
        }
        */

        String msg="";

        if(mIsRun) msg=" 전송완료";
        else msg="네트워크가 정상적이지 않습니다.";

        Toast.makeText(mContext, Integer.toString(result) + msg,
                Toast.LENGTH_SHORT).show();
    }
}
