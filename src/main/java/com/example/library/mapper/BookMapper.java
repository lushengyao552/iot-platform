package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    @Update("UPDATE book SET stock = stock - #{count} WHERE id = #{bookId} AND stock >= #{count} AND deleted = 0")
    int decreaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);

    @Update("UPDATE book SET stock = stock + #{count} WHERE id = #{bookId} AND deleted = 0")
    int increaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);
}
