package com.example.library.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.User;

/**
 * 用户数据仓库接口
 *
 * <p>继承 IService<User> 获得 MyBatis-Plus 提供的通用 CRUD 能力，
 * 当前无自定义数据访问方法。
 */
public interface UserRepository extends IService<User> {
}
