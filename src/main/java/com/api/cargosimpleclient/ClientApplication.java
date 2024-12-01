package com.api.cargosimpleclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Главный класс JavaFX приложения для клиентской части CargoSimpleClient.
 * <p>
 * Отвечает за инициализацию и запуск графического интерфейса приложения,
 * загружая стартовую страницу авторизации.
 *
 */
public class ClientApplication extends Application {

    /**
     * Метод запуска JavaFX приложения, который настраивает и отображает
     * начальное окно авторизации.
     *
     * @param primaryStage Основная сцена приложения
     * @throws Exception В случае ошибки загрузки FXML или ресурсов
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));

        Scene scene = new Scene(root, 1600, 900);

        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());

        primaryStage.setTitle("Авторизация");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Точка входа в приложение. Вызывает метод launch для запуска JavaFX приложения.
     *
     * @param args Аргументы командной строки
     */
    public static void main(String[] args) {
        launch(args);
    }
}