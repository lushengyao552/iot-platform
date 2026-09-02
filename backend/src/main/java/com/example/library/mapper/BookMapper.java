package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 图书 Mapper 接口
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 扣减库存（原子操作，防止超卖）
     *
     * @param bookId 图书ID
     * @param count  扣减数量
     * @return 影响行数（0表示库存不足）
     */
    @Update("UPDATE book SET stock = stock - #{count} WHERE id = #{bookId} AND stock >= #{count} AND deleted = 0")
    int decreaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);

    /**
     * 增加库存
     *
     * @param bookId 图书ID
     * @param count  增加数量
     * @return 影响行数
     */
    @Update("UPDATE book SET stock = stock + #{count} WHERE id = #{bookId} AND deleted = 0")
    int increaseStock(@Param("bookId") Long bookId, @Param("count") Integer count);
}
