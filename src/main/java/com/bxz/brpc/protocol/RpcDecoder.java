package com.bxz.brpc.protocol;

import com.bxz.brpc.utils.SerializationUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 解码
 * Created by baixiangzhu on 2017/6/29.
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass){
        this.genericClass = genericClass;
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        //TODO:为什么小于4返回  4 为存放消息长度的int类型长度。
        //这个HEAD_LENGTH是我们用于表示头长度的字节数。  由于上面我们传的是一个int类型的值，所以这里HEAD_LENGTH的值为4.
        if(in.readableBytes() < 4){
            return;
        }

        //我们标记一下当前的readIndex的位置
        in.markReaderIndex();
        //读取传送过来的消息的长度。ByteBuf 的readInt()方法会让他的readIndex增加4
        int dataLength = in.readInt();
        //// 我们读到的消息体长度为0，这是不应该出现的情况，这里出现这情况，关闭连接。
        if(dataLength < 0){
            ctx.close();
        }

        //读到的消息体长度如果小于我们传送过来的消息长度，则resetReaderIndex. 这个配合markReaderIndex使用的。把readIndex重置到mark的地方
        if(in.readableBytes() < dataLength){
            in.resetReaderIndex();
            return;
        }

        //长度满足要求，读取出来
        byte [] body = new byte[dataLength];
        in.readBytes(body);

        //将byte[] 转换为序列化对象
        Object obj = SerializationUtil.deserialize(body, genericClass);

        out.add(obj);
    }
}
