package com.example.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通知微服务启动类
 *
 * <p>独立部署的微服务，通过 RabbitMQ 异步消费主应用发送的通知消息。
 *
 * <p>微服务架构优势：
 * <ul>
 *   <li>解耦：通知服务与主业务服务独立，互不影响</li>
 *   <li>独立扩展：通知量大时可单独扩容通知服务实例</li>
 *   <li>独立部署：通知服务升级不影响主业务</li>
 *   <li>技术异构：通知服务可使用不同技术栈</li>
 * </ul>
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        System.out.println("\n" +
                "  _   _       _   _  __           _   _                 \n" +
                " | \\ | | ___ | |_(_)/ _| ___ __ _| |_(_) ___  _ __    \n" +
                " |  \\| |/ _ \\| __| | |_ / __/ _` | __| |/ _ \\| '_ \\   \n" +
                " | |\\  | (_) | |_| |  _| (_| (_| | |_| | (_) | | | |  \n" +
                " |_| \\_|\\___/ \\__|_|_|  \\___\\__,_|\\__|_|\\___/|_| |_|  \n" +
                "                                                          \n" +
                "  通知微服务启动成功！\n" +
                "  正在监听 RabbitMQ 消息队列...\n" +
                "  管理接口: http://localhost:8081/notifications\n");
    }
}
