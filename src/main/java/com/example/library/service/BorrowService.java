package com.example.library.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.BorrowRecord;
import com.example.library.vo.BorrowRecordVO;

/**
 * 借阅服务接口
 */
public interface BorrowService extends IService<BorrowRecord> {

    /**
     * 借阅图书
     *
     * @param userId 用户ID
     * @param bookId 图书ID
     * @return 借阅记录
     */
    BorrowRecordVO borrowBook(Long userId, Long bookId);

    /**
     * 归还图书
     *
     * @param recordId 借阅记录ID
     * @return 归还后的借阅记录
     */
    BorrowRecordVO returnBook(Long recordId);

    /**
     * 分页查询当前用户的借阅记录
     *
     * @param userId   用户ID
     * @param status   借阅状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    IPage<BorrowRecordVO> pageUserBorrowRecords(Long userId, String status, Integer pageNum, Integer pageSize);

    /**
     * 分页查询所有借阅记录（管理员）
     *
     * @param status   借阅状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    IPage<BorrowRecordVO> pageAllBorrowRecords(String status, Integer pageNum, Integer pageSize);

    /**
     * 转换为 VO（补充用户名、书名等关联信息）
     */
    BorrowRecordVO toVO(BorrowRecord record);
}
