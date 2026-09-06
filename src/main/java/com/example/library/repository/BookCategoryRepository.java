package com.example.library.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.BookCategory;

/**
 * 图书分类数据仓库接口
 */
public interface BookCategoryRepository extends IService<BookCategory> {

    /**
     * 统计指定分类下的图书数量
     *
     * @param categoryId 分类ID
     * @return 图书数量
     */
    Long countByCategoryId(Long categoryId);
}
