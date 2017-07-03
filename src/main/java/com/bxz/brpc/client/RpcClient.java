package com.bxz.brpc.client;

import com.bxz.brpc.proxy.ObjectProxy;
import com.bxz.brpc.registry.ServiceDiscovery;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 客户端
 * Created by baixiangzhu on 2017/7/3.
 */
public class RpcClient {


    private String serverAddress;

    private ServiceDiscovery serviceDiscovery;

    private static ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(16,16,600L,
                    TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(65536));

    public RpcClient(String serverAddress){
        this.serverAddress = serverAddress;
    }

    public RpcClient(ServiceDiscovery serviceDiscovery){
        this.serviceDiscovery = serviceDiscovery;
    }

    public static <T> T create(Class<T> interfaceClass){
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[] {interfaceClass},
                new ObjectProxy<T>(interfaceClass));
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }
}
