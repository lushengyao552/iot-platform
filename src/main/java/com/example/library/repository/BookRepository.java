package com.example.library.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.Book;

/**
 * 图书数据仓库接口
 *
 * <p>继承 IService<Book> 获得 MyBatis-Plus 提供的通用 CRUD 能力，
 * 同时声明图书特有的数据访问方法。
 */
public interface BookRepository extends IService<Book> {

    /**
     * 扣减库存（原子操作，防止超卖）
     *
     * @param bookId 图书ID
     * @param count  扣减数量
     * @return 影响行数（0表示库存不足）
     */
    int decreaseStock(Long bookId, Integer count);

    /**
     * 增加库存
     *
     * @param bookId 图书ID
     * @param count  增加数量
     * @return 影响行数
     */
    int increaseStock(Long bookId, Integer count);
}
