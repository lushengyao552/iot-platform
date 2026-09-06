package com.example.library.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.library.dto.BookAddDTO;
import com.example.library.dto.BookQueryDTO;
import com.example.library.dto.BookUpdateDTO;
import com.example.library.entity.Book;
import com.example.library.vo.BookVO;

/**
 * 图书服务接口
 */
public interface BookService {

    /**
     * 分页查询图书
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<BookVO> pageBooks(BookQueryDTO queryDTO);

    /**
     * 根据ID查询图书详情
     *
     * @param id 图书ID
     * @return 图书 VO
     */
    BookVO getBookById(Long id);

    /**
     * 新增图书
     *
     * @param addDTO 新增请求
     * @return 新增的图书 VO
     */
    BookVO addBook(BookAddDTO addDTO);

    /**
     * 更新图书
     *
     * @param id        图书ID
     * @param updateDTO 更新请求
     * @return 更新后的图书 VO
     */
    BookVO updateBook(Long id, BookUpdateDTO updateDTO);

    /**
     * 删除图书（逻辑删除）
     *
     * @param id 图书ID
     */
    void deleteBook(Long id);

    /**
     * 根据ID查询图书实体（供其他服务调用）
     *
     * @param id 图书ID
     * @return 图书实体
     */
    Book getById(Long id);

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

    /**
     * 转换为 VO（补充分类名称等关联信息）
     *
     * @param book 图书实体
     * @return 图书 VO
     */
    BookVO toVO(Book book);
}
