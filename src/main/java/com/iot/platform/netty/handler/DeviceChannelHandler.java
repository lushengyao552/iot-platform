package com.iot.platform.netty.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.config.RabbitMQConfig;
import com.iot.platform.device.service.DeviceService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class DeviceChannelHandler extends SimpleChannelInboundHandler<String> {

    private final DeviceService deviceService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CHANNEL_KEY = "device:channel:";

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("设备连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(msg, Map.class);
            String deviceName = (String) data.get("deviceName");
            String ip = ctx.channel().remoteAddress().toString();

            if (deviceName != null) {
                deviceService.online(deviceName, ip);
                redisTemplate.opsForValue().set(CHANNEL_KEY + deviceName, ctx.channel().id().asLongText(),
                        90, TimeUnit.SECONDS);

                Map<String, Object> amqpMsg = new HashMap<>();
                amqpMsg.put("deviceName", deviceName);
                amqpMsg.put("productKey", data.get("productKey"));
                amqpMsg.put("metrics", data.get("metrics"));
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, amqpMsg);

                ctx.writeAndFlush("{\"code\":200,\"msg\":\"OK\"}");
            }
        } catch (Exception e) {
            log.error("处理设备消息失败: {}", e.getMessage());
            ctx.writeAndFlush("{\"code\":500,\"msg\":\"BAD_DATA\"}");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("设备断开: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("通道异常", cause);
        ctx.close();
    }
}
