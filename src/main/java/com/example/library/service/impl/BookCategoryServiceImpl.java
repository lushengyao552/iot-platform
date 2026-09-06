package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.entity.BookCategory;
import com.example.library.repository.BookCategoryRepository;
import com.example.library.service.BookCategoryService;
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
public class BookCategoryServiceImpl implements BookCategoryService {

    private final BookCategoryRepository categoryRepository;

    @Override
    public List<CategoryVO> listAllCategories() {
        LambdaQueryWrapper<BookCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BookCategory::getSort)
                .orderByAsc(BookCategory::getId);
        List<BookCategory> categories = categoryRepository.list(wrapper);

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

        categoryRepository.save(category);
        log.info("新增分类成功: categoryId={}, name={}", category.getId(), category.getName());
        return toVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO updateCategory(BookCategory category) {
        BookCategory existing = categoryRepository.getById(category.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }

        // 校验名称唯一性（排除自身）
        if (StringUtils.hasText(category.getName())) {
            validateNameUnique(category.getName(), category.getId());
        }

        categoryRepository.updateById(category);
        log.info("更新分类成功: categoryId={}", category.getId());
        return toVO(categoryRepository.getById(category.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        BookCategory category = categoryRepository.getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }

        // 检查该分类下是否有图书
        Long bookCount = categoryRepository.countByCategoryId(id);
        if (bookCount != null && bookCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    "该分类下存在 " + bookCount + " 本图书，无法删除");
        }

        categoryRepository.removeById(id);
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
        if (categoryRepository.count(wrapper) > 0) {
            throw new BusinessException(ResultCode.CATEGORY_NAME_EXIST);
        }
    }

    @Override
    public BookCategory getById(Long id) {
        return categoryRepository.getById(id);
    }

    @Override
    public CategoryVO toVO(BookCategory category) {
        if (category == null) {
            return null;
        }
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);

        // 统计该分类下的图书数量
        Long count = categoryRepository.countByCategoryId(category.getId());
        vo.setBookCount(count != null ? count.intValue() : 0);

        return vo;
    }
}
