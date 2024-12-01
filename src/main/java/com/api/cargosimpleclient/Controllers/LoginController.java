package com.api.cargosimpleclient.Controllers;

import com.api.cargosimpleclient.DTO.UserDto;
import com.api.cargosimpleclient.Services.AlertService;
import com.api.cargosimpleclient.Services.AuthService;
import com.api.cargosimpleclient.Services.LoadViewService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.Getter;

/**
 * Контроллер для управления процессом аутентификации пользователя.
 * <p>
 * Обеспечивает логику входа в систему,
 * включая проверку учетных данных и навигацию между экранами.
 *
 */
public class LoginController {

    @Getter
    private static UserDto currentUser;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    private final AuthService authService = new AuthService();

    private final LoadViewService loadViewService = new LoadViewService();

    private final AlertService alertService = new AlertService();

    /**
     * Обработчик процесса входа в систему.
     * <p>
     * Выполняет следующие действия:
     * 1. Получает введенные учетные данные
     * 2. Блокирует кнопку входа
     * 3. Вызывает сервис аутентификации
     * 4. Обрабатывает результат входа
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        loginButton.setDisable(true);

        authService.login(username, password)
                .thenAcceptAsync(this::processSuccessfulLogin, Platform::runLater)
                .exceptionally(throwable -> {
                    alertService.handleLoginError(throwable, loginButton);
                    return null;
                });
    }

    /**
     * Открывает экран регистрации.
     * <p>
     * Загружает представление формы регистрации
     * при взаимодействии пользователя.
     */
    @FXML
    private void openRegistration() {
        loadViewService.loadView("/fxml/register.fxml", loginButton, "Регистрация");
    }

    /**
     * Обрабатывает успешный вход пользователя в систему.
     * <p>
     * Выполняет следующие действия:
     * 1. Сохраняет информацию о текущем пользователе
     * 2. Открывает главную панель управления
     * 3. Обрабатывает возможные исключения
     *
     * @param user Данные успешно аутентифицированного пользователя
     */
    private void processSuccessfulLogin(UserDto user) {
        Platform.runLater(() -> {
            try {
                currentUser = user;

                openDashboard();
            } catch (Exception e) {
                alertService.handleLoginError(e, loginButton);
            } finally {
                loginButton.setDisable(false);
            }
        });
    }

    /**
     * Открывает главную панель управления (приборную панель).
     * <p>
     * Загружает представление основного экрана после успешной авторизации.
     */
    private void openDashboard() {
        loadViewService.loadView("/fxml/dashboard.fxml", loginButton, "Об авторе");
    }
}
