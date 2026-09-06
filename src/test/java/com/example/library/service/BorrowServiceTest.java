package com.example.library.service;

import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.User;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.service.impl.BorrowServiceImpl;
import com.example.library.util.MessageProducer;
import com.example.library.util.RedisService;
import com.example.library.vo.BorrowRecordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 借阅服务单元测试
 *
 * <p>测试借阅、归还等核心业务逻辑，包含库存原子操作、逾期罚款计算等场景。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("借阅服务单元测试")
class BorrowServiceTest {

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @Mock
    private RedisService redisService;

    @Mock
    private MessageProducer messageProducer;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private User testUser;
    private Book testBook;
    private BorrowRecord testRecord;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setNickname("测试用户");
        testUser.setStatus(1);

        // 初始化测试图书
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Java核心技术");
        testBook.setAuthor("Cay S. Horstmann");
        testBook.setStock(5);

        // 初始化借阅记录
        testRecord = new BorrowRecord();
        testRecord.setId(1L);
        testRecord.setUserId(1L);
        testRecord.setBookId(1L);
        testRecord.setBorrowDate(LocalDate.now());
        testRecord.setDueDate(LocalDate.now().plusDays(30));
        testRecord.setStatus("BORROWED");
        testRecord.setFine(BigDecimal.ZERO);

        // 注入配置字段
        ReflectionTestUtils.setField(borrowService, "maxBorrowDays", 30);
        ReflectionTestUtils.setField(borrowService, "maxBorrowCount", 5);

        // mock 分布式锁：默认获取锁成功（避免 borrowBook 中 redisService 未 mock 导致 NPE）
        lenient().when(redisService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
    }

    @Test
    @DisplayName("借阅成功 - 正常借阅流程")
    void borrowBook_Success() {
        // Given
        when(userService.getById(1L)).thenReturn(testUser);
        when(bookService.getById(1L)).thenReturn(testBook);
        when(borrowRecordRepository.countByUserAndBook(1L, 1L)).thenReturn(0);
        when(borrowRecordRepository.countBorrowingByUserId(1L)).thenReturn(2);

        when(bookService.decreaseStock(1L, 1)).thenReturn(1);

        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            record.setId(100L);
            return true;
        });

        // When
        BorrowRecordVO result = borrowService.borrowBook(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getBookId());
        assertEquals("BORROWED", result.getStatus());
        assertEquals(LocalDate.now(), result.getBorrowDate());
        assertEquals(LocalDate.now().plusDays(30), result.getDueDate());
        assertEquals(0, result.getFine().compareTo(BigDecimal.ZERO));
        assertFalse(result.getOverdue());
        assertTrue(result.getRemainingDays() >= 29);

        verify(bookService, times(1)).decreaseStock(1L, 1);
        verify(borrowRecordRepository, times(1)).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("借阅失败 - 用户不存在")
    void borrowBook_UserNotFound() {
        // Given
        when(userService.getById(99L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.borrowBook(99L, 1L));
        assertEquals(ResultCode.USERNAME_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("借阅失败 - 图书不存在")
    void borrowBook_BookNotFound() {
        // Given
        when(userService.getById(1L)).thenReturn(testUser);
        when(bookService.getById(99L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.borrowBook(1L, 99L));
        assertEquals(ResultCode.BOOK_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("借阅失败 - 库存不足")
    void borrowBook_OutOfStock() {
        // Given
        testBook.setStock(0);
        when(userService.getById(1L)).thenReturn(testUser);
        when(bookService.getById(1L)).thenReturn(testBook);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.borrowBook(1L, 1L));
        assertEquals(ResultCode.BOOK_OUT_OF_STOCK.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("借阅失败 - 已借阅此书未归还")
    void borrowBook_AlreadyBorrowed() {
        // Given
        when(userService.getById(1L)).thenReturn(testUser);
        when(bookService.getById(1L)).thenReturn(testBook);
        when(borrowRecordRepository.countByUserAndBook(1L, 1L)).thenReturn(1);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.borrowBook(1L, 1L));
        assertEquals(ResultCode.BOOK_ALREADY_BORROWED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("借阅失败 - 达到借阅上限")
    void borrowBook_LimitExceeded() {
        // Given
        when(userService.getById(1L)).thenReturn(testUser);
        when(bookService.getById(1L)).thenReturn(testBook);
        when(borrowRecordRepository.countByUserAndBook(1L, 1L)).thenReturn(0);
        when(borrowRecordRepository.countBorrowingByUserId(1L)).thenReturn(5);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.borrowBook(1L, 1L));
        assertEquals(ResultCode.BORROW_LIMIT_EXCEEDED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("归还成功 - 正常归还（无逾期）")
    void returnBook_Success_NoOverdue() {
        // Given
        when(borrowRecordRepository.getById(1L)).thenReturn(testRecord);

        when(bookService.increaseStock(1L, 1)).thenReturn(1);

        when(borrowRecordRepository.updateById(any(BorrowRecord.class))).thenReturn(true);
        when(userService.getById(1L)).thenReturn(testUser);
        when(bookService.getById(1L)).thenReturn(testBook);

        // When
        BorrowRecordVO result = borrowService.returnBook(1L);

        // Then
        assertNotNull(result);
        assertEquals("RETURNED", result.getStatus());
        assertEquals(LocalDate.now(), result.getReturnDate());
        assertEquals(0, result.getFine().compareTo(BigDecimal.ZERO));
        assertFalse(result.getOverdue());

        verify(bookService, times(1)).increaseStock(1L, 1);
        verify(borrowRecordRepository, times(1)).updateById(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("归还成功 - 逾期归还（计算罚款）")
    void returnBook_Success_Overdue() {
        // Given - 设置借阅记录为已逾期状态
        testRecord.setBorrowDate(LocalDate.now().minusDays(40));
        testRecord.setDueDate(LocalDate.now().minusDays(10));

        when(borrowRecordRepository.getById(1L)).thenReturn(testRecord);

        when(bookService.increaseStock(1L, 1)).thenReturn(1);

        when(borrowRecordRepository.updateById(any(BorrowRecord.class))).thenReturn(true);
        when(userService.getById(1L)).thenReturn(testUser);
        when(bookService.getById(1L)).thenReturn(testBook);

        // When
        BorrowRecordVO result = borrowService.returnBook(1L);

        // Then
        assertNotNull(result);
        assertEquals("RETURNED", result.getStatus());
        assertTrue(result.getOverdue());
        // 逾期10天，每天0.5元，罚款5元
        assertEquals(0, result.getFine().compareTo(new BigDecimal("5.00")));
    }

    @Test
    @DisplayName("归还失败 - 借阅记录不存在")
    void returnBook_RecordNotFound() {
        // Given
        when(borrowRecordRepository.getById(99L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.returnBook(99L));
        assertEquals(ResultCode.BORROW_RECORD_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("归还失败 - 已归还")
    void returnBook_AlreadyReturned() {
        // Given
        testRecord.setStatus("RETURNED");
        when(borrowRecordRepository.getById(1L)).thenReturn(testRecord);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.returnBook(1L));
        assertEquals(ResultCode.BORROW_ALREADY_RETURNED.getCode(), exception.getCode());
    }
}
