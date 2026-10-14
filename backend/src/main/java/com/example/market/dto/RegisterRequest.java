package com.example.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "[A-Za-z0-9_]{3,50}", message = "用户名须为3至50位字母、数字或下划线") String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码须为8至72个字符，且不能超过72字节") String password,
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称不能超过50个字符") String nickname) {
}
