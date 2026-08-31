package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.entity.Book;
import com.example.library.entity.BookCategory;
import com.example.library.mapper.BookCategoryMapper;
import com.example.library.service.BookCategoryService;
import com.example.library.service.BookService;
import com.example.library.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书分类服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookCategoryServiceImpl extends ServiceImpl<BookCategoryMapper, BookCategory> implements BookCategoryService {

    private final BookService bookService;

    @Override
    public List<CategoryVO> listAllCategories() {
        LambdaQueryWrapper<BookCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BookCategory::getSort)
                .orderByAsc(BookCategory::getId);
        List<BookCategory> categories = list(wrapper);

        return categories.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO addCategory(BookCategory category) {
        // 校验名称是否已存在
        validateNameUnique(category.getName(), null);

        if (category.getSort() == null) {
            category.setSort(0);
        }

        save(category);
        log.info("新增分类成功: categoryId={}, name={}", category.getId(), category.getName());
        return toVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO updateCategory(BookCategory category) {
        BookCategory existing = getById(category.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }

        // 校验名称唯一性（排除自身）
        if (StringUtils.hasText(category.getName())) {
            validateNameUnique(category.getName(), category.getId());
        }

        updateById(category);
        log.info("更新分类成功: categoryId={}", category.getId());
        return toVO(getById(category.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        BookCategory category = getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }

        // 检查该分类下是否有图书
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getCategoryId, id);
        long bookCount = bookService.count(wrapper);
        if (bookCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该分类下存在 " + bookCount + " 本图书，无法删除");
        }

        removeById(id);
        log.info("删除分类成功: categoryId={}", id);
    }

    /**
     * 校验分类名称唯一性
     */
    private void validateNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<BookCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookCategory::getName, name);
        if (excludeId != null) {
            wrapper.ne(BookCategory::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.CATEGORY_NAME_EXIST);
        }
    }

    @Override
    public CategoryVO toVO(BookCategory category) {
        if (category == null) {
            return null;
        }
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);

        // 统计该分类下的图书数量
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getCategoryId, category.getId());
        vo.setBookCount((int) bookService.count(wrapper));

        return vo;
    }
}
