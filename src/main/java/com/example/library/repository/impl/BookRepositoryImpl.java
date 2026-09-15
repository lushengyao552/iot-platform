package com.example.library.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.entity.Book;
import com.example.library.mapper.BookMapper;
import com.example.library.repository.BookRepository;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepositoryImpl extends ServiceImpl<BookMapper, Book> implements BookRepository {

    @Override
    public int decreaseStock(Long bookId, Integer count) {
        return baseMapper.decreaseStock(bookId, count);
    }

    @Override
    public int increaseStock(Long bookId, Integer count) {
        return baseMapper.increaseStock(bookId, count);
    }
}
