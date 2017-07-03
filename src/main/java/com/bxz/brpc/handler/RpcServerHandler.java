package com.bxz.brpc.handler;

import com.bxz.brpc.protocol.RpcRequest;
import com.bxz.brpc.protocol.RpcResponse;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 处理请求的处理器
 * Created by baixiangzhu on 2017/6/29.
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest>{

    private final Map<String,Object> handlerMap;

    public RpcServerHandler(Map<String, Object> beanRegistryMap) {

        this.handlerMap = beanRegistryMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final RpcRequest rpcRequest) throws Exception {

        RpcResponse response = new RpcResponse();
        response.setRequestId(rpcRequest.getRequestId());
        try {

            Object result =  handle(rpcRequest);
            response.setResult(result);
        }catch (Exception e){
            response.setError(e.getMessage());
        }

        ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                log.info("Send response for request id=[{}] :",rpcRequest.getRequestId());
            }
        });

    }


    //TODO:待做
    private Object handle(RpcRequest rpcRequest) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("request = [{}]",rpcRequest);

        String className = rpcRequest.getClassName();
        Object serviceBean = handlerMap.get(className);

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = rpcRequest.getMethodName();
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
        Object[] parameters = rpcRequest.getParameters();

        for (int i = 0; i < parameterTypes.length; i++){
            log.debug("parameterTypes[{}] is [{}]",i,parameterTypes[i]);
        }

        for (int i =0; i < parameters.length; i++){
            log.debug("parameters[{}] is [{}]",i,parameters[i]);
        }

        //JDK - reflect
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        //强制访问
        method.setAccessible(true);
        Object invoke = method.invoke(serviceBean, parameters);

        log.info("result is [{}]",invoke);

        return invoke;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
