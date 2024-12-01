package com.api.cargosimpleclient.Services;

import com.api.cargosimpleclient.DTO.LoginRequestDto;
import com.api.cargosimpleclient.DTO.UserDto;
import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис аутентификации и регистрации пользователей.
 * <p>
 * Предоставляет методы для выполнения операций входа и регистрации
 * с использованием HTTP-запросов к серверу аутентификации.
 *
 */
public class AuthService {

    private static final String BASE_URL = "http://localhost:8081/api/auth";
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final Gson GSON = new Gson();

    /**
     * Выполняет аутентификацию пользователя.
     *
     * @param login Логин пользователя
     * @param password Пароль пользователя
     * @return CompletableFuture с данными пользователя после успешной аутентификации
     */
    public CompletableFuture<UserDto> login(String login, String password) {

        CompletableFuture<UserDto> future = new CompletableFuture<>();

        LoginRequestDto loginRequest = new LoginRequestDto(login, password);

        String jsonBody = GSON.toJson(loginRequest);

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/login")
                .post(body)
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {

                    assert response.body() != null;
                    String responseBody = response.body().string();

                    UserDto user = GSON.fromJson(responseBody, UserDto.class);

                    future.complete(user);
                } else {
                    future.completeExceptionally(
                            new Exception("Ошибка входа: " + response.message())
                    );
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Регистрация нового пользователя.
     *
     * @param user Dto пользователя с базовой информацией
     * @param password Пароль для регистрации
     * @return CompletableFuture с данными зарегистрированного пользователя
     */
    public CompletableFuture<UserDto> register(UserDto user, String password) {

        CompletableFuture<UserDto> future = new CompletableFuture<>();

        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("login", user.getLogin());
        registrationData.put("password", password);

        String jsonBody = GSON.toJson(registrationData);

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "/register")
                .post(body)
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    UserDto registeredUser = GSON.fromJson(responseBody, UserDto.class);
                    future.complete(registeredUser);
                } else {
                    future.completeExceptionally(
                            new Exception("Ошибка регистрации: " + response.message())
                    );
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
