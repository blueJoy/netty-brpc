package com.bxz.brpc.server;

import com.bxz.brpc.handler.RpcHandler;
import com.bxz.brpc.protocol.RpcDecoder;
import com.bxz.brpc.protocol.RpcEncoder;
import com.bxz.brpc.protocol.RpcRequest;
import com.bxz.brpc.protocol.RpcResponse;
import com.bxz.brpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by baixiangzhu on 2017/6/29.
 */
@Slf4j
public class RpcServer implements ApplicationContextAware,InitializingBean{

    //缓存   beanName -- Bean实例
    private Map<String,Object> beanRegistryMap = new HashMap<>();


    private String serviceAddress;

    private ServiceRegistry serviceRegistry;


    public RpcServer(String serviceAddress){
        this.serviceAddress = serviceAddress;
    }


    //Bean初始化执行。启动netty服务器
    @Override
    public void afterPropertiesSet() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try{

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup,workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,128)
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            socketChannel.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(65536,0,4,0,0))
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcHandler(beanRegistryMap));

                        }
                    });

            String[] arry = serviceAddress.split(":");

            String host = arry[0];
            int port = Integer.parseInt(arry[1]);

            //绑定端口地址
            ChannelFuture future = bootstrap.bind(host, port).sync();

            log.info("Server start host=[{}]，post=[{}]",host,port);

            if(serviceRegistry != null){
                serviceRegistry.registry(serviceAddress);
            }

            //监听关闭
            future.channel().closeFuture().sync();

        }finally {

            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }




    }

    //可以获取容器中所有的bean
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(RpcService.class);

        if(!CollectionUtils.isEmpty(beansWithAnnotation)){

            for (Object bean : beansWithAnnotation.values()){

                String interfaceName = bean.getClass().getAnnotation(RpcService.class).value().getName();

                beanRegistryMap.put(interfaceName,bean);
            }
        }
    }
}
