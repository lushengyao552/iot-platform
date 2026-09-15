package com.example.library.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.entity.BorrowRecord;
import com.example.library.mapper.BorrowRecordMapper;
import com.example.library.repository.BorrowRecordRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BorrowRecordRepositoryImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowRecordRepository {

    @Override
    public int countBorrowingByUserId(Long userId) {
        return baseMapper.countBorrowingByUserId(userId);
    }

    @Override
    public int countByUserAndBook(Long userId, Long bookId) {
        return baseMapper.countByUserAndBook(userId, bookId);
    }
}
