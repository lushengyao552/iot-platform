package com.iot.platform.user.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.platform.common.exception.BusinessException;
import com.iot.platform.common.result.ResultCode;
import com.iot.platform.user.dto.LoginDTO;
import com.iot.platform.user.dto.RegisterDTO;
import com.iot.platform.user.entity.User;
import com.iot.platform.user.mapper.UserMapper;
import com.iot.platform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) throw new BusinessException(ResultCode.USERNAME_EXIST);

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
    }

    public Map<String, Object> login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword()))
            throw new BusinessException(ResultCode.PASSWORD_ERROR);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenPrefix", "Bearer ");
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("role", user.getRole());
        result.put("user", userInfo);
        return result;
    }
}
