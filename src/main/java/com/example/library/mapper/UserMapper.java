package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * <p>继承 BaseMapper<User> 后自动获得以下方法：
 * <ul>
 *   <li>insert：插入</li>
 *   <li>deleteById / delete：按ID/条件删除</li>
 *   <li>updateById / update：按ID/条件更新</li>
 *   <li>selectById / selectOne / selectList / selectPage：查询</li>
 * </ul>
 *
 * <p>自定义查询在 resources/mapper/UserMapper.xml 中定义
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
