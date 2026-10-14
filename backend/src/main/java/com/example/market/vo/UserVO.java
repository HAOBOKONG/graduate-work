package com.example.market.vo;

import com.example.market.entity.User;

/** 认证响应只暴露基本信息，不序列化密码哈希。 */
public record UserVO(Long id, String username, String nickname, String avatar, String role, String status) {
    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                user.getRole(), user.getStatus());
    }
}
