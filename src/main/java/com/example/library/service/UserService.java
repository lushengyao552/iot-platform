package com.example.library.service;

import com.example.library.dto.LoginDTO;
import com.example.library.dto.RegisterDTO;
import com.example.library.entity.User;
import com.example.library.vo.LoginVO;
import com.example.library.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param loginDTO 登录请求
     * @return 登录响应（包含 Token 和用户信息）
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户注册
     *
     * @param registerDTO 注册请求
     * @return 注册成功的用户信息
     */
    UserVO register(RegisterDTO registerDTO);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    User getByUsername(String username);

    /**
     * 根据ID查询用户（供控制器调用）
     *
     * @param id 用户ID
     * @return 用户实体
     */
    User getById(Long id);

    /**
     * 转换为 VO（去除敏感字段）
     *
     * @param user 用户实体
     * @return 用户 VO
     */
    UserVO toVO(User user);
}
