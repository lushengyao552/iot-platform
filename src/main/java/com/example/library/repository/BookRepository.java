package com.example.library.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.Book;

public interface BookRepository extends IService<Book> {
    int decreaseStock(Long bookId, Integer count);
    int increaseStock(Long bookId, Integer count);
}
