package org.ggpol2.fileclient;

import android.content.Context;

import com.orhanobut.logger.Logger;

import java.io.File;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Created by End-User on 2015-06-08.
 */
public class FileClient{

    private Context mContext;

    private String mFilePathName;

    private int mPort;

    public FileClient(Context context,String filePathName) {

        this.mContext=context;

        this.mPort = (FileClientConstants.IS_SSL ? FileClientConstants.SSL_PORT : FileClientConstants.PORT);

        this.mFilePathName=filePathName;


    }

    public FileClient start() throws Exception {

        //((HomeActivity)mContext).startProgressBar2();

        // Configure SSL.

        final SslContext sslCtx;
        if (FileClientConstants.IS_SSL) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            //sslCtx = SslContextBuilder.forServer(f_certificate, f_privatekey,"12345").build();


        } else {
            sslCtx = null;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), FileClientConstants.HOST, mPort));
                            }
                            p.addLast(//new StringEncoder(CharsetUtil.UTF_8),
                                    new ChunkedWriteHandler(),
                                    new FileClientHandler(mContext,mFilePathName));
                        }
                    });

            // Start the connection attempt.
            ChannelFuture f =  b.connect(FileClientConstants.HOST, mPort).sync();

            f.awaitUninterruptibly();

            if (f.isCancelled()) {
                // Connection attempt cancelled by user
            } else if (!f.isSuccess()) {
                Logger.e("Netty Error !!!!!");
                f.cause().printStackTrace();
            } else {
                // Connection established successfully
                Logger.d("Netty Connection Success !!");
            }

            f.channel().closeFuture().sync();


        } finally {
            // The connection is closed automatically on shutdown.
            group.shutdownGracefully();
        }
        Logger.d(" netty 종료~");

        return this;
    }



}
