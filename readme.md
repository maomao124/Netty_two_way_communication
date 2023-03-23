



# 双向通信

## 需求

客户端向服务器端发送一段字符串，服务器端立马响应 hello! +发送的字符串



## 服务端

```java
package mao;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Project name(项目名称)：Netty_双向通信
 * Package(包名): mao
 * Class(类名): Server
 * Author(作者）: mao
 * Author QQ：1296193245
 * GitHub：https://github.com/maomao124/
 * Date(创建日期)： 2023/3/23
 * Time(创建时间)： 13:51
 * Version(版本): 1.0
 * Description(描述)： 服务端
 */

@Slf4j
public class Server
{
    @SneakyThrows
    public static void main(String[] args)
    {
        new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup(Runtime.getRuntime().availableProcessors()))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>()
                {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception
                    {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter()
                        {
                            /**
                             * 连接建立
                             *
                             * @param ctx ctx
                             * @throws Exception 异常
                             */
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception
                            {
                                log.info("连接建立：" + ctx.channel().toString());
                                super.channelActive(ctx);
                            }

                            /**
                             * 通道读
                             *
                             * @param ctx ctx
                             * @param msg 消息
                             * @throws Exception 异常
                             */
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
                            {
                                ByteBuf buffer = (ByteBuf) msg;
                                String s = buffer.toString(StandardCharsets.UTF_8);
                                log.debug(s);
                                String respMsg = "hello! " + s;
                                //buffer.release();
                                //建议使用ctx.alloc()创建ByteBuf
                                buffer = ctx.alloc().buffer();
                                buffer.writeCharSequence(respMsg, StandardCharsets.UTF_8);
                                ctx.writeAndFlush(buffer);
                                super.channelRead(ctx, msg);
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception
                            {
                                log.info("连接关闭：" + ctx.channel().toString());
                                super.channelInactive(ctx);
                            }
                        });
                    }
                })
                .bind(8080)
                .sync();
        log.info("服务启动成功");
    }
}

```





## 客户端

```java
package mao;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Project name(项目名称)：Netty_双向通信
 * Package(包名): mao
 * Class(类名): Client
 * Author(作者）: mao
 * Author QQ：1296193245
 * GitHub：https://github.com/maomao124/
 * Date(创建日期)： 2023/3/23
 * Time(创建时间)： 14:02
 * Version(版本): 1.0
 * Description(描述)： 客户端
 */

@Slf4j
public class Client
{
    public static void main(String[] args)
    {
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>()
                {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception
                    {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter()
                        {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
                            {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                String respMsg = byteBuf.toString(StandardCharsets.UTF_8);
                                log.info("接收到来自服务端的响应信息：" + respMsg);
                                byteBuf.release();
                            }
                        });
                    }
                })
                .connect(new InetSocketAddress("127.0.0.1", 8080));
        Channel channel = channelFuture.channel();

        Scanner input = new Scanner(System.in);

        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    //System.out.print("请输入：");
                    String next = input.next();
                    if ("q".equals(next))
                    {
                        channel.close();
                    }
                    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(next.length());
                    buffer.writeCharSequence(next, StandardCharsets.UTF_8);
                    channel.writeAndFlush(buffer);
                    //buffer.release();
                }
            }
        }, "input");
        thread.setDaemon(true);

        channelFuture.addListener(new GenericFutureListener<Future<? super Void>>()
        {
            /**
             * 操作完成(客户端启动完成)
             *
             * @param future future
             * @throws Exception 异常
             */
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception
            {
                boolean success = future.isSuccess();
                if (!success)
                {
                    Throwable throwable = future.cause();
                    log.error("错误：" + throwable.getMessage());
                }
                else
                {
                    log.debug("服务器连接成功");
                    thread.start();
                }
            }
        });

        channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>()
        {
            /**
             * 操作完成(关闭客户端)
             *
             * @param future Future
             * @throws Exception 异常
             */
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception
            {
                log.info("关闭客户端");
                nioEventLoopGroup.shutdownGracefully();
            }
        });
    }
}

```





## 运行

启动一个服务端和3个客户端

随即发送数据

服务端

