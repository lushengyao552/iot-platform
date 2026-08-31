package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.BookCategory;
import com.example.library.vo.CategoryVO;

import java.util.List;

/**
 * 图书分类服务接口
 */
public interface BookCategoryService extends IService<BookCategory> {

    /**
     * 查询所有分类（按排序号排序）
     *
     * @return 分类列表
     */
    List<CategoryVO> listAllCategories();

    /**
     * 新增分类
     *
     * @param category 分类实体
     * @return 新增的分类
     */
    CategoryVO addCategory(BookCategory category);

    /**
     * 更新分类
     *
     * @param category 分类实体
     * @return 更新后的分类
     */
    CategoryVO updateCategory(BookCategory category);

    /**
     * 删除分类
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 转换为 VO
     */
    CategoryVO toVO(BookCategory category);
}
