package com.bxz.brpc.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 服务发现
 * Created by baixiangzhu on 2017/6/29.
 */
@Slf4j
public class ServiceDiscovery {

    private CountDownLatch latch = new CountDownLatch(1);

    //存放注册的节点
    public static volatile List<String> nodeList = new ArrayList<>();

    public String discovery(){

        if(!CollectionUtils.isEmpty(nodeList)){

            return nodeList.get(0);
        }

        return null;
    }

    public static void notifyRegistry(String node){
        nodeList.add(node);
    }

}