```sh
2023-03-23  14:41:24.717  [main] INFO  mao.Server:  服务启动成功
2023-03-23  14:41:29.905  [nioEventLoopGroup-3-1] INFO  mao.Server:  连接建立：[id: 0x42de88c6, L:/127.0.0.1:8080 - R:/127.0.0.1:64294]
2023-03-23  14:41:32.988  [nioEventLoopGroup-3-2] INFO  mao.Server:  连接建立：[id: 0x0d7f76af, L:/127.0.0.1:8080 - R:/127.0.0.1:64298]
2023-03-23  14:41:35.105  [nioEventLoopGroup-3-3] INFO  mao.Server:  连接建立：[id: 0x7393b555, L:/127.0.0.1:8080 - R:/127.0.0.1:64302]
2023-03-23  14:41:42.038  [nioEventLoopGroup-3-1] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.maxCapacityPerThread: 4096
2023-03-23  14:41:42.039  [nioEventLoopGroup-3-1] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.maxSharedCapacityFactor: 2
2023-03-23  14:41:42.039  [nioEventLoopGroup-3-1] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.linkCapacity: 16
2023-03-23  14:41:42.039  [nioEventLoopGroup-3-1] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.ratio: 8
2023-03-23  14:41:42.042  [nioEventLoopGroup-3-1] DEBUG io.netty.buffer.AbstractByteBuf:  -Dio.netty.buffer.checkAccessible: true
2023-03-23  14:41:42.042  [nioEventLoopGroup-3-1] DEBUG io.netty.buffer.AbstractByteBuf:  -Dio.netty.buffer.checkBounds: true
2023-03-23  14:41:42.042  [nioEventLoopGroup-3-1] DEBUG io.netty.util.ResourceLeakDetectorFactory:  Loaded default ResourceLeakDetector: io.netty.util.ResourceLeakDetector@2f39d977
2023-03-23  14:41:42.047  [nioEventLoopGroup-3-1] DEBUG mao.Server:  45326246
2023-03-23  14:41:42.048  [nioEventLoopGroup-3-1] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded inbound message PooledUnsafeDirectByteBuf(ridx: 0, widx: 8, cap: 1024) that reached at the tail of the pipeline. Please check your pipeline configuration.
2023-03-23  14:41:42.048  [nioEventLoopGroup-3-1] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded message pipeline : [Server$1$1#0, DefaultChannelPipeline$TailContext#0]. Channel : [id: 0x42de88c6, L:/127.0.0.1:8080 - R:/127.0.0.1:64294].
2023-03-23  14:41:46.462  [nioEventLoopGroup-3-1] DEBUG mao.Server:  34
2023-03-23  14:41:46.462  [nioEventLoopGroup-3-1] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded inbound message PooledUnsafeDirectByteBuf(ridx: 0, widx: 2, cap: 1024) that reached at the tail of the pipeline. Please check your pipeline configuration.
2023-03-23  14:41:46.463  [nioEventLoopGroup-3-1] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded message pipeline : [Server$1$1#0, DefaultChannelPipeline$TailContext#0]. Channel : [id: 0x42de88c6, L:/127.0.0.1:8080 - R:/127.0.0.1:64294].
2023-03-23  14:41:51.026  [nioEventLoopGroup-3-2] DEBUG mao.Server:  rtet
2023-03-23  14:41:51.026  [nioEventLoopGroup-3-2] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded inbound message PooledUnsafeDirectByteBuf(ridx: 0, widx: 4, cap: 1024) that reached at the tail of the pipeline. Please check your pipeline configuration.
2023-03-23  14:41:51.027  [nioEventLoopGroup-3-2] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded message pipeline : [Server$1$1#0, DefaultChannelPipeline$TailContext#0]. Channel : [id: 0x0d7f76af, L:/127.0.0.1:8080 - R:/127.0.0.1:64298].
2023-03-23  14:41:59.197  [nioEventLoopGroup-3-3] DEBUG mao.Server:  你好
2023-03-23  14:41:59.198  [nioEventLoopGroup-3-3] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded inbound message PooledUnsafeDirectByteBuf(ridx: 0, widx: 6, cap: 1024) that reached at the tail of the pipeline. Please check your pipeline configuration.
2023-03-23  14:41:59.198  [nioEventLoopGroup-3-3] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded message pipeline : [Server$1$1#0, DefaultChannelPipeline$TailContext#0]. Channel : [id: 0x7393b555, L:/127.0.0.1:8080 - R:/127.0.0.1:64302].
2023-03-23  14:42:06.732  [nioEventLoopGroup-3-3] DEBUG mao.Server:  sfaf
2023-03-23  14:42:06.732  [nioEventLoopGroup-3-3] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded inbound message PooledUnsafeDirectByteBuf(ridx: 0, widx: 4, cap: 1024) that reached at the tail of the pipeline. Please check your pipeline configuration.
2023-03-23  14:42:06.732  [nioEventLoopGroup-3-3] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded message pipeline : [Server$1$1#0, DefaultChannelPipeline$TailContext#0]. Channel : [id: 0x7393b555, L:/127.0.0.1:8080 - R:/127.0.0.1:64302].
2023-03-23  14:42:10.048  [nioEventLoopGroup-3-1] DEBUG mao.Server:  asf
2023-03-23  14:42:10.049  [nioEventLoopGroup-3-1] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded inbound message PooledUnsafeDirectByteBuf(ridx: 0, widx: 3, cap: 512) that reached at the tail of the pipeline. Please check your pipeline configuration.
2023-03-23  14:42:10.049  [nioEventLoopGroup-3-1] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded message pipeline : [Server$1$1#0, DefaultChannelPipeline$TailContext#0]. Channel : [id: 0x42de88c6, L:/127.0.0.1:8080 - R:/127.0.0.1:64294].
2023-03-23  14:42:14.680  [nioEventLoopGroup-3-2] DEBUG mao.Server:  asfgs
2023-03-23  14:42:14.681  [nioEventLoopGroup-3-2] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded inbound message PooledUnsafeDirectByteBuf(ridx: 0, widx: 5, cap: 1024) that reached at the tail of the pipeline. Please check your pipeline configuration.
2023-03-23  14:42:14.681  [nioEventLoopGroup-3-2] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded message pipeline : [Server$1$1#0, DefaultChannelPipeline$TailContext#0]. Channel : [id: 0x0d7f76af, L:/127.0.0.1:8080 - R:/127.0.0.1:64298].
2023-03-23  14:42:31.074  [nioEventLoopGroup-3-2] DEBUG mao.Server:  再见
2023-03-23  14:42:31.074  [nioEventLoopGroup-3-2] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded inbound message PooledUnsafeDirectByteBuf(ridx: 0, widx: 6, cap: 512) that reached at the tail of the pipeline. Please check your pipeline configuration.
2023-03-23  14:42:31.074  [nioEventLoopGroup-3-2] DEBUG io.netty.channel.DefaultChannelPipeline:  Discarded message pipeline : [Server$1$1#0, DefaultChannelPipeline$TailContext#0]. Channel : [id: 0x0d7f76af, L:/127.0.0.1:8080 - R:/127.0.0.1:64298].
2023-03-23  14:42:38.723  [nioEventLoopGroup-3-2] INFO  mao.Server:  连接关闭：[id: 0x0d7f76af, L:/127.0.0.1:8080 ! R:/127.0.0.1:64298]
2023-03-23  14:42:42.986  [nioEventLoopGroup-3-1] INFO  mao.Server:  连接关闭：[id: 0x42de88c6, L:/127.0.0.1:8080 ! R:/127.0.0.1:64294]
2023-03-23  14:42:47.697  [nioEventLoopGroup-3-3] INFO  mao.Server:  连接关闭：[id: 0x7393b555, L:/127.0.0.1:8080 ! R:/127.0.0.1:64302]
```



