package com.example.library.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.entity.Book;
import com.example.library.entity.BookCategory;
import com.example.library.mapper.BookCategoryMapper;
import com.example.library.mapper.BookMapper;
import com.example.library.repository.BookCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 图书分类数据仓库实现类
 *
 * <p>封装分类相关的数据访问操作，内部调用 Mapper 完成数据库交互。
 */
@Repository
public class BookCategoryRepositoryImpl extends ServiceImpl<BookCategoryMapper, BookCategory> implements BookCategoryRepository {

    // 注入 BookMapper 用于跨表统计分类下的图书数量（字段注入以保留无参构造器，供子类继承）
    @Autowired
    private BookMapper bookMapper;

    @Override
    public Long countByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getCategoryId, categoryId);
        return bookMapper.selectCount(wrapper);
    }
}
