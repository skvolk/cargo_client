package com.api.cargosimpleclient.Services;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Сервисный класс для загрузки и отображения представлений (view) в JavaFX приложении.
 * <p>
 * Предоставляет методы для динамической смены сцен и отображения диалоговых окон с ошибками.
 *
 */
public class LoadViewService {

    private final AlertService alertService = new AlertService();

    /**
     * Загружает и отображает новое представление (FXML) в текущем окне.
     *
     * @param fxmlPath Путь к FXML файлу для загрузки
     * @param control Элемент управления, используемый для получения текущей сцены
     * @param title Заголовок окна
     */
    public void loadView(String fxmlPath, Control control, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) control.getScene().getWindow();
            Scene scene = new Scene(root, 1600, 900);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            alertService.showErrorAlert("Ошибка", "Не удалось открыть " + title.toLowerCase());
        }
    }

    /**
     * Отображает диалоговое окно с информацией об ошибке.
     *
     * @param title Заголовок диалогового окна
     * @param message Основное сообщение об ошибке
     * @param exception Объект исключения для детализации ошибки (может быть null)
     */
    public void showErrorAlert(String title, String message, Exception exception) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);

        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            if (exceptionMessage != null) {
                Text exceptionText = new Text(exceptionMessage);
                alert.getDialogPane().setContent(exceptionText);
            }
        }

        alert.getButtonTypes().setAll(ButtonType.OK);

        alert.showAndWait();
    }
}
