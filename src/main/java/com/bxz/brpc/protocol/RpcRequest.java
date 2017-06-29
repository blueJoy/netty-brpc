package com.bxz.brpc.protocol;

import lombok.Data;

/**
 * Created by baixiangzhu on 2017/6/29.
 */
@Data
public class RpcRequest {

    /**
     * 请求ID
     * */
    private String requestId;
    /**
     * 类名
     * */
    private String className;
    /**
     * 方法名
     * */
    private String methodName;
    /**
     * 方法参数类型
     * */
    private Class<?> [] parameterTypes;
    /**
     * 方法参数
     **/
    private Object[] paramters;

}
