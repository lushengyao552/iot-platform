package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 借阅记录 Mapper 接口
 */
@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    /**
     * 查询用户当前借阅中（未归还）的图书数量
     *
     * @param userId 用户ID
     * @return 借阅中数量
     */
    @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id = #{userId} AND status = 'BORROWED' AND deleted = 0")
    int countBorrowingByUserId(@Param("userId") Long userId);

    /**
     * 查询用户是否已借阅某本书（未归还）
     *
     * @param userId 用户ID
     * @param bookId 图书ID
     * @return 数量（0表示未借阅）
     */
    @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id = #{userId} AND book_id = #{bookId} AND status = 'BORROWED' AND deleted = 0")
    int countByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
