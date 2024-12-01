package com.api.cargosimpleclient.DTO;

import lombok.Data;

/**
 * DTO для представления пользователя в системе.
 * <p>
 * Содержит базовую информацию о пользователе,
 * используется для идентификации и базового представления.
 *
 */
@Data
public class UserDto {
    private String login;
}
