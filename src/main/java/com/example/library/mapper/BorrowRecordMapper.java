package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id = #{userId} AND status = 'BORROWED' AND deleted = 0")
    int countBorrowingByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id = #{userId} AND book_id = #{bookId} AND status = 'BORROWED' AND deleted = 0")
    int countByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
