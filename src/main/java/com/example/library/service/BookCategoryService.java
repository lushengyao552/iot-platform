package com.example.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.entity.Book;
import com.example.library.entity.BookCategory;
import com.example.library.repository.BookCategoryRepository;
import com.example.library.repository.BookRepository;
import com.example.library.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookCategoryService {

    private final BookCategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    public List<CategoryVO> list() {
        List<BookCategory> categories = categoryRepository.list(
                new LambdaQueryWrapper<BookCategory>().orderByAsc(BookCategory::getSort));
        return categories.stream().map(cat -> {
            Long bookCount = bookRepository.count(
                    new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, cat.getId()));
            return CategoryVO.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .description(cat.getDescription())
                    .sort(cat.getSort())
                    .bookCount(bookCount)
                    .createTime(cat.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
    }

    public void add(String name, String description, Integer sort) {
        Long count = categoryRepository.count(
                new LambdaQueryWrapper<BookCategory>().eq(BookCategory::getName, name));
        if (count > 0) {
            throw new BusinessException(ResultCode.CATEGORY_NAME_EXIST);
        }
        BookCategory cat = new BookCategory();
        cat.setName(name);
        cat.setDescription(description);
        cat.setSort(sort != null ? sort : 0);
        categoryRepository.save(cat);
    }

    public void update(Long id, String name, String description, Integer sort) {
        BookCategory cat = categoryRepository.getById(id);
        if (cat == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        if (name != null) cat.setName(name);
        if (description != null) cat.setDescription(description);
        if (sort != null) cat.setSort(sort);
        categoryRepository.updateById(cat);
    }

    public void delete(Long id) {
        Long bookCount = bookRepository.count(
                new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, id));
        if (bookCount > 0) {
            throw new BusinessException("该分类下还有图书，无法删除");
        }
        if (!categoryRepository.removeById(id)) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
    }
}
