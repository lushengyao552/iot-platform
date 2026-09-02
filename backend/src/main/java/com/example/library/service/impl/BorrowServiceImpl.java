package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.dto.NotificationMessage;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.User;
import com.example.library.mapper.BookMapper;
import com.example.library.mapper.BorrowRecordMapper;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.UserService;
import com.example.library.util.MessageProducer;
import com.example.library.util.RedisService;
import com.example.library.vo.BorrowRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 借阅服务实现类
 *
 * <p>核心业务：
 * <ul>
 *   <li>借阅：校验 → 扣减库存（原子） → 创建记录（事务）</li>
 *   <li>归还：校验 → 计算逾期罚款 → 增加库存（原子） → 更新记录（事务）</li>
 *   <li>查询：分页 + 状态筛选 + 关联信息补充</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowService {

    private final BookService bookService;
    private final UserService userService;
    private final BookMapper bookMapper;
    private final MessageProducer messageProducer;
    private final RedisService redisService;

    @Value("${library.borrow.max-days}")
    private int maxBorrowDays;

    @Value("${library.borrow.max-count}")
    private int maxBorrowCount;

    /** 每日逾期罚款金额（元） */
    private static final BigDecimal DAILY_FINE = new BigDecimal("0.50");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecordVO borrowBook(Long userId, Long bookId) {
        // ========== 分布式锁：防止同一用户并发重复借阅同一本书 ==========
        String lockKey = "library:lock:borrow:" + userId + ":" + bookId;
        // 锁的 value 使用唯一标识，防止误删别人的锁
        String lockValue = java.util.UUID.randomUUID().toString();
        // 尝试获取锁，过期时间 10 秒（防止死锁）
        boolean locked = redisService.tryLock(lockKey, lockValue, 10, java.util.concurrent.TimeUnit.SECONDS);
        if (!locked) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "操作太频繁，请稍后再试");
        }

        try {
            return doBorrowBook(userId, bookId);
        } finally {
            // 释放锁（使用 Lua 脚本保证原子性，只释放自己的锁）
            redisService.unlock(lockKey, lockValue);
        }
    }

    /**
     * 借阅业务逻辑（在分布式锁保护下执行）
     */
    private BorrowRecordVO doBorrowBook(Long userId, Long bookId) {
        // 1. 校验用户是否存在
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 2. 校验图书是否存在且有库存
        Book book = bookService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        if (book.getStock() == null || book.getStock() <= 0) {
            throw new BusinessException(ResultCode.BOOK_OUT_OF_STOCK);
        }

        // 3. 校验用户是否已借阅此书（未归还）
        int existingCount = baseMapper.countByUserAndBook(userId, bookId);
        if (existingCount > 0) {
            throw new BusinessException(ResultCode.BOOK_ALREADY_BORROWED);
        }

        // 4. 校验用户借阅数量是否达上限
        int borrowingCount = baseMapper.countBorrowingByUserId(userId);
        if (borrowingCount >= maxBorrowCount) {
            throw new BusinessException(ResultCode.BORROW_LIMIT_EXCEEDED);
        }

        // 5. 原子扣减库存（防止并发超卖）
        int affectedRows = bookMapper.decreaseStock(bookId, 1);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.BOOK_OUT_OF_STOCK);
        }

        // 6. 创建借阅记录
        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(today);
        record.setDueDate(today.plusDays(maxBorrowDays));
        record.setStatus("BORROWED");
        record.setFine(BigDecimal.ZERO);
        save(record);

        log.info("借阅成功: userId={}, bookId={}, recordId={}, dueDate={}",
                userId, bookId, record.getId(), record.getDueDate());

        // 7. 异步发送借阅成功通知（通过 RabbitMQ，不影响主业务响应）
        sendBorrowNotification(user, book, record);

        // 8. 发送延迟到期提醒消息（60秒后触发，实际项目应设置为到期前1天）
        sendDelayReminder(user, book, record);

        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecordVO returnBook(Long recordId) {
        // 1. 校验借阅记录是否存在
        BorrowRecord record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_FOUND);
        }

        // 2. 校验是否已归还
        if ("RETURNED".equals(record.getStatus())) {
            throw new BusinessException(ResultCode.BORROW_ALREADY_RETURNED);
        }

        // 3. 计算逾期罚款
        LocalDate today = LocalDate.now();
        BigDecimal fine = BigDecimal.ZERO;
        if (today.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
            fine = DAILY_FINE.multiply(BigDecimal.valueOf(overdueDays));
            log.info("检测到逾期: recordId={}, overdueDays={}, fine={}", recordId, overdueDays, fine);
        }

        // 4. 原子增加库存
        bookMapper.increaseStock(record.getBookId(), 1);

        // 5. 更新借阅记录
        record.setReturnDate(today);
        record.setStatus("RETURNED");
        record.setFine(fine);
        updateById(record);

        log.info("归还成功: recordId={}, bookId={}, fine={}", recordId, record.getBookId(), fine);

        // 6. 异步发送归还成功通知
        User user = userService.getById(record.getUserId());
        Book book = bookService.getById(record.getBookId());
        sendReturnNotification(user, book, record, fine);

        return toVO(record);
    }

    @Override
    public IPage<BorrowRecordVO> pageUserBorrowRecords(Long userId, String status,
                                                         Integer pageNum, Integer pageSize) {
        Page<BorrowRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getUserId, userId);

        if (StringUtils.hasText(status)) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        wrapper.orderByDesc(BorrowRecord::getCreateTime);

        IPage<BorrowRecord> recordPage = page(page, wrapper);
        return recordPage.convert(this::toVO);
    }

    @Override
    public IPage<BorrowRecordVO> pageAllBorrowRecords(String status, Integer pageNum, Integer pageSize) {
        Page<BorrowRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        wrapper.orderByDesc(BorrowRecord::getCreateTime);

        IPage<BorrowRecord> recordPage = page(page, wrapper);
        return recordPage.convert(this::toVO);
    }

    @Override
    public BorrowRecordVO toVO(BorrowRecord record) {
        if (record == null) {
            return null;
        }

        BorrowRecordVO vo = new BorrowRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setBookId(record.getBookId());
        vo.setBorrowDate(record.getBorrowDate());
        vo.setDueDate(record.getDueDate());
        vo.setReturnDate(record.getReturnDate());
        vo.setStatus(record.getStatus());
        vo.setFine(record.getFine());
        vo.setCreateTime(record.getCreateTime());

        // 补充用户信息
        User user = userService.getById(record.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
        }

        // 补充图书信息
        Book book = bookService.getById(record.getBookId());
        if (book != null) {
            vo.setBookTitle(book.getTitle());
            vo.setBookAuthor(book.getAuthor());
        }

        // 计算是否逾期及剩余天数
        LocalDate today = LocalDate.now();
        if ("BORROWED".equals(record.getStatus())) {
            long remaining = ChronoUnit.DAYS.between(today, record.getDueDate());
            vo.setRemainingDays((int) remaining);
            vo.setOverdue(remaining < 0);
        } else if ("RETURNED".equals(record.getStatus())) {
            vo.setOverdue(record.getFine() != null && record.getFine().compareTo(BigDecimal.ZERO) > 0);
            vo.setRemainingDays(0);
        } else {
            vo.setOverdue(true);
            vo.setRemainingDays(0);
        }

        return vo;
    }

    // ============================================================
    // 私有方法：发送 MQ 消息
    // ============================================================

    /**
     * 发送借阅成功通知
     */
    private void sendBorrowNotification(User user, Book book, BorrowRecord record) {
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .operationType("BORROW")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .bookId(book.getId())
                    .bookTitle(book.getTitle())
                    .borrowRecordId(record.getId())
                    .dueDate(record.getDueDate().toString())
                    .content(String.format("您已成功借阅《%s》，请在 %s 前归还",
                            book.getTitle(), record.getDueDate()))
                    .sendTime(java.time.LocalDateTime.now())
                    .build();
            messageProducer.sendBorrowSuccessMessage(message);
        } catch (Exception e) {
            // 消息发送失败不影响主业务，只记录日志
            log.error("发送借阅通知失败, recordId={}", record.getId(), e);
        }
    }

    /**
     * 发送归还成功通知
     */
    private void sendReturnNotification(User user, Book book, BorrowRecord record, BigDecimal fine) {
        try {
            String content = String.format("您已成功归还《%s》", book.getTitle());
            if (fine != null && fine.compareTo(BigDecimal.ZERO) > 0) {
                content += String.format("，逾期罚款 %.2f 元", fine);
            }

            NotificationMessage message = NotificationMessage.builder()
                    .operationType("RETURN")
                    .userId(user != null ? user.getId() : null)
                    .username(user != null ? user.getUsername() : null)
                    .bookId(book != null ? book.getId() : null)
                    .bookTitle(book != null ? book.getTitle() : null)
                    .borrowRecordId(record.getId())
                    .content(content)
                    .sendTime(java.time.LocalDateTime.now())
                    .build();
            messageProducer.sendReturnSuccessMessage(message);
        } catch (Exception e) {
            log.error("发送归还通知失败, recordId={}", record.getId(), e);
        }
    }

    /**
     * 发送延迟到期提醒
     *
     * <p>消息发送到延迟队列，等待 TTL 到期后自动转发到死信队列，
     * 消费者从死信队列消费消息，触发到期提醒。
     *
     * <p>演示用 TTL 为 60 秒，实际项目中应根据到期时间动态计算延迟时长。
     */
    private void sendDelayReminder(User user, Book book, BorrowRecord record) {
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .operationType("DELAY_REMIND")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .bookId(book.getId())
                    .bookTitle(book.getTitle())
                    .borrowRecordId(record.getId())
                    .dueDate(record.getDueDate().toString())
                    .content(String.format("提醒：您借阅的《%s》即将到期（应还日期：%s），请及时归还",
                            book.getTitle(), record.getDueDate()))
                    .sendTime(java.time.LocalDateTime.now())
                    .build();
            messageProducer.sendDelayBorrowReminder(message);
        } catch (Exception e) {
            log.error("发送延迟到期提醒失败, recordId={}", record.getId(), e);
        }
    }
}
