package com.bxz.brpc.protocol;

import com.bxz.brpc.utils.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 编码器
 * Created by baixiangzhu on 2017/6/29.
 */
public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> genericClass;

    public RpcEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {

        if(genericClass.isInstance(in)){
            byte[] data = SerializationUtil.serialize(in);

            //先写数据的长度
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}
