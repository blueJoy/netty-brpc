package com.bxz.brpc.proxy;

import com.bxz.brpc.client.RpcClient;
import com.bxz.brpc.protocol.RpcRequest;
import com.bxz.brpc.protocol.RpcResponse;
import com.bxz.brpc.sync.AsyncRpcCallback;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by baixiangzhu on 2017/7/3.
 */
@Slf4j
public class RpcFuture implements Future<Object> {


    //同步器
    private Sync sync;

    private RpcRequest request;
    private RpcResponse response;

    private long startTime;
    //返回时间阈值
    private long responseTimeThreadhold = 5000L;

    private List<AsyncRpcCallback> pendingCallbacks = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    public RpcFuture(RpcRequest request){
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
       sync.acquire(-1);
       if(this.response != null){
           return this.response.getResult();
       }else {
           return null;
       }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if(success){
            if(this.response != null){
                return  this.response.getResult();
            }else {
                return null;
            }
        }else {
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getClassName()
                    + ". Request method: " + this.request.getMethodName());
        }

    }
    
    public void done(RpcResponse response){
        this.response = response;
        sync.release(1);
        invokeCallbacks();
        long responseTime = System.currentTimeMillis()-startTime;
        
        if(responseTime > this.responseTimeThreadhold){
            log.warn("Service response time is too slow. Request id = " + response.getRequestId() + ". Response Time = " + responseTime + "ms");
        }
    }

    private void invokeCallbacks() {
        lock.lock();
        try{
            for (final AsyncRpcCallback callback : pendingCallbacks){
                runCallback(callback);
            }
        }finally {
            lock.unlock();
        }
    }

    public RpcFuture addCallback(AsyncRpcCallback callback){
        lock.lock();
        try{
            if(isDone()){
                runCallback(callback);
            }else {
                this.pendingCallbacks.add(callback);
            }
        }finally {
            lock.unlock();
        }
        return this;
    }

    private void runCallback(final AsyncRpcCallback callback) {

        final RpcResponse res = this.response;
        RpcClient.submit(new Runnable(){

            @Override
            public void run() {
                if(!res.isError()){
                    callback.success(res.getResult());
                }else {
                    callback.fail(new RuntimeException("Response error", new Throwable(res.getError())));
                }
            }
        });

    }


    static class Sync extends AbstractQueuedSynchronizer{


        //可以获取锁
        private final int done = 1;
        //可以释放锁
        private final int pending = 0;


        //排它的获取这个状态。这个方法的实现需要查询当前状态是否允许获取，然后再进行获取（使用compareAndSetState来做）状态。
        @Override
        protected boolean tryAcquire(int acquires){
            return super.getState() == done ? true : false;
        }

        //释放状态
        @Override
        protected boolean tryRelease(int arg) {
            if(super.getState() == pending){
                if(super.compareAndSetState(pending,done)){
                    return true;
                }
            }

            return false;
        }

        public boolean isDone(){
            getState();
            return getState() == done;
        }
    }
}
