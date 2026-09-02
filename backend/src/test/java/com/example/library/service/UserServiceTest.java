package com.example.library.service;

import cn.hutool.crypto.digest.BCrypt;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.dto.LoginDTO;
import com.example.library.dto.RegisterDTO;
import com.example.library.entity.User;
import com.example.library.mapper.UserMapper;
import com.example.library.service.impl.UserServiceImpl;
import com.example.library.util.JwtUtil;
import com.example.library.vo.LoginVO;
import com.example.library.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 *
 * <p>使用 @ExtendWith(MockitoExtension.class) 启用 Mockito
 * <p>@Mock：模拟依赖对象，不调用真实方法
 * <p>@InjectMocks：将模拟对象注入到被测对象中
 *
 * <p>单元测试不依赖 Spring 容器和数据库，运行速度快，可重复执行。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));
        testUser.setNickname("测试用户");
        testUser.setRole("USER");
        testUser.setStatus(1);

        // 注入 @Value 字段
        ReflectionTestUtils.setField(userService, "expiration", 86400000L);
        ReflectionTestUtils.setField(userService, "tokenPrefix", "Bearer ");

        // 注入 ServiceImpl 基类的 baseMapper 字段（Mockito 不会自动注入父类字段）
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("登录成功 - 返回 Token 和用户信息")
    void login_Success() {
        // Given
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);
        when(jwtUtil.generateToken(eq(1L), eq("testuser"), eq("USER")))
                .thenReturn("mock-jwt-token");

        // When
        LoginVO result = userService.login(loginDTO);

        // Then
        assertNotNull(result);
        assertEquals("mock-jwt-token", result.getToken());
        assertEquals("Bearer ", result.getTokenPrefix());
        assertEquals(86400000L, result.getExpiresIn());
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());
        assertEquals("测试用户", result.getUser().getNickname());

        verify(userMapper, times(1)).selectOne(any(), anyBoolean());
        verify(jwtUtil, times(1)).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void login_UserNotFound() {
        // Given
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("nonexistent");
        loginDTO.setPassword("password123");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.login(loginDTO));
        assertEquals(ResultCode.USERNAME_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_PasswordError() {
        // Given
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("wrongpassword");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.login(loginDTO));
        assertEquals(ResultCode.PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录失败 - 账号已禁用")
    void login_UserDisabled() {
        // Given
        testUser.setStatus(0);
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.login(loginDTO));
        assertEquals(ResultCode.USER_DISABLED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("注册成功 - 返回用户信息")
    void register_Success() {
        // Given
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("newpassword123");
        registerDTO.setNickname("新用户");
        registerDTO.setEmail("new@example.com");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return 1;
        });

        // When
        UserVO result = userService.register(registerDTO);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("新用户", result.getNickname());
        assertEquals("USER", result.getRole());
        assertEquals(1, result.getStatus());

        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_UsernameExists() {
        // Given
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.register(registerDTO));
        assertEquals(ResultCode.USERNAME_EXIST.getCode(), exception.getCode());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("根据用户名查询用户")
    void getByUsername_Success() {
        // Given
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        // When
        User result = userService.getByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    @DisplayName("转换为 VO - 去除密码等敏感字段")
    void toVO_Success() {
        // When
        UserVO vo = userService.toVO(testUser);

        // Then
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("testuser", vo.getUsername());
        assertEquals("测试用户", vo.getNickname());
        assertEquals("USER", vo.getRole());
        // VO 中不应包含密码字段（编译期保证）
    }

    @Test
    @DisplayName("转换为 VO - 输入 null 返回 null")
    void toVO_NullInput() {
        assertNull(userService.toVO(null));
    }
}
