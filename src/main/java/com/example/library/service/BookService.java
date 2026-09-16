package com.example.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.dto.BookAddDTO;
import com.example.library.dto.BookQueryDTO;
import com.example.library.dto.BookUpdateDTO;
import com.example.library.entity.Book;
import com.example.library.entity.BookCategory;
import com.example.library.repository.BookCategoryRepository;
import com.example.library.repository.BookRepository;
import com.example.library.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookCategoryRepository bookCategoryRepository;

    public void add(BookAddDTO dto) {
        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        book.setTotalStock(dto.getStock());
        bookRepository.save(book);
    }

    public void update(Long id, BookUpdateDTO dto) {
        Book book = bookRepository.getById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        if (dto.getIsbn() != null) book.setIsbn(dto.getIsbn());
        if (dto.getTitle() != null) book.setTitle(dto.getTitle());
        if (dto.getAuthor() != null) book.setAuthor(dto.getAuthor());
        if (dto.getPublisher() != null) book.setPublisher(dto.getPublisher());
        if (dto.getPublishDate() != null) book.setPublishDate(dto.getPublishDate());
        if (dto.getCategoryId() != null) book.setCategoryId(dto.getCategoryId());
        if (dto.getPrice() != null) book.setPrice(dto.getPrice());
        if (dto.getStock() != null) book.setStock(dto.getStock());
        if (dto.getDescription() != null) book.setDescription(dto.getDescription());
        if (dto.getCoverUrl() != null) book.setCoverUrl(dto.getCoverUrl());
        bookRepository.updateById(book);
    }

    public void delete(Long id) {
        if (!bookRepository.removeById(id)) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
    }

    public BookVO getById(Long id) {
        Book book = bookRepository.getById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        return toVO(book);
    }

    public Map<String, Object> page(BookQueryDTO query) {
        Page<Book> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Book::getTitle, query.getTitle());
        }
        if (StringUtils.hasText(query.getAuthor())) {
            wrapper.like(Book::getAuthor, query.getAuthor());
        }
        if (StringUtils.hasText(query.getIsbn())) {
            wrapper.eq(Book::getIsbn, query.getIsbn());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Book::getCategoryId, query.getCategoryId());
        }
        if (Boolean.TRUE.equals(query.getOnlyAvailable())) {
            wrapper.gt(Book::getStock, 0);
        }

        wrapper.orderByDesc(Book::getCreateTime);
        bookRepository.page(page, wrapper);

        Map<Long, String> categoryMap = loadCategoryMap();

        List<BookVO> voList = page.getRecords().stream()
                .map(b -> toVO(b, categoryMap))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        result.put("pages", page.getPages());
        return result;
    }

    private Map<Long, String> loadCategoryMap() {
        List<BookCategory> categories = bookCategoryRepository.list();
        return categories.stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName));
    }

    private BookVO toVO(Book book) {
        return toVO(book, loadCategoryMap());
    }

    private BookVO toVO(Book book, Map<Long, String> categoryMap) {
        return BookVO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publishDate(book.getPublishDate())
                .categoryId(book.getCategoryId())
                .categoryName(categoryMap.get(book.getCategoryId()))
                .price(book.getPrice())
                .stock(book.getStock())
                .totalStock(book.getTotalStock())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .available(book.getStock() != null && book.getStock() > 0)
                .createTime(book.getCreateTime())
                .build();
    }
}
