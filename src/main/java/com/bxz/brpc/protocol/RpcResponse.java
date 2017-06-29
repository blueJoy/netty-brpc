package com.bxz.brpc.protocol;

import lombok.Data;

/**
 * Created by baixiangzhu on 2017/6/29.
 */
@Data
public class RpcResponse {

    /**
     * 请求ID
     */
    private String requestId;
    /**
     * 错误信息
     */
    private String error;
    /**
     * 返回结果
     */
    private Object result;

}
