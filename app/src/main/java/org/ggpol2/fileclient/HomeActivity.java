package org.ggpol2.fileclient;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;

import static android.view.View.OnClickListener;

public class HomeActivity extends AppCompatActivity  {


    private Button mBtnStartUpload;
    private Button mBtnProgressDlg;

    //콜백 리턴값
    //private static ArrayList<FileNameStatus> mArrFileList=null;

    private AsyncTask<Integer, String, Integer> mProgressDlg;


    ProgressDialog mProgressBar;
    public int mProgressBarStatus = 0;
    private Handler mProgressBarbHandler = new Handler();
    private long mFileSize = 0L;

    public Context getContext() {
        return (Context)this;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 버튼 객체 설정
        addFileUploadBtnListener();
        addProgressBtnListener();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //파일업로드 버튼 클릭
    public void addFileUploadBtnListener(){
        mBtnStartUpload = (Button) findViewById(R.id.btnStartUpload);

        //업로드할 파일객체 등록
        final ArrayList<FileNameStatus> arrFileList=new ArrayList<FileNameStatus>();
        arrFileList.add(rtnFiNmaeStatus("/mnt/sdcard/ML-3475_Print.zip"));
        arrFileList.add(rtnFiNmaeStatus("/mnt/sdcard/Tulips.jpg"));
        arrFileList.add(rtnFiNmaeStatus("/mnt/sdcard/ML1750.zip"));
        arrFileList.add(rtnFiNmaeStatus("/mnt/sdcard/IPChanger_3_0_15.zip"));




        mBtnStartUpload.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                Logger.d("startUpload!!");

                new ProgressBarDlg(getContext(), arrFileList).execute(100);


            }
        });

    }



    // 비동기로 실행된 결과를 받아 처리하는 코드
//
//    private AsyncCallback<ToonDataList> callback = new AsyncCallback<ToonDataList>() {
//        @Override
//        public void onResult(ToonDataList result) {
//            appendResult(result);
//        }
//
//        @Override
//        public void exceptionOccured(Exception e) {
//            AlertUtil.alert(context, context.getString(R.string.dataloading_error));
//        }
//
//        @Override
//        public void cancelled() {
//        }
//    };



    public void addProgressBtnListener() {

        mBtnProgressDlg = (Button) findViewById(R.id.btnProgressDialog);

        mBtnProgressDlg.setOnClickListener(new OnClickListener() {

        @Override

        public void onClick(View view) {

                // create and display a new ProgressBarDialog

                mProgressBar = new ProgressDialog(view.getContext());
                mProgressBar.setCancelable(true);
                mProgressBar.setMessage("File downloading ...");
                mProgressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressBar.setProgress(0);
                mProgressBar.setMax(100);
                mProgressBar.show();
                mProgressBarStatus = 0;

                mFileSize = 0;



                new Thread(new Runnable() {
                    public void run() {
                        while (mProgressBarStatus < 100) {
                            // process some tasks
                            mProgressBarStatus = (int)downloadFile();
                            // sleep 1 second (simulating a time consuming task...)

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            // Update the progress bar
                            mProgressBarbHandler.post(new Runnable() {
                                public void run() {
                                    mProgressBar.setProgress((int)mProgressBarStatus);
                                }
                            });
                        }
                        // if the file is downloaded,
                        if (mProgressBarStatus >= 100) {
                            // sleep 2 seconds, so that you can see the 100%
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                           // and then close the progressbar dialog
                            mProgressBar.dismiss();
                        }
                    }
                }).start();

            }
        });

    }

    public int downloadFile() {
        while (mFileSize <= 1000000) {

            mFileSize++;

            if (mFileSize == 100000) {
                return 10;
            } else if (mFileSize == 200000) {
                return 20;
            } else if (mFileSize == 300000) {
                return 30;
            } else if (mFileSize == 400000) {
                return 40;
            } else if (mFileSize == 500000) {
                return 50;
            } else if (mFileSize == 700000) {
                return 70;
            } else if (mFileSize == 800000) {
                return 80;
            }
            //...

        }
        return 100;
    }



    public void startProgressBar(){

        mProgressBar = new ProgressDialog(getContext());
        mProgressBar.setCancelable(true);
        mProgressBar.setMessage("File Uploading ...");
        mProgressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressBar.setProgress(0);
        mProgressBar.setMax(100);
        mProgressBar.show();
        mProgressBarStatus = 0;

        new Thread(new Runnable() {
            public void run() {
                while (mProgressBarStatus < 100) {
                    // process some tasks
                   // progressBarStatus = (int)downloadFile();
                    // sleep 1 second (simulating a time consuming task...)

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Update the progress bar
                    mProgressBarbHandler.post(new Runnable() {
                        public void run() {
                            mProgressBar.setProgress((int)mProgressBarStatus);
                        }
                    });
                }
                // if the file is downloaded,
                if (mProgressBarStatus >= 100) {
                    // sleep 2 seconds, so that you can see the 100%
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // and then close the progressbar dialog
                    mProgressBar.dismiss();
                }
            }
        }).start();


    }


    //파일이름,진행상태 객체 리턴
    private static FileNameStatus rtnFiNmaeStatus(String strFilePathName){

        FileNameStatus obj = new FileNameStatus();
        obj.setStrFilePathName(strFilePathName);
        obj.setnFilePercent(0);
        obj.setIsStop(false);
        return obj;
    }






}
