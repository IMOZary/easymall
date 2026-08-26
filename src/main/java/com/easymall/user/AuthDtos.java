package com.easymall.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Pattern(regexp = "[a-zA-Z0-9_]{4,20}", message = "用户名需为4-20位字母、数字或下划线") String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 30, message = "密码需为6-30位") String password,
            @NotBlank(message = "昵称不能为空")
            @Size(max = 20, message = "昵称最多20个字符") String nickname) {}

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {}

    public record UserView(Long id, String username, String nickname, Role role) {
        public static UserView from(User user) {
            return new UserView(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
        }
    }
}
