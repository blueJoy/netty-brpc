package com.bxz.brpc.registry;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * 服务注册
 * Created by baixiangzhu on 2017/6/29.
 */
@Slf4j
public class ServiceRegistry {


    private CountDownLatch latch = new CountDownLatch(1);

    //注册的地址
    private String registryAddress;


    public ServiceRegistry(String registryAddress){
        this.registryAddress = registryAddress;
        ServiceDiscovery.notifyRegistry(registryAddress);
    }

    /**
     * 注册服务
     * @param registryAddress
     */
    public void registry(String registryAddress){
        this.registryAddress = registryAddress;
        ServiceDiscovery.notifyRegistry(registryAddress);
    }




    public String getRegistryAddress() {
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
        ServiceDiscovery.notifyRegistry(registryAddress);
    }
}
