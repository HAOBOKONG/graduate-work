package com.example.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.market.dto.LoginRequest;
import com.example.market.dto.RegisterRequest;
import com.example.market.entity.User;
import com.example.market.mapper.UserMapper;
import com.example.market.vo.LoginResponse;
import com.example.market.vo.UserVO;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserMapper users;
    private final PasswordEncoder passwords;
    private final JwtService tokens;

    public AuthService(UserMapper users, PasswordEncoder passwords, JwtService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @Transactional
    public UserVO register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码UTF-8编码后不能超过72字节");
        }
        if (findByUsername(request.username()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwords.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setRole("USER");
        user.setStatus("ACTIVE");
        try { users.insert(user); }
        catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        return UserVO.from(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = findByUsername(request.username());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72 || user == null
                || !"ACTIVE".equals(user.getStatus()) || !passwords.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误，或账号不可用");
        }
        return new LoginResponse(tokens.issue(user), UserVO.from(user));
    }

    public UserVO currentUser(String subject) {
        Long id;
        try { id = Long.valueOf(subject); }
        catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户身份无效");
        }
        User user = users.selectById(id);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号不可用，请重新登录");
        }
        return UserVO.from(user);
    }

    private User findByUsername(String username) {
        return users.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
