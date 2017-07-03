package com.bxz.brpc.sync;

/**
 * Created by baixiangzhu on 2017/7/3.
 */
public interface AsyncRpcCallback {

    void success(Object result);

    void fail(Exception e);
}
