package com.api.cargosimpleclient.Controllers;

import com.api.cargosimpleclient.DTO.UserDto;
import com.api.cargosimpleclient.Services.AlertService;
import com.api.cargosimpleclient.Services.AuthService;
import com.api.cargosimpleclient.Services.LoadViewService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Контроллер для управления процессом регистрации пользователя.
 * <p>
 * Обеспечивает логику регистрации нового пользователя,
 * включая валидацию данных и взаимодействие с сервисом аутентификации.
 *
 */
public class RegisterController {

    private final AuthService authService = new AuthService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button registerButton;

    private final LoadViewService loadViewService = new LoadViewService();

    private final AlertService alertService = new AlertService();

    /**
     * Обработчик возврата на экран входа.
     * <p>
     * Переключает представление на экран авторизации.
     */
    @FXML
    private void backToLogin() {
        openLoginScreen();
    }

    /**
     * Обработчик процесса регистрации.
     * <p>
     * Выполняет следующие действия:
     * 1. Валидация введенных данных
     * 2. Создание нового пользователя
     * 3. Вызов сервиса регистрации
     * 4. Обработка результата регистрации
     */
    @FXML
    private void handleRegister() {
        if (!validateFields()) {
            return;
        }

        UserDto newUser = new UserDto();
        newUser.setLogin(usernameField.getText());

        registerButton.setDisable(true);

        authService.register(newUser, passwordField.getText())
                .thenAcceptAsync(this::processSuccessfulRegistration, Platform::runLater)
                .exceptionally(this::handleRegistrationError);
    }

    /**
     * Обрабатывает успешную регистрацию пользователя.
     * <p>
     * Выполняет следующие действия:
     * 1. Отображает уведомление об успешной регистрации
     * 2. Переходит на экран входа
     *
     * @param registeredUser Зарегистрированный пользователь
     */
    private void processSuccessfulRegistration(UserDto registeredUser) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Регистрация");
                alert.setHeaderText(null);
                alert.setContentText("Регистрация прошла успешно!");
                alert.showAndWait();

                openLoginScreen();
            } catch (Exception e) {
                handleRegistrationError(e);
            } finally {
                registerButton.setDisable(false);
            }
        });
    }

    /**
     * Открывает экран входа в систему.
     * <p>
     * Загружает представление экрана авторизации.
     */
    private void openLoginScreen()  {
        loadViewService.loadView("/fxml/login.fxml", registerButton, "Авторизация");
    }

    /**
     * Выполняет валидацию полей регистрации.
     * <p>
     * Проверяет:
     * 1. Заполненность всех обязательных полей
     * 2. Совпадение паролей
     *
     * @return true, если все поля корректны, иначе false
     */
    private boolean validateFields() {

        if (usernameField.getText().isEmpty() ||
                passwordField.getText().isEmpty()) {
            alertService.showErrorAlert("Ошибка", "Все поля должны быть заполнены");
            return false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            alertService.showErrorAlert("Ошибка", "Пароли не совпадают");
            return false;
        }

        return true;
    }

    /**
     * Обрабатывает ошибки, возникшие в процессе регистрации.
     * <p>
     * Отображает сообщение об ошибке и разблокирует кнопку регистрации.
     *
     * @param throwable Исключение, возникшее при регистрации
     * @return null
     */
    private Void handleRegistrationError(Throwable throwable) {
        Platform.runLater(() -> {
            alertService.showErrorAlert("Ошибка регистрации", throwable.getMessage());
            registerButton.setDisable(false);
        });
        return null;
    }
}
