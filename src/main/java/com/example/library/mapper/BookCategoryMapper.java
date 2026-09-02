package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.BookCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图书分类 Mapper 接口
 */
@Mapper
public interface BookCategoryMapper extends BaseMapper<BookCategory> {
}
