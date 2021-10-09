package com.sc.job.core.server;

import com.sc.job.core.biz.JobExecutorBiz;
import com.sc.job.core.biz.service.JobExecutorBizService;
import com.sc.job.core.biz.dto.*;
import com.sc.job.core.constants.Constants;
import com.sc.job.core.start.ExecutorRegistryStartThread;
import com.sc.job.core.util.GsonTool;
import com.sc.job.core.util.ThrowableUtil;
import com.sc.job.core.util.ScJobRemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * CoreNettyServer
 * @author scer 2020-04-11 21:25
 */
public class CoreNettyServer {
    private static final Logger logger = LoggerFactory.getLogger(CoreNettyServer.class);

    private JobExecutorBiz executorBiz;
    private Thread thread;

    public void start(final String address, final int port, final String appname, final String accessToken) {
        executorBiz = new JobExecutorBizService();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // param
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                ThreadPoolExecutor bizThreadPool = getBizThreadPool();
                try {
                    // start netty server
                    ChannelFuture future = startNettyServer(bossGroup, workerGroup, bizThreadPool, accessToken, port);

                    logger.info(">>>>>>>>>>> sc-job remoting server start success, nettype = {}, port = {}", CoreNettyServer.class, port);

                    // start registry
                    startRegistry(appname, address);

                    // wait util stop
                    future.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    if (e instanceof InterruptedException) {
                        logger.info(">>>>>>>>>>> sc-job remoting server stop.");
                    } else {
                        logger.error(">>>>>>>>>>> sc-job remoting server error.", e);
                    }
                } finally {
                    // stop
                    try {
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        // daemon, service jvm, user thread leave >>> daemon leave >>> jvm leave
        thread.setDaemon(true);
        thread.start();
    }

    private ChannelFuture startNettyServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup, ThreadPoolExecutor bizThreadPool, String accessToken, int port) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS))  // beat 3N, close if idle
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(5 * 1024 * 1024))  // merge request & reponse to FULL
                                .addLast(new EmbedHttpServerHandler(executorBiz, accessToken, bizThreadPool));
                    }
                })
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // bind
        ChannelFuture future = bootstrap.bind(port).sync();
        return future;
    }

    private ThreadPoolExecutor getBizThreadPool() {
        return new ThreadPoolExecutor(
                0,
                200,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "sc-rpc, EmbedServer bizThreadPool-" + r.hashCode());
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        throw new RuntimeException("sc-job, EmbedServer bizThreadPool is EXHAUSTED!");
                    }
                });
    }

    public void stop() throws Exception {
        // destroy server thread
        if (thread!=null && thread.isAlive()) {
            thread.interrupt();
        }

        // stop registry
        stopRegistry();
        logger.info(">>>>>>>>>>> sc-job remoting server destroy success.");
    }


    // ---------------------- registry ----------------------

    /**
     * netty_http
     *
     * @author scer 2020-11-24 22:25:15
     */
    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private static final Logger logger = LoggerFactory.getLogger(EmbedHttpServerHandler.class);

        private JobExecutorBiz executorBiz;
        private String accessToken;
        private ThreadPoolExecutor bizThreadPool;
        public EmbedHttpServerHandler(JobExecutorBiz executorBiz, String accessToken, ThreadPoolExecutor bizThreadPool) {
            this.executorBiz = executorBiz;
            this.accessToken = accessToken;
            this.bizThreadPool = bizThreadPool;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {

            // request parse
            //final byte[] requestBytes = ByteBufUtil.getBytes(msg.content());    // byteBuf.toString(io.netty.util.CharsetUtil.UTF_8);
            String requestData = msg.content().toString(CharsetUtil.UTF_8);
            String uri = msg.uri();
            HttpMethod httpMethod = msg.method();
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            String accessTokenReq = msg.headers().get(ScJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);

            // invoke
            bizThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    // do invoke
                    Object responseObj = process(httpMethod, uri, requestData, accessTokenReq);

                    // to json
                    String responseJson = GsonTool.toJson(responseObj);

                    // write response
                    writeResponse(ctx, keepAlive, responseJson);
                }
            });
        }

        private Object process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq) {

            // valid
            if (HttpMethod.POST != httpMethod) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
            }
            if (uri==null || uri.trim().length()==0) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
            }
            if (accessToken!=null
                    && accessToken.trim().length()>0
                    && !accessToken.equals(accessTokenReq)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
            }

            // services mapping
            try {
                if (Constants.URI_BEAT.equals(uri)) {
                    return executorBiz.beat();
                } else if (Constants.URI_IDLE_BEAT.equals(uri)) {
                    IdleBeatParam idleBeatParam = GsonTool.fromJson(requestData, IdleBeatParam.class);
                    return executorBiz.idleBeat(idleBeatParam);
                } else if (Constants.URI_RUN.equals(uri)) {
                    TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
                    return executorBiz.run(triggerParam);
                } else if (Constants.URI_KILL.equals(uri)) {
                    KillParam killParam = GsonTool.fromJson(requestData, KillParam.class);
                    return executorBiz.kill(killParam);
                } else if (Constants.URI_LOG.equals(uri)) {
                    LogParam logParam = GsonTool.fromJson(requestData, LogParam.class);
                    return executorBiz.log(logParam);
                } else {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping("+ uri +") not found.");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new ReturnT<String>(ReturnT.FAIL_CODE, "request error:" + ThrowableUtil.toString(e));
            }
        }

        /**
         * write response
         */
        private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
            // write response
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));   //  Unpooled.wrappedBuffer(responseJson)
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");       // HttpHeaderValues.TEXT_PLAIN.toString()
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.writeAndFlush(response);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error(">>>>>>>>>>> sc-job provider netty_http server caught exception", cause);
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.channel().close();      // beat 3N, close if idle
                logger.debug(">>>>>>>>>>> sc-job provider netty_http server close an idle channel.");
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    // ---------------------- registry ----------------------

    public void startRegistry(final String appname, final String address) {
        // start registry
        ExecutorRegistryStartThread.getInstance().start(appname, address);
    }

    public void stopRegistry() {
        // stop registry
        ExecutorRegistryStartThread.getInstance().toStop();
    }


}
