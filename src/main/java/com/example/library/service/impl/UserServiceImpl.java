package com.example.library.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.dto.LoginDTO;
import com.example.library.dto.RegisterDTO;
import com.example.library.entity.User;
import com.example.library.repository.UserRepository;
import com.example.library.service.UserService;
import com.example.library.util.JwtUtil;
import com.example.library.vo.LoginVO;
import com.example.library.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 *
 * <p>核心业务：
 * <ul>
 *   <li>登录：验证用户名密码 → 生成 JWT Token</li>
 *   <li>注册：校验用户名唯一性 → BCrypt 加密密码 → 保存用户</li>
 * </ul>
 *
 * <p>@Transactional：声明式事务，方法内所有数据库操作要么全部成功，要么全部回滚
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${library.jwt.expiration}")
    private Long expiration;

    @Value("${library.jwt.prefix}")
    private String tokenPrefix;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 查询用户
        User user = getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_NOT_FOUND);
        }

        // 2. 检查账号状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 3. 验证密码（BCrypt 校验）
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        log.info("用户登录成功: username={}, role={}", user.getUsername(), user.getRole());

        // 5. 构建登录响应
        return LoginVO.builder()
                .token(token)
                .tokenPrefix(tokenPrefix)
                .expiresIn(expiration)
                .user(toVO(user))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO registerDTO) {
        // 1. 校验用户名是否已存在
        User existingUser = getByUsername(registerDTO.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }

        // 2. 构建用户实体
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);

        // 3. BCrypt 加密密码（每次加密结果不同，因为包含随机盐）
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword(), BCrypt.gensalt()));

        // 4. 设置默认值
        user.setRole("USER");  // 注册默认普通用户
        user.setStatus(1);     // 默认正常状态

        // 5. 保存用户
        userRepository.save(user);

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());

        return toVO(user);
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userRepository.getOne(wrapper);
    }

    @Override
    public User getById(Long id) {
        return userRepository.getById(id);
    }

    @Override
    public UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
