package com.example.market.controller;

import com.example.market.service.AuthService;
import com.example.market.vo.UserVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService auth;
    public UserController(AuthService auth) { this.auth = auth; }

    @GetMapping("/me")
    public UserVO me(@AuthenticationPrincipal Jwt jwt) { return auth.currentUser(jwt.getSubject()); }
}
