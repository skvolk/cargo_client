package com.api.cargosimpleclient.Services;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

/**
 * Сервис для отображения диалоговых окон (alerts) в JavaFX приложении.
 * <p>
 * Предоставляет методы для показа информационных и error-диалогов,
 * а также обработки ошибок входа с потокобезопасным механизмом.
 *
 */
public class AlertService {

    /**
     * Отображает информационное диалоговое окно об успешном выполнении операции.
     *
     * @param message Текст сообщения для отображения
     */
    public void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Отображает диалоговое окно с сообщением об ошибке.
     *
     * @param title Заголовок диалогового окна
     * @param message Текст сообщения об ошибке
     */
    public void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Обрабатывает ошибки входа с потокобезопасным механизмом отображения.
     * <p>
     * Метод выполняется в JavaFX Application Thread, что позволяет
     * безопасно взаимодействовать с UI-компонентами.
     *
     * @param throwable Объект исключения с информацией об ошибке
     * @param button Кнопка, состояние которой необходимо изменить после обработки
     */
    public void handleLoginError(Throwable throwable, Button button) {
        Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка входа");
            alert.setHeaderText(null);
            alert.setContentText(throwable.getMessage());
            alert.showAndWait();

            button.setDisable(false);
        });
    }
}