客户端1

```sh
2023-03-23  14:41:29.898  [nioEventLoopGroup-2-1] DEBUG mao.Client:  服务器连接成功
45326246
2023-03-23  14:41:42.025  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.maxCapacityPerThread: 4096
2023-03-23  14:41:42.025  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.maxSharedCapacityFactor: 2
2023-03-23  14:41:42.025  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.linkCapacity: 16
2023-03-23  14:41:42.025  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.ratio: 8
2023-03-23  14:41:42.029  [input] DEBUG io.netty.buffer.AbstractByteBuf:  -Dio.netty.buffer.checkAccessible: true
2023-03-23  14:41:42.029  [input] DEBUG io.netty.buffer.AbstractByteBuf:  -Dio.netty.buffer.checkBounds: true
2023-03-23  14:41:42.030  [input] DEBUG io.netty.util.ResourceLeakDetectorFactory:  Loaded default ResourceLeakDetector: io.netty.util.ResourceLeakDetector@5e3df20
2023-03-23  14:41:42.051  [nioEventLoopGroup-2-1] INFO  mao.Client:  接收到来自服务端的响应信息：hello! 45326246
34
2023-03-23  14:41:46.463  [nioEventLoopGroup-2-1] INFO  mao.Client:  接收到来自服务端的响应信息：hello! 34
asf
2023-03-23  14:42:10.049  [nioEventLoopGroup-2-1] INFO  mao.Client:  接收到来自服务端的响应信息：hello! asf
q
2023-03-23  14:42:42.986  [nioEventLoopGroup-2-1] INFO  mao.Client:  关闭客户端
2023-03-23  14:42:45.218  [nioEventLoopGroup-2-1] DEBUG io.netty.buffer.PoolThreadCache:  Freed 2 thread-local buffer(s) from thread: nioEventLoopGroup-2-1
```

