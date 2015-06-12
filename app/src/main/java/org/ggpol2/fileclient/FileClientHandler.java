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


    Thread mProgressThread;
    private ProgressDialog mDlg;
    private Context mContext;



    //1MB POOLED 버퍼
    ByteBuf mPoolBuf;

    ByteBuf mBuffer;
    long mOffest=0L;

    //프로그래스바에 던져줄 값
    private volatile int mPercent;

    private long mFileLenth=0L;

    //콜백
    private static FileAsyncCallBack mFileAsyncCallBack =null;


    public FileClientHandler(Context context){
        this.mContext=context;
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

        mPoolBuf=PooledByteBufAllocator.DEFAULT.directBuffer(1048576);

        mBuffer = mPoolBuf.alloc().buffer(4096);


    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        Logger.d("handlerRemoved");
        mPercent=0;
        mFileAsyncCallBack =null;

        if (ctx.channel().isActive()){
            Logger.d("!!!!!! ctx.channel().isActive()");
            ctx.close();
            mPoolBuf.clear();
        }

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
        //ByteBuf buffer = m_pool_buf.alloc.buffer(file.getName().length() + 12);
        ByteBuf buffer = mPoolBuf.alloc().buffer(4096);
        buffer.writeInt(f_name.length());
        buffer.writeBytes(f_name.getBytes());
        buffer.writeLong(file.length());
        return buffer;
    }


    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {

        Logger.d("channelActive");



        //File file  = new File("/mnt/sdcard/Tulips.jpg");
        File file  = new File("/mnt/sdcard/ML-3475_Print.zip");
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

        mFileLenth=fileLength;


        

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

                    int nWriteLen = (int)Math.min(mFileLenth-offset,buffer.writableBytes());

                    //Logger.t("FileSend").d("SENDING: offset["+offset+"] fileLength["+mFileLenth+"] buffer.writableBytes()["+buffer.writableBytes()+"]");

                    //mPercent=(int)((offset*100)/mFileLenth);
                    mPercent=(int)(offset * 100.0 / mFileLenth + 0.5);

                    Logger.t("FileSend").d("SENDING: offset["+offset+"] fileLength["+mFileLenth+"] mPercent["+mPercent+"]  buffer.writableBytes()["+buffer.writableBytes()+"]");

                    mFileAsyncCallBack.onResult(mPercent);

                    buffer.clear();
                    buffer.writeBytes(fis,nWriteLen);
                    offset += buffer.writerIndex();

                    //ChannelFuture chunkWriteFuture=future.channel().writeAndFlush(buffer);
                    ChannelFuture chunkWriteFuture=ctx.writeAndFlush(buffer);
                    if (offset < mFileLenth) {
                        Logger.t("FileSend").d("call!!");
                        chunkWriteFuture.addListener(this);
                    } else {
                        // Wrote the last chunk - close the connection if thewrite is done.
                        Logger.t("FileSend").d("DONE: fileLength["+mFileLenth+"] offset["+offset+"]");
                        chunkWriteFuture.addListener(ChannelFutureListener.CLOSE);
                        fis.close();
                    }

                }

            });

            lastContentFuture = sendFileFuture;

        } else {
            Logger.d("ssl transfer");
            sendFileFuture = ctx.writeAndFlush(new ChunkedFile(raf, 0, fileLength, 8192),ctx.newProgressivePromise());
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.


            sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {

                    mPercent=(int)((progress*100)/mFileLenth);

                    if (total < 0) { // total unknown
                        //System.err.println(future.channel() + " Transfer progress: " + progress);
                        Logger.d(future.channel() + " Transfer progress: " + progress+"("+mPercent+"%)");

                    } else {

                        Logger.d(future.channel() + " Transfer progress: " + progress + " / " + total);
                        //Logger.d("!!! mPercent :"+mPercent);

                    }
                    //((HomeActivity)mContext).myCallBack(mPercent);


                    mFileAsyncCallBack.onResult(mPercent);


                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) {
                    Logger.d(future.channel() + " Transfer complete.");
                    mPercent=100;
                }
            });

            lastContentFuture = sendFileFuture;
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

}
