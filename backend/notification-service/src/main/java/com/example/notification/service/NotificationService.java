package com.example.notification.service;

import com.example.notification.model.NotificationRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 通知服务
 *
 * <p>演示用内存存储，实际项目中应存储到数据库。
 * 负责通知记录的保存、查询、幂等性校验。
 */
@Slf4j
@Service
public class NotificationService {

    /** 内存存储（演示用） */
    private final Map<Long, NotificationRecord> storage = new ConcurrentHashMap<>();

    /** 已处理的消息 ID 集合（用于幂等性校验，防止重复消费） */
    private final Map<String, Long> processedMessageIds = new ConcurrentHashMap<>();

    /** ID 生成器 */
    private final AtomicLong idGenerator = new AtomicLong(0);

    /**
     * 保存通知记录
     *
     * @param record 通知记录
     * @return 保存后的记录（含 ID）
     */
    public NotificationRecord save(NotificationRecord record) {
        // 幂等性校验：如果消息 ID 已处理过，直接返回已有记录
        if (record.getMessageId() != null && processedMessageIds.containsKey(record.getMessageId())) {
            Long existingId = processedMessageIds.get(record.getMessageId());
            log.info("消息已处理，跳过重复消费, messageId={}, notificationId={}",
                    record.getMessageId(), existingId);
            return storage.get(existingId);
        }

        // 生成 ID 并保存
        long id = idGenerator.incrementAndGet();
        record.setId(id);
        storage.put(id, record);

        // 记录已处理的消息 ID
        if (record.getMessageId() != null) {
            processedMessageIds.put(record.getMessageId(), id);
        }

        log.info("通知记录已保存, id={}, messageId={}, type={}", id, record.getMessageId(), record.getOperationType());
        return record;
    }

    /**
     * 查询所有通知记录
     */
    public List<NotificationRecord> findAll() {
        return storage.values().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 根据用户 ID 查询通知记录
     */
    public List<NotificationRecord> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(r -> userId.equals(r.getUserId()))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 查询
     */
    public NotificationRecord findById(Long id) {
        return storage.get(id);
    }

    /**
     * 获取通知总数
     */
    public long count() {
        return storage.size();
    }
}
