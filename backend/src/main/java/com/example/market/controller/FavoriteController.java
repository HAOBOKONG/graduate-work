package com.example.market.controller;

import com.example.market.service.AuthService;
import com.example.market.service.FavoriteService;
import com.example.market.vo.FavoriteProductVO;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final FavoriteService favorites;
    private final AuthService auth;

    public FavoriteController(FavoriteService favorites, AuthService auth) {
        this.favorites = favorites;
        this.auth = auth;
    }

    private Long userId(Jwt jwt) { return auth.currentUser(jwt.getSubject()).id(); }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> add(@PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {
        favorites.add(userId(jwt), productId);
        return Map.of("message", "收藏成功");
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long productId, @AuthenticationPrincipal Jwt jwt) {
        favorites.remove(userId(jwt), productId);
    }

    @GetMapping("/my")
    public List<FavoriteProductVO> my(@AuthenticationPrincipal Jwt jwt) {
        return favorites.my(userId(jwt));
    }
}
