package com.api.cargosimpleclient.Controllers;

import com.api.cargosimpleclient.Services.LoadViewService;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;

/**
 * Контроллер главной панели управления (приборной панели) приложения.
 * <p>
 * Отвечает за:
 * - Настройку и отображение навигационных кнопок
 * - Управление переходами между различными представлениями
 * - Реализацию функционала выхода из системы
 *
 */
public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private GridPane topButtonsGrid;

    @FXML
    private GridPane logoutGrid;

    private LoadViewService loadViewService;

    /**
     * Метод инициализации контроллера.
     * <p>
     * Вызывается автоматически после загрузки FXML.
     * Выполняет:
     * - Инициализацию сервиса загрузки представлений
     * - Настройку кнопок на панели управления
     */
    @FXML
    public void initialize() {
        loadViewService = new LoadViewService();
        setupDashboardButtons();
    }

    /**
     * Обработчик события выхода из системы.
     * <p>
     * Переводит пользователя на экран входа.
     */
    @FXML
    private void handleLogout() {
        loadViewService.loadView("/fxml/login.fxml", welcomeLabel,"Авторизация");
    }

    /**
     * Настраивает кнопки на панели управления.
     * <p>
     * Создает и размещает кнопки:
     * - Товары
     * - Склады
     * - Товары на складах
     * - Выход
     * <p>
     * Применяет стили и настройки макета для кнопок.
     */
    private void setupDashboardButtons() {
        Button[] topButtons = {
                createStyledButton("Товары", this::openProductsView),
                createStyledButton("Склады", this::openWarehousesView),
                createStyledButton("Товары на складах", this::openWarehouseProductsView)
        };

        Button logoutButton = createStyledButton("Выход", this::handleLogout);

        topButtonsGrid.getChildren().clear();

        for (int i = 0; i < topButtons.length; i++) {
            GridPane.setHalignment(topButtons[i], HPos.CENTER);
            GridPane.setValignment(topButtons[i], VPos.CENTER);

            GridPane.setMargin(topButtons[i], new Insets(10));

            GridPane.setHgrow(topButtons[i], Priority.ALWAYS);
            GridPane.setVgrow(topButtons[i], Priority.ALWAYS);

            topButtonsGrid.add(topButtons[i], i, 0);
        }

        logoutGrid.getChildren().clear();

        GridPane.setHalignment(logoutButton, HPos.CENTER);
        GridPane.setValignment(logoutButton, VPos.CENTER);

        logoutGrid.add(logoutButton, 0, 0);
        GridPane.setMargin(logoutButton, new Insets(10));
        GridPane.setHgrow(logoutButton, Priority.ALWAYS);
        GridPane.setVgrow(logoutButton, Priority.ALWAYS);
    }

    /**
     * Создает стилизованную кнопку с заданным текстом и действием.
     *
     * @param text Текст кнопки
     * @param action Действие, выполняемое при нажатии
     * @return Настроенная кнопка
     */
    private Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMaxHeight(Double.MAX_VALUE);
        button.setOnAction(_ -> action.run());
        if (!Objects.equals(text, "Выход")) {
            button.getStyleClass().add("dashboard-button");
        }
        else{
            button.getStyleClass().add("logout-button");
        }
        return button;
    }

    /**
     * Открывает представление списка товаров.
     */
    private void openProductsView() {
        loadViewService.loadView("/fxml/products/products.fxml", welcomeLabel, "Информация о товарах");
    }

    /**
     * Открывает представление списка складов.
     */
    private void openWarehousesView() {
        loadViewService.loadView("/fxml/warehouses/warehouses.fxml", welcomeLabel,"Информация о складах");
    }

    /**
     * Открывает представление товаров на складах.
     */
    private void openWarehouseProductsView() {
        loadViewService.loadView("/fxml/warehousesInStock/warehouseInStock.fxml", welcomeLabel,"Информация о товарах на складах");
    }
}