package com.bxz.brpc.utils;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化工具包
 * Created by baixiangzhu on 2017/6/29.
 */
public class SerializationUtil {

    private static Map<Class<?>,Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    //java 创建对象类库
    private static Objenesis objenesis = new ObjenesisStd(true);

    /**
     * 反序列化  byte[] --> 对象
     * @param body
     * @param genericClass
     * @param <T>
     * @return
     */
    public static <T> T deserialize(byte[] body, Class<T> genericClass) {

        try {

            T message = objenesis.newInstance(genericClass);
            Schema<T> schema = getSchema(genericClass);

            ProtobufIOUtil.mergeFrom(body,message,schema);
            return message;

        }catch (Exception e) {

            throw new IllegalStateException(e);
        }
    }

    private static <T> Schema<T> getSchema(Class<T> genericClass) {

        Schema<T> schema = (Schema<T>) cachedSchema.get(genericClass);
        if(schema == null){
            schema = RuntimeSchema.getSchema(genericClass);
            if(schema != null) {
                cachedSchema.put(genericClass,schema);
            }
        }
        return schema;
    }


    /**
     * 序列化  对象 --》 byte[] 数组
     * @param obj
     * @return
     */
    public static <T> byte[] serialize(T obj) {

        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try{

            Schema<T> schema = getSchema(cls);
            return ProtobufIOUtil.toByteArray(obj,schema,buffer);
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(),e);
        }finally {
            buffer.clear();
        }

    }
}
