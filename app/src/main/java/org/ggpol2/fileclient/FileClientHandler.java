package org.ggpol2.fileclient;

import android.app.ProgressDialog;
import android.content.Context;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.orhanobut.logger.Logger;
/**
 * Created by End-User on 2015-05-29.
 */
public class FileClientHandler extends SimpleChannelInboundHandler<Object> {

    static final String TAG = "FileClientHandler";

    Thread mProgressThread;
    private ProgressDialog mDlg;
    private Context mContext;

    private String mFilePathName;

    //1MB POOLED 버퍼
    ByteBuf mPoolBuf;
    ByteBuf mBuffer;
    long mOffest=0L;

    //프로그래스바에 던져줄 값
    private volatile int mPercent;

    private volatile long mProgress;

    private long mFileLength=0L;

    //콜백
    private static FileAsyncCallBack mFileAsyncCallBack =null;

    //업로드 데이터의 처음 시작인지 여부
    private Boolean mIsFirstUpload;

    public FileClientHandler(Context context,String filePathName){
        this.mContext=context;
        this.mFilePathName=filePathName;

        mPoolBuf=PooledByteBufAllocator.DEFAULT.directBuffer(FileClientConstants.POOL_BUF_SIZE);
    }

    //콜벡 셋팅
    public static void setAsyncCallBack(FileAsyncCallBack fileAsyncCallBack){
        mFileAsyncCallBack = fileAsyncCallBack;
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        //System.out.println("handlerAdded");
        Logger.d("handlerAdded");

        mBuffer = mPoolBuf.alloc().buffer(FileClientConstants.SND_BUF_SIZE);

    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        Logger.d("handlerRemoved");

        ctx.close();
        mPoolBuf.release();
        mPoolBuf=null;

        /*
        if (ctx.channel().isActive()){
            Logger.d("!!!!!! ctx.channel().isActive()");
            ctx.close();
            mPoolBuf.clear();
        }
        */

    }

