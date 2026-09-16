package com.example.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import com.example.library.util.UserContext;
import com.example.library.vo.BorrowRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Value("${library.borrow.max-days:30}")
    private int maxBorrowDays;

    @Value("${library.borrow.max-count:5}")
    private int maxBorrowCount;

    private static final BigDecimal DAILY_FINE = new BigDecimal("0.5");

    @Transactional
    public void borrow(Long bookId) {
        Long userId = UserContext.getCurrentUserId();

        Book book = bookRepository.getById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }

        int borrowingCount = borrowRecordRepository.countBorrowingByUserId(userId);
        if (borrowingCount >= maxBorrowCount) {
            throw new BusinessException(ResultCode.BORROW_LIMIT_EXCEEDED);
        }

        int alreadyBorrowed = borrowRecordRepository.countByUserAndBook(userId, bookId);
        if (alreadyBorrowed > 0) {
            throw new BusinessException(ResultCode.BOOK_ALREADY_BORROWED);
        }

        int rows = bookRepository.decreaseStock(bookId, 1);
        if (rows == 0) {
            throw new BusinessException(ResultCode.BOOK_OUT_OF_STOCK);
        }

        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(today);
        record.setDueDate(today.plusDays(maxBorrowDays));
        record.setStatus("BORROWED");
        record.setFine(BigDecimal.ZERO);
        borrowRecordRepository.save(record);
    }

    @Transactional
    public void returnBook(Long recordId) {
        BorrowRecord record = borrowRecordRepository.getById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.BORROW_RECORD_NOT_FOUND);
        }
        if ("RETURNED".equals(record.getStatus())) {
            throw new BusinessException(ResultCode.BORROW_ALREADY_RETURNED);
        }

        LocalDate today = LocalDate.now();
        record.setReturnDate(today);
        record.setStatus("RETURNED");

        if (today.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
            record.setFine(DAILY_FINE.multiply(BigDecimal.valueOf(overdueDays)));
        }

        borrowRecordRepository.updateById(record);
        bookRepository.increaseStock(record.getBookId(), 1);
    }

    public Map<String, Object> myRecords(int pageNum, int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        Page<BorrowRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getUserId, userId)
                .orderByDesc(BorrowRecord::getCreateTime);
        borrowRecordRepository.page(page, wrapper);

        List<BorrowRecordVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return result;
    }

    public Map<String, Object> allRecords(int pageNum, int pageSize, String status) {
        Page<BorrowRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        wrapper.orderByDesc(BorrowRecord::getCreateTime);
        borrowRecordRepository.page(page, wrapper);

        List<BorrowRecordVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return result;
    }

    private BorrowRecordVO toVO(BorrowRecord record) {
        Book book = bookRepository.getById(record.getBookId());
        User user = userRepository.getById(record.getUserId());

        LocalDate today = LocalDate.now();
        boolean overdue = "BORROWED".equals(record.getStatus()) && today.isAfter(record.getDueDate());
        long remainingDays = record.getDueDate() != null
                ? ChronoUnit.DAYS.between(today, record.getDueDate()) : 0;

        return BorrowRecordVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .username(user != null ? user.getUsername() : null)
                .nickname(user != null ? user.getNickname() : null)
                .bookId(record.getBookId())
                .bookTitle(book != null ? book.getTitle() : null)
                .bookAuthor(book != null ? book.getAuthor() : null)
                .borrowDate(record.getBorrowDate())
                .dueDate(record.getDueDate())
                .returnDate(record.getReturnDate())
                .status(record.getStatus())
                .fine(record.getFine())
                .overdue(overdue)
                .remainingDays((int) remainingDays)
                .createTime(record.getCreateTime())
                .build();
    }
}
