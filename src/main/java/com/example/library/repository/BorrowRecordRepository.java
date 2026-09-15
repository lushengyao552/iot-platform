package com.example.library.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.BorrowRecord;

public interface BorrowRecordRepository extends IService<BorrowRecord> {
    int countBorrowingByUserId(Long userId);
    int countByUserAndBook(Long userId, Long bookId);
}
