package com.bxz.brpc.handler;

import com.bxz.brpc.protocol.RpcRequest;
import com.bxz.brpc.protocol.RpcResponse;
import com.bxz.brpc.proxy.RpcFuture;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by baixiangzhu on 2017/7/3.
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    /**
     * 缓存执行的任务   key:requestId --> value:future
     */
    private ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<>();


    private volatile Channel channel;

    private SocketAddress remoteAddr;


    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remoteAddr = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {

        String requestId = rpcResponse.getRequestId();
        RpcFuture rpcFuture = pendingRPC.get(requestId);
        if(rpcFuture != null){
            pendingRPC.remove(requestId);
            rpcFuture.done(rpcResponse);
        }

    }

    public void close(){
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
    }


    public RpcFuture sendRequest(RpcRequest rpcRequest) {

        RpcFuture rpcFuture = new RpcFuture(rpcRequest);
        pendingRPC.put(rpcRequest.getRequestId(),rpcFuture);
        channel.writeAndFlush(rpcRequest);

        return rpcFuture;
    }
}
