package com.example.library.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.entity.BorrowRecord;
import com.example.library.mapper.BorrowRecordMapper;
import com.example.library.repository.BorrowRecordRepository;
import org.springframework.stereotype.Repository;

/**
 * 借阅记录数据仓库实现类
 *
 * <p>封装借阅记录相关的数据访问操作，内部调用 Mapper 完成数据库交互。
 */
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
