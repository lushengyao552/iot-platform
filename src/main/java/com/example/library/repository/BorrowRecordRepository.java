package com.example.library.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.BorrowRecord;

/**
 * 借阅记录数据仓库接口
 */
public interface BorrowRecordRepository extends IService<BorrowRecord> {

    /**
     * 查询用户当前借阅中（未归还）的图书数量
     *
     * @param userId 用户ID
     * @return 借阅中数量
     */
    int countBorrowingByUserId(Long userId);

    /**
     * 查询用户是否已借阅某本书（未归还）
     *
     * @param userId 用户ID
     * @param bookId 图书ID
     * @return 数量（0表示未借阅）
     */
    int countByUserAndBook(Long userId, Long bookId);
}
