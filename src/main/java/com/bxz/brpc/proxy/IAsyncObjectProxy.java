package com.bxz.brpc.proxy;

/**
 * Created by baixiangzhu on 2017/7/3.
 */
public interface IAsyncObjectProxy {

    RpcFuture call(String funcName,Object... args);

}
