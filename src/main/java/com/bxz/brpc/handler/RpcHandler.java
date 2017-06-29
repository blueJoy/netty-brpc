package com.bxz.brpc.handler;

import com.bxz.brpc.protocol.RpcRequest;
import com.bxz.brpc.protocol.RpcResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 处理请求的处理器
 * Created by baixiangzhu on 2017/6/29.
 */
@Slf4j
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest>{

    private final Map<String,Object> handlerMap;

    public RpcHandler(Map<String, Object> beanRegistryMap) {

        this.handlerMap = beanRegistryMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {

        RpcResponse response = new RpcResponse();
        response.setRequestId(rpcRequest.getRequestId());
        try {

            Object result =  handle(rpcRequest);
            response.setResult(result);
        }catch (Exception e){
            response.setError(e.getMessage());
        }

    }


    //TODO:待做
    private Object handle(RpcRequest rpcRequest) {


    }
}
