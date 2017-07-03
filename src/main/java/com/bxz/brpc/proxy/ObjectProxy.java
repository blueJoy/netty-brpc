package com.bxz.brpc.proxy;

import com.bxz.brpc.client.ConnectManager;
import com.bxz.brpc.handler.RpcClientHandler;
import com.bxz.brpc.protocol.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by baixiangzhu on 2017/7/3.
 */
@Slf4j
public class ObjectProxy<T> implements InvocationHandler,IAsyncObjectProxy {

    //原始类
    private Class<T> originClass;

    public ObjectProxy(Class<T> interfaceClass) {
        this.originClass = interfaceClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(UUID.randomUUID().toString());
        rpcRequest.setClassName(method.getDeclaringClass().getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterTypes(method.getParameterTypes());
        rpcRequest.setParameters(args);

        RpcClientHandler handler = ConnectManager.getInstance().chooseHandler();

        RpcFuture rpcFuture = handler.sendRequest(rpcRequest);

        return rpcFuture.get();
    }

    @Override
    public RpcFuture call(String methodName, Object... args) {
        RpcClientHandler handler = ConnectManager.getInstance().chooseHandler();
        RpcRequest request = createRequest(this.originClass.getName(),methodName,args);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture;
    }

    private RpcRequest createRequest(String className, String methodName, Object[] args) {

        RpcRequest request = new RpcRequest();
        request.setClassName(className);
        request.setRequestId(UUID.randomUUID().toString());
        request.setMethodName(methodName);
        request.setParameters(args);

        Class[] parameterTypes = new Class[args.length];
        for(int i = 0; i< args.length ; i++){
            parameterTypes[i] = getClassType(args[i]);
        }

        request.setParameterTypes(parameterTypes);

        return request;

    }

    //获取参数类型
    private Class getClassType(Object obj) {

        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName){
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }

        return classType;
    }
}
