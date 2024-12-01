package com.api.cargosimpleclient.DTO;

import lombok.Data;

/**
 * DTO для запроса аутентификации пользователя.
 * <p>
 * Содержит credentials (учетные данные) для входа в систему,
 * используется в процессе авторизации.
 *
 */
@Data
public class LoginRequestDto {
    private String login;
    private String password;

    public LoginRequestDto(String username, String password) {
        this.login = username;
        this.password = password;
    }
}
