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
