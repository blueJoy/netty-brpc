package com.bxz.brpc.client;

import com.bxz.brpc.handler.RpcClientHandler;
import com.bxz.brpc.protocol.RpcDecoder;
import com.bxz.brpc.protocol.RpcEncoder;
import com.bxz.brpc.protocol.RpcRequest;
import com.bxz.brpc.protocol.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by baixiangzhu on 2017/7/3.
 */
@Slf4j
public class ConnectManager {

    private volatile static ConnectManager connectManager;

    EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private static ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(16,16,600L,
                    TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(65536));



    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();
    //记录连接的服务数量
    private Map<InetSocketAddress,RpcClientHandler> connectedServerNodes =new ConcurrentHashMap<>();

    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    protected long connectTimeoutMillis = 6000L;

    private AtomicInteger roundRobin = new AtomicInteger(0);

    //运行状态
    private volatile boolean isRunning = true;


    private ConnectManager(){}

    public static ConnectManager getInstance(){

        if(connectManager == null){
            synchronized (ConnectManager.class){
                if(connectManager == null){
                    connectManager = new ConnectManager();
                }
            }
        }

        return connectManager;
    }


    public void updateConnectedServer(List<String> allServerAddress){

    }

    public void reconnect(){

    }


    private void connectServerNode(final InetSocketAddress remotePeer){

        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {

                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {

                                ch.pipeline()
                                        .addLast(new RpcEncoder(RpcRequest.class))
                                        .addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0))
                                        .addLast(new RpcDecoder(RpcResponse.class))
                                        .addLast(new RpcClientHandler());
                            }
                        });

                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if(channelFuture.isSuccess()){
                            log.debug("connect to remote[{}] server successfully!");
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            addHandler(handler);
                        }else {
                            log.debug("connected to remote[{}] server failed!",remotePeer);
                        }

                    }
                });
            }
        });

    }

    private void addHandler(RpcClientHandler handler) {
        connectedHandlers.add(handler);
        //获取连接地址
        SocketAddress remoteAddress = handler.getChannel().remoteAddress();
        connectedServerNodes.put((InetSocketAddress) remoteAddress,handler);
        signalAvailableHandler();
    }


    //唤醒所有等待线程
    private void signalAvailableHandler() {
        lock.lock();
        try{
            //唤醒在此Lock对象上等待的所有线程。
            connected.signalAll();
        }finally {
            lock.unlock();
        }
    }

    //等待被唤醒
    private boolean waitingForHandler() throws InterruptedException {

        lock.lock();
        try {
            return connected.await(this.connectTimeoutMillis,TimeUnit.MILLISECONDS);
        }finally {
            lock.unlock();
        }
    }

    /**
     * 获得rpcHandler
     * @return
     */
    public RpcClientHandler chooseHandler(){

        CopyOnWriteArrayList<RpcClientHandler> handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlers.clone();

        int size = handlers.size();

        while (isRunning && size <=0 ){

            try{
                boolean available = waitingForHandler();
                if(available){
                    handlers = (CopyOnWriteArrayList<RpcClientHandler>) this.connectedHandlers.clone();
                    size = handlers.size();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int index = (roundRobin.getAndAdd(1) + size) % size;

        return handlers.get(index);
    }

    //停止客户端
    public void stop(){
        isRunning = false;
        for(RpcClientHandler handler : connectedHandlers){
            handler.close();
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

}