客户端2

```sh
2023-03-23  14:41:32.989  [nioEventLoopGroup-2-1] DEBUG mao.Client:  服务器连接成功
rtet
2023-03-23  14:41:51.014  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.maxCapacityPerThread: 4096
2023-03-23  14:41:51.014  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.maxSharedCapacityFactor: 2
2023-03-23  14:41:51.014  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.linkCapacity: 16
2023-03-23  14:41:51.014  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.ratio: 8
2023-03-23  14:41:51.017  [input] DEBUG io.netty.buffer.AbstractByteBuf:  -Dio.netty.buffer.checkAccessible: true
2023-03-23  14:41:51.017  [input] DEBUG io.netty.buffer.AbstractByteBuf:  -Dio.netty.buffer.checkBounds: true
2023-03-23  14:41:51.017  [input] DEBUG io.netty.util.ResourceLeakDetectorFactory:  Loaded default ResourceLeakDetector: io.netty.util.ResourceLeakDetector@5e3df20
2023-03-23  14:41:51.031  [nioEventLoopGroup-2-1] INFO  mao.Client:  接收到来自服务端的响应信息：hello! rtet
asfgs
2023-03-23  14:42:14.681  [nioEventLoopGroup-2-1] INFO  mao.Client:  接收到来自服务端的响应信息：hello! asfgs
再见
2023-03-23  14:42:31.074  [nioEventLoopGroup-2-1] INFO  mao.Client:  接收到来自服务端的响应信息：hello! 再见
q
2023-03-23  14:42:38.722  [nioEventLoopGroup-2-1] INFO  mao.Client:  关闭客户端
2023-03-23  14:42:40.962  [nioEventLoopGroup-2-1] DEBUG io.netty.buffer.PoolThreadCache:  Freed 2 thread-local buffer(s) from thread: nioEventLoopGroup-2-1
```

客户端3

```sh
2023-03-23  14:41:35.106  [nioEventLoopGroup-2-1] DEBUG mao.Client:  服务器连接成功
你好
2023-03-23  14:41:59.182  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.maxCapacityPerThread: 4096
2023-03-23  14:41:59.182  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.maxSharedCapacityFactor: 2
2023-03-23  14:41:59.182  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.linkCapacity: 16
2023-03-23  14:41:59.183  [input] DEBUG io.netty.util.Recycler:  -Dio.netty.recycler.ratio: 8
2023-03-23  14:41:59.187  [input] DEBUG io.netty.buffer.AbstractByteBuf:  -Dio.netty.buffer.checkAccessible: true
2023-03-23  14:41:59.187  [input] DEBUG io.netty.buffer.AbstractByteBuf:  -Dio.netty.buffer.checkBounds: true
2023-03-23  14:41:59.188  [input] DEBUG io.netty.util.ResourceLeakDetectorFactory:  Loaded default ResourceLeakDetector: io.netty.util.ResourceLeakDetector@5e3df20
2023-03-23  14:41:59.202  [nioEventLoopGroup-2-1] INFO  mao.Client:  接收到来自服务端的响应信息：hello! 你好
sfaf
2023-03-23  14:42:06.732  [nioEventLoopGroup-2-1] INFO  mao.Client:  接收到来自服务端的响应信息：hello! sfaf
q
2023-03-23  14:42:47.697  [nioEventLoopGroup-2-1] INFO  mao.Client:  关闭客户端
2023-03-23  14:42:49.944  [nioEventLoopGroup-2-1] DEBUG io.netty.buffer.PoolThreadCache:  Freed 1 thread-local buffer(s) from thread: nioEventLoopGroup-2-1
```