    /**
     * Protocol 초기화
     * [4byte|?byte|8byte|chunk ?byte]
     * [filenamelen|filenamedata|filedatalen|filedatadatachunks]
     *
     * @param file
     * @return
     */
    private ByteBuf initializeProtocol(File file) {


        String f_name ="";
        try {
            f_name = URLEncoder.encode(file.getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ByteBuf buffer = mPoolBuf.alloc().buffer(FileClientConstants.INIT_BUF_SIZE);     //고정크기 512
        buffer.writeInt(f_name.length());                                                //파일이름 길이(4)
        buffer.writeBytes(f_name.getBytes());                                            //파일이름에따라 틀림
        buffer.writeLong(file.length());                                                 //파일크기(8)
        int nFillZero = buffer.capacity()-buffer.writerIndex();
        buffer.writeZero(nFillZero);                                                     //나머지 부분을 0으로 셋팅해서 버퍼크기를 맞춤
        return buffer;
    }


    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {

        Logger.d("channelActive");

        File file  = new File(mFilePathName);

        //File file  = new File("/mnt/sdcard/Tulips.jpg");
       // File file  = new File("/mnt/sdcard/ML-3475_Print.zip");
        //File file  = new File("E:\\UTIL\\dotnet framework 4.0\\dotNetFx40_Full_x86.exe");
        //File file  = new File("E:\\UTIL\\ML-3475ND\\ML-3475_Print.zip");

        RandomAccessFile raf = null;
        long fileLength = -1;
        try {
              raf = new RandomAccessFile(file, "r");
            fileLength = raf.length();
        } catch (Exception e) {
             //ctx.writeAndFlush("ERR: " + e.getClass().getSimpleName() + ": " + e.getMessage() + '\n');
             return;
        } finally {
            if (fileLength < 0 && raf != null) {
                   raf.close();
            }
        }

        mFileLength=fileLength;


        

        ByteBuf bufferHead = initializeProtocol(file);

        // Write the initial line and the header.
        ctx.writeAndFlush(bufferHead);

        // Write the content.
        final ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;

        if (ctx.pipeline().get(SslHandler.class) == null) {

            Logger.d("transfer");
            //sendFileFuture =ctx.writeAndFlush(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise()); //일케 하면 안됨
            //sendFileFuture =ctx.writeAndFlush(new ChunkedFile(raf, 0, fileLength, 8192),ctx.newProgressivePromise()); //일케 해야됨

            final FileInputStream fis = new FileInputStream(file);


            mBuffer.writeBytes(fis, (int) Math.min(fileLength - mOffest,mBuffer.writableBytes()));
            mOffest += mBuffer.writerIndex();
            sendFileFuture = ctx.writeAndFlush(mBuffer);
            mBuffer.clear();
            sendFileFuture.addListener(new ChannelFutureListener(){
                private long offset=mOffest;

                @Override
                public void operationComplete(ChannelFuture future)	throws Exception {
                    // TODO Auto-generated method stub

                    if (!future.isSuccess()) {
                        Logger.t("FileSend").d("Fail!!");
                        future.cause().printStackTrace();
                        future.channel().close();
                        fis.close();
                        return;
                    }


                    ByteBuf buffer =  mPoolBuf.alloc().buffer(4096);

                    int nWriteLen = (int)Math.min(mFileLength-offset,buffer.writableBytes());

                    //Logger.t("FileSend").d("SENDING: offset["+offset+"] fileLength["+mFileLenth+"] buffer.writableBytes()["+buffer.writableBytes()+"]");

                    //mPercent=(int)((offset*100)/mFileLenth);
                    mPercent=(int)(offset * 100.0 / mFileLength + 0.5);

                    Logger.t(TAG).d("SENDING: offset["+offset+"] fileName["+mFilePathName+"] fileLength["+mFileLength+"] mPercent["+mPercent+"]  buffer.writableBytes()["+buffer.writableBytes()+"]");

                    //콜백에 전달
                    FileNameStatus fileNameStauts = new FileNameStatus();
                    fileNameStauts.setStrFilePathName(mFilePathName);
                    fileNameStauts.setnFilePercent(mPercent);
                    fileNameStauts.setlProgress(offset);
                    fileNameStauts.setIsComplete(false);

                    mFileAsyncCallBack.onResult(fileNameStauts);

                    buffer.clear();
                    buffer.writeBytes(fis,nWriteLen);
                    offset += buffer.writerIndex();

                    //ChannelFuture chunkWriteFuture=future.channel().writeAndFlush(buffer);
                    ChannelFuture chunkWriteFuture=ctx.writeAndFlush(buffer);
                    if (offset < mFileLength) {
                        Logger.t(TAG).d("call!!");
                        chunkWriteFuture.addListener(this);
                    } else {
                        // Wrote the last chunk - close the connection if thewrite is done.
                        Logger.t(TAG).d("DONE: fileLength["+mFileLength+"] offset["+offset+"]");

                        fileNameStauts.setnFilePercent(100);
                        fileNameStauts.setlProgress(offset);
                        fileNameStauts.setIsComplete(true);
                        mFileAsyncCallBack.onResult(fileNameStauts);

                        chunkWriteFuture.addListener(ChannelFutureListener.CLOSE);
                        fis.close();
                    }

                }

            });

            //lastContentFuture = sendFileFuture;

        } else {
            Logger.t(TAG).d("ssl transfer");

            mIsFirstUpload=true;


            sendFileFuture = ctx.writeAndFlush(new ChunkedFile(raf, 0, fileLength, 8192),ctx.newProgressivePromise());
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.


            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {

                    mPercent=(int)((progress*100)/mFileLength);

                    mProgress=progress; //complete 에서 사용하기 위해서

                    if (total < 0) { // total unknown
                        //System.err.println(future.channel() + " Transfer progress: " + progress);
                        Logger.t(TAG).d(future.channel() + " Transfer progress: ["+mFilePathName+"]" + progress + "(" + mPercent + "%)");

                    } else {

                        Logger.t(TAG).d(future.channel() + " Transfer progress: ["+mFilePathName+"]" + progress + " / " + total);
                        //Logger.d("!!! mPercent :"+mPercent);

                    }
                    //((HomeActivity)mContext).myCallBack(mPercent);


                    //콜백에 전달
                    FileNameStatus fileNameStauts = new FileNameStatus();
                    fileNameStauts.setStrFilePathName(mFilePathName);
                    fileNameStauts.setnFilePercent(mPercent);
                    fileNameStauts.setlProgress(progress);
                    fileNameStauts.setIsComplete(false);


                    //if(mIsFirstUpload)  fileNameStauts.setIsFirstUpload(true);
                    mFileAsyncCallBack.onResult(fileNameStauts);
                    //mIsFirstUpload=false;



                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) {
                    Logger.t(TAG).d(future.channel() + " Transfer complete. " + "mFilePathName[" + mFilePathName + "],mPercent[" + mPercent+"],mFileLength["+mFileLength+"]");

                    //콜백에 전달
                    FileNameStatus fileNameStauts = new FileNameStatus();
                    fileNameStauts.setStrFilePathName(mFilePathName);
                    fileNameStauts.setnFilePercent(mPercent);
                    fileNameStauts.setlProgress(mProgress);
                    fileNameStauts.setIsComplete(true);


                    mFileAsyncCallBack.onResult(fileNameStauts);


                    sendFileFuture.addListener(ChannelFutureListener.CLOSE);

                }
            });

            //lastContentFuture = sendFileFuture;
            //lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }


        // Decide whether to close the connection or not.
        Logger.t("FileSend").d("Close");
        //lastContentFuture.addListener(ChannelFutureListener.CLOSE);

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)	throws Exception {
        // TODO Auto-generated method stub
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();

        Logger.d("exceptionCaught");

        if (ctx.channel().isActive()) {
                          //ctx.writeAndFlush("ERR: " +
                          //cause.getClass().getSimpleName() + ": " +
                          //cause.getMessage() + '\n').addListener(ChannelFutureListener.CLOSE);
         }
    }

    public long byteToMB(long byteTransform)
    {
        long mb=1024L*1024L;
        return byteTransform/mb;

    }

}
