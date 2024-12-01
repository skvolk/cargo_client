package com.api.cargosimpleclient.Controllers.Warehouses;

import com.api.cargosimpleclient.DTO.WarehouseDTO;
import com.api.cargosimpleclient.DTO.WarehouseStatus;
import com.api.cargosimpleclient.Services.AlertService;
import com.api.cargosimpleclient.Services.LoadViewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

/**
 * Контроллер управления складами в пользовательском интерфейсе.
 * <p>
 * Основные функции:
 * - Отображение списка складов в таблице
 * - Фильтрация и поиск складов
 * - Управление складами (добавление, редактирование, удаление)
 * - Отображение статистики складов
 * - Навигация между связанными представлениями
 */
public class WarehousesController {

    @FXML
    private TableView<WarehouseDTO> warehousesTable;

    @FXML
    private TableColumn<WarehouseDTO, Long> idColumn;

    @FXML
    private TableColumn<WarehouseDTO, String> nameColumn;

    @FXML
    private TableColumn<WarehouseDTO, String> addressColumn;

    @FXML
    private TableColumn<WarehouseDTO, String> contactPersonColumn;

    @FXML
    private TableColumn<WarehouseDTO, String> phoneColumn;

    @FXML
    private TableColumn<WarehouseDTO, String> emailColumn;

    @FXML
    private TableColumn<WarehouseDTO, Integer> capacityColumn;

    @FXML
    private TableColumn<WarehouseDTO, WarehouseStatus> statusColumn;

    @FXML
    private Button productsButton;

    @FXML
    private Button warehouseProductsButton;

    @FXML
    private Button addWarehouseButton;

    @FXML
    private Button editWarehouseButton;

    @FXML
    private Button deleteWarehouseButton;

    @FXML
    private TextField filterTextField;

    @FXML
    private Button advancedFilterButton;

    @FXML
    private Label totalWarehousesLabel;

    @FXML
    private Label activeWarehousesLabel;

    @FXML
    private Label inactiveWarehousesLabel;

    @FXML
    private Label totalCapacityLabel;

    private ObservableList<WarehouseDTO> masterWarehouseList;

    private FilteredList<WarehouseDTO> filteredWarehouseList;

    private final LoadViewService loadViewService = new LoadViewService();

    private final AlertService alertService = new AlertService();

    /**
     * Инициализация контроллера после загрузки FXML.
     * <p>
     * Выполняет настройку:
     * - Списков данных
     * - Столбцов таблицы
     * - Загрузки данных с сервера
     * - Обработчиков событий
     * - Кнопок управления
     * - Фильтрации
     * - Статистики
     */
    @FXML
    public void initialize() {
        masterWarehouseList = FXCollections.observableArrayList();
        filteredWarehouseList = new FilteredList<>(masterWarehouseList, p -> true);

        SortedList<WarehouseDTO> sortedWarehouseList = new SortedList<>(filteredWarehouseList);
        sortedWarehouseList.comparatorProperty().bind(warehousesTable.comparatorProperty());

        warehousesTable.setItems(sortedWarehouseList);

        setupTableColumns();
        loadWarehousesFromServer();

        editWarehouseButton.setDisable(true);

        warehousesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    editWarehouseButton.setDisable(newSelection == null);
                }
        );

        deleteWarehouseButton.setDisable(true);

        warehousesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    deleteWarehouseButton.setDisable(newSelection == null);
                }
        );

        advancedFilterButton.setOnAction(event -> showAdvancedFilterDialog());

        filterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredWarehouseList.setPredicate(warehouse -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();
                return matchesSimpleFilter(warehouse, lowerCaseFilter);
            });
        });

        productsButton.setOnAction(event -> openProductsView());
        warehouseProductsButton.setOnAction(event -> openWarehouseProductsView());
        addWarehouseButton.setOnAction(event -> openAddWarehouseDialog());
        editWarehouseButton.setOnAction(event -> openEditWarehouseDialog());
        deleteWarehouseButton.setOnAction(event -> deleteWarehouse());

        updateStatistics();
    }

    /**
     * Открытие диалогового окна добавления нового склада.
     * <p>
     * Выполняет следующие действия:
     * - Загрузка FXML-шаблона окна добавления склада
     * - Настройка контроллера с обработчиком добавления
     * - Создание модального диалогового окна
     * - Обновление списка складов после добавления
     */
    @FXML
    private void openAddWarehouseDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/warehouses/add-warehouse.fxml"));
            Parent root = loader.load();

            AddWarehouseController addController = loader.getController();

            addController.setOnWarehouseAddedListener(newWarehouse -> {
                Platform.runLater(() -> {
                    if (masterWarehouseList != null) {
                        masterWarehouseList.add(newWarehouse);
                    }
                });
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Добавление склада");
            Scene scene = new Scene(root, 350, 750);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            updateStatistics();
        } catch (IOException e) {
            alertService.showErrorAlert("Ошибка", "Не удалось открыть диалог добавления склада");
        }
    }

    /**
     * Открытие диалогового окна редактирования выбранного склада.
     * <p>
     * Последовательность действий:
     * - Проверка наличия выбранного склада
     * - Загрузка FXML-шаблона окна редактирования
     * - Передача данных выбранного склада в контроллер
     * - Настройка обработчика обновления склада
     * - Создание модального диалогового окна
     * - Обновление статистики после редактирования
     */
    @FXML
    private void openEditWarehouseDialog() {
        WarehouseDTO selectedWarehouse = warehousesTable.getSelectionModel().getSelectedItem();

        if (selectedWarehouse == null) {
            alertService.showErrorAlert("Ошибка", "Выберите склад для редактирования");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/warehouses/edit-warehouse.fxml"));
            Parent root = loader.load();

            EditWarehouseController editController = loader.getController();
            editController.setWarehouseToEdit(selectedWarehouse);

            editController.setOnWarehouseUpdatedListener(updatedWarehouse -> {
                Platform.runLater(() -> {
                    if (masterWarehouseList != null) {
                        int index = masterWarehouseList.indexOf(selectedWarehouse);
                        if (index != -1) {
                            masterWarehouseList.set(index, updatedWarehouse);
                        }
                    }
                });
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Редактировать склад");
            Scene scene = new Scene(root, 600, 600);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            updateStatistics();
        } catch (IOException e) {
            alertService.showErrorAlert("Ошибка", "Не удалось открыть диалог редактирования");
        }
    }

    /**
     * Удаление выбранного склада.
     * <p>
     * Процесс удаления включает:
     * - Проверку выбора склада
     * - Отображение диалога подтверждения
     * - Вызов метода удаления на сервере при подтверждении
     * - Обработку возможных ошибок
     */
    @FXML
    private void deleteWarehouse() {
        WarehouseDTO selectedWarehouse = warehousesTable.getSelectionModel().getSelectedItem();

        if (selectedWarehouse == null) {
            alertService.showErrorAlert("Ошибка", "Выберите склад для удаления");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Вы уверены, что хотите удалить склад: " + selectedWarehouse.getName() + "?");
        alert.setContentText("Это действие нельзя будет отменить.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteWarehouseFromServer(selectedWarehouse.getId());
            }
        });
    }

    /**
     * Удаление склада через HTTP-запрос к серверу.
     * <p>
     * Детали процесса:
     * - Формирование HTTP DELETE-запроса
     * - Отправка запроса на удаление склада
     * - Обработка различных сценариев ответа сервера
     * - Обновление локального списка складов
     *
     * @param warehouseId Идентификатор склада для удаления
     */
    private void deleteWarehouseFromServer(Long warehouseId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/warehouses/" + warehouseId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                masterWarehouseList.removeIf(product -> product.getId().equals(warehouseId));
                updateStatistics();
                alertService.showSuccessAlert("Склад успешно удален");
            } else if (response.statusCode() == 400) {
                String errorMessage = response.body();
                alertService.showErrorAlert("Ошибка удаления", errorMessage);
            } else {
                alertService.showErrorAlert("Ошибка удаления", "Не удалось удалить склад");
            }
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка сети", "Не удалось подключиться к серверу: " + e.getMessage());
        }
    }

    /**
     * Открытие представления списка товаров.
     * <p>
     * Использует {@link LoadViewService} для загрузки
     * страницы со списком товаров.
     */
    @FXML
    private void openProductsView() {
        loadViewService.loadView("/fxml/products/products.fxml", warehousesTable, "Информация о товарах");
    }

    /**
     * Открытие представления товаров на складах.
     * <p>
     * Использует {@link LoadViewService} для загрузки
     * страницы с информацией о товарах на различных складах.
     */
    @FXML
    private void openWarehouseProductsView() {
        loadViewService.loadView("/fxml/warehousesInStock/warehouseInStock.fxml", warehousesTable,"Информация о товарах на складах");
    }

    /**
     * Сброс фильтрации списка складов.
     * <p>
     * Выполняет:
     * - Очистку текстового поля фильтра
     * - Сброс фильтра в исходное состояние
     * - Обновление статистики
     */
    @FXML
    private void clearFilters() {
        filterTextField.clear();
        filteredWarehouseList.setPredicate(warehouse -> true);
        updateStatistics();
    }

    /**
     * Переход на домашнюю страницу.
     * <p>
     * Использует {@link LoadViewService} для загрузки
     * главной страницы приложения (дашборда).
     */
    @FXML
    public void goToHomePage() {
        loadViewService.loadView("/fxml/dashboard.fxml", warehousesTable, "Об авторе");
    }

    /**
     * Выход из текущей учетной записи.
     * <p>
     * Использует {@link LoadViewService} для возврата
     * на страницу авторизации.
     */
    @FXML
    private void handleLogout() {
        loadViewService.loadView("/fxml/login.fxml", warehousesTable, "Авторизация");
    }

    /**
     * Отображение диалогового окна расширенной фильтрации складов.
     * <p>
     * Функциональность диалога:
     * - Создание интерактивного интерфейса фильтрации
     * - Динамическое добавление и удаление фильтров
     * - Поддержка фильтрации по различным полям склада
     * - Предварительный просмотр активных фильтров
     * <p>
     * Этапы работы:
     * 1. Создание диалогового окна
     * 2. Настройка элементов управления
     * 3. Конфигурация обработчиков событий
     * 4. Применение выбранных фильтров
     */
    private void showAdvancedFilterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Расширенный фильтр складов");
        dialog.setHeaderText("Настройте критерии поиска");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> fieldComboBox = new ComboBox<>();
        fieldComboBox.getItems().addAll(
                "Название",
                "Адрес",
                "Контактное лицо",
                "Телефон",
                "Email",
                "Вместимость",
                "Статус"
        );
        fieldComboBox.setPromptText("Выберите поле");

        ComboBox<String> operatorComboBox = new ComboBox<>();
        operatorComboBox.getItems().addAll(
                "Содержит",
                "Равно",
                "Больше",
                "Меньше",
                "Больше или равно",
                "Меньше или равно"
        );
        operatorComboBox.setPromptText("Выберите условие");

        TextField valueTextField = new TextField();
        valueTextField.setPromptText("Введите значение");

        ComboBox<WarehouseStatus> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll(WarehouseStatus.values());
        statusComboBox.setPromptText("Выберите статус");

        ListView<String> activeFiltersListView = new ListView<>();
        ObservableList<String> activeFiltersList = FXCollections.observableArrayList();
        activeFiltersListView.setItems(activeFiltersList);

        Button addFilterButton = new Button("Добавить фильтр");
        addFilterButton.setOnAction(e -> {
            if (validateFilterInput(fieldComboBox, operatorComboBox, valueTextField, statusComboBox)) {
                String filterCondition = createFilterCondition(
                        fieldComboBox.getValue(),
                        operatorComboBox.getValue(),
                        valueTextField.getText(),
                        statusComboBox.getValue()
                );
                activeFiltersList.add(filterCondition);
                fieldComboBox.setValue(null);
                operatorComboBox.setValue(null);
                valueTextField.clear();
                statusComboBox.setValue(null);
            }
        });

        Button removeFilterButton = new Button("Удалить фильтр");
        removeFilterButton.setOnAction(e -> {
            int selectedIndex = activeFiltersListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex != -1) {
                activeFiltersList.remove(selectedIndex);
            }
        });

        gridPane.add(new Label("Поле:"), 0, 0);
        gridPane.add(fieldComboBox, 1, 0);
        gridPane.add(new Label("Условие:"), 0, 1);
        gridPane.add(operatorComboBox, 1, 1);
        gridPane.add(new Label("Значение:"), 0, 2);

        gridPane.add(valueTextField, 1, 2);
        gridPane.add(statusComboBox, 1, 2);
        statusComboBox.setVisible(false);

        fieldComboBox.setOnAction(e -> {
            boolean isStatusField = "Статус".equals(fieldComboBox.getValue());
            valueTextField.setVisible(!isStatusField);
            statusComboBox.setVisible(isStatusField);
        });

        HBox buttonBox = new HBox(10, addFilterButton, removeFilterButton);
        gridPane.add(buttonBox, 1, 3);

        gridPane.add(new Label("Активные фильтры:"), 0, 4);
        gridPane.add(activeFiltersListView, 1, 4, 2, 1);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(gridPane);

        dialogPane.getButtonTypes().addAll(
                ButtonType.APPLY,
                ButtonType.CANCEL
        );

        Button applyButton = (Button) dialogPane.lookupButton(ButtonType.APPLY);
        applyButton.setText("Применить");

        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Отмена");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.APPLY) {
                applyFilters(activeFiltersList);
            }
            return dialogButton;
        });

        dialog.showAndWait();
    }

    /**
     * Валидация введенных пользователем параметров фильтра.
     * <p>
     * Проверяет корректность заполнения полей фильтрации:
     * - Наличие выбранного поля
     * - Наличие условия фильтрации
     * - Заполненность значения фильтра
     * - Корректность выбора статуса для специфических полей
     *
     * @param fieldComboBox Выбор поля для фильтрации
     * @param operatorComboBox Выбор условия фильтрации
     * @param valueTextField Текстовое поле значения
     * @param statusComboBox Выбор статуса склада
     *
     * @return true, если все параметры корректны, иначе false
     */
    private boolean validateFilterInput(
            ComboBox<String> fieldComboBox,
            ComboBox<String> operatorComboBox,
            TextField valueTextField,
            ComboBox<WarehouseStatus> statusComboBox
    ) {
        if (fieldComboBox.getValue() == null) {
            alertService.showErrorAlert("Ошибка", "Выберите поле для фильтрации");
            return false;
        }

        if (operatorComboBox.getValue() == null) {
            alertService.showErrorAlert("Ошибка", "Выберите условие фильтрации");
            return false;
        }

        if (!"Статус".equals(fieldComboBox.getValue()) && valueTextField.getText().trim().isEmpty()) {
            alertService.showErrorAlert("Ошибка", "Введите значение для фильтра");
            return false;
        }

        if ("Статус".equals(fieldComboBox.getValue()) && statusComboBox.getValue() == null) {
            alertService.showErrorAlert("Ошибка", "Выберите статус");
            return false;
        }

        return true;
    }

    /**
     * Формирование текстового описания условия фильтрации.
     * <p>
     * Генерирует читаемое представление фильтра для отображения:
     * - Для обычных полей: "Поле Условие Значение"
     * - Для статуса: "Статус Условие Значение"
     *
     * @param field Выбранное поле фильтрации
     * @param operator Условие фильтрации
     * @param value Значение фильтра
     * @param status Статус склада (для специфических фильтров)
     *
     * @return Строковое представление условия фильтрации
     */
    private String createFilterCondition(
            String field,
            String operator,
            String value,
            WarehouseStatus status
    ) {
        if ("Статус".equals(field)) {
            return String.format("Статус %s \"%s\"", operator, status);
        }
        return String.format("%s %s \"%s\"", field, operator, value);
    }

    /**
     * Применение сформированных фильтров к списку складов.
     * <p>
     * Основные действия:
     * - Установка предиката для фильтрованного списка
     * - Проверка каждого склада на соответствие всем активным фильтрам
     * - Динамическое обновление отображаемых складов
     * <p>
     * Алгоритм фильтрации:
     * 1. Если фильтры отсутствуют - показать все склады
     * 2. Проверить каждый склад на соответствие всем фильтрам
     * 3. Исключить склады, не прошедшие хотя бы один фильтр
     *
     * @param activeFiltersList Список активных фильтров
     */
    private void applyFilters(ObservableList<String> activeFiltersList) {
        filteredWarehouseList.setPredicate(warehouse -> {
            if (activeFiltersList.isEmpty()) {
                return true;
            }

            for (String filterCondition : activeFiltersList) {
                if (!matchesFilter(warehouse, filterCondition)) {
                    return false;
                }
            }

            return true;
        });
    }

    /**
     * Проверка соответствия склада конкретному фильтру.
     * <p>
     * Выполняет сопоставление значений склада с условиями фильтра:
     * - Поддержка различных типов сравнения
     * - Обработка числовых и текстовых полей
     * - Специальная логика для статусов
     *
     * @param warehouse Проверяемый склад
     * @param filterCondition Условие фильтрации
     *
     * @return true, если склад соответствует фильтру, иначе false
     */
    private boolean matchesFilter(WarehouseDTO warehouse, String filterCondition) {
        String field, operator, value;
        WarehouseStatus status = null;
        String[] parts = filterCondition.split(" ", 6);
        if (Objects.equals(parts[0], "Контактное")){
            field = parts[0] + " " + parts[1];
            if (Objects.equals(parts[3], "или")){
                operator = parts[2] + " " + parts[3] + " " + parts[4];
                value = parts[5].replaceAll("^\"|\"$", "");;
            }
            else {
                operator = parts[2];
                value = parts[3].replaceAll("^\"|\"$", "");;
            }
        }
        else
        {
            field = parts[0];
            if (Objects.equals(parts[2], "или")){
                operator = parts[1] + " " + parts[2] + " " + parts[3];
                value = parts[4].replaceAll("^\"|\"$", "");;
            }
            else {
                operator = parts[1];
                value = parts[2].replaceAll("^\"|\"$", "");;
            }
        }

        if ((Objects.equals(value, "ACTIVE")) || (Objects.equals(value, "INACTIVE"))){
            status = WarehouseStatus.valueOf(value);
        }

        try {
            switch (field) {
                case "Название":
                    return compareString(warehouse.getName(), operator, value);
                case "Адрес":
                    return compareString(warehouse.getAddress(), operator, value);
                case "Контактное лицо":
                    return compareString(warehouse.getContactPerson(), operator, value);
                case "Телефон":
                    return compareString(warehouse.getPhone(), operator, value);
                case "Email":
                    return compareString(warehouse.getEmail(), operator, value);
                case "Вместимость":
                    return compareInteger(warehouse.getCapacity(), operator, Integer.parseInt(value));
                case "Статус":
                    return warehouse.getStatus() == status;
            }
        } catch (NumberFormatException e) {
            alertService.showErrorAlert("Ошибка", "Неверный формат числа: " + value);
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Неверный формат фильтра: " + filterCondition);
        }

        return false;
    }

    /**
     * Сравнение строковых значений с использованием различных операторов.
     * <p>
     * Поддерживаемые операции:
     * - "Содержит": проверка вхождения подстроки
     * - "Равно": точное совпадение строк
     * <p>
     * Особенности:
     * - Нечувствительность к регистру
     * - Безопасная обработка null-значений
     *
     * @param warehouseValue Значение из объекта склада
     * @param operator Оператор сравнения
     * @param filterValue Значение фильтра
     * @return Результат сравнения
     */
    private boolean compareString(String warehouseValue, String operator, String filterValue) {
        warehouseValue = warehouseValue.toLowerCase();
        filterValue = filterValue.toLowerCase();

        return switch (operator) {
            case "Содержит" -> warehouseValue.contains(filterValue);
            case "Равно" -> warehouseValue.equals(filterValue);
            default -> false;
        };
    }

    /**
     * Сравнение числовых значений с использованием различных операторов.
     * <p>
     * Поддерживаемые операции:
     * - "Больше": строгое превышение
     * - "Меньше": строгое занижение
     * - "Больше или равно": превышение или равенство
     * - "Меньше или равно": занижение или равенство
     * - "Равно": точное совпадение
     *
     * @param warehouseValue Числовое значение из объекта склада
     * @param operator Оператор сравнения
     * @param filterValue Значение фильтра для сравнения
     * @return Результат сравнения
     */
    private boolean compareInteger(int warehouseValue, String operator, int filterValue) {
        return switch (operator) {
            case "Больше" -> warehouseValue > filterValue;
            case "Меньше" -> warehouseValue < filterValue;
            case "Больше или равно" -> warehouseValue >= filterValue;
            case "Меньше или равно" -> warehouseValue <= filterValue;
            case "Равно" -> warehouseValue == filterValue;
            default -> false;
        };
    }

    /**
     * Обновление статистических показателей складов.
     * <p>
     * Расчет и отображение статистики:
     * - Общее количество складов
     * - Количество активных складов
     * - Количество неактивных складов
     * - Суммарная вместимость складов
     * <p>
     * Алгоритм:
     * 1. Подсчет общего количества складов
     * 2. Фильтрация активных складов
     * 3. Расчет суммарной вместимости
     * 4. Обновление меток интерфейса
     */
    private void updateStatistics() {
        int totalWarehouses = masterWarehouseList.size();
        long activeWarehouses = masterWarehouseList.stream()
                .filter(w -> w.getStatus() == WarehouseStatus.ACTIVE)
                .count();
        long inactiveWarehouses = totalWarehouses - activeWarehouses;
        int totalCapacity = masterWarehouseList.stream()
                .mapToInt(WarehouseDTO::getCapacity)
                .sum();

        totalWarehousesLabel.setText(String.format("Всего складов: %d", totalWarehouses));
        activeWarehousesLabel.setText(String.format("Всего активных складов: %d", activeWarehouses));
        inactiveWarehousesLabel.setText(String.format("Всего неактивных складов: %d", inactiveWarehouses));
        totalCapacityLabel.setText(String.format("Общая вместимость: %d", totalCapacity));
    }

    /**
     * Простая фильтрация складов по введенному тексту.
     * <p>
     * Поиск совпадений во множестве полей:
     * - Название склада
     * - Адрес
     * - Контактное лицо
     * - Телефон
     * - Email
     * <p>
     * Особенности:
     * - Нечувствительность к регистру
     * - Частичное совпадение
     *
     * @param warehouse Объект склада для проверки
     * @param filter Текст фильтра
     * @return true, если найдено совпадение
     */
    private boolean matchesSimpleFilter(WarehouseDTO warehouse, String filter) {
        return warehouse.getName().toLowerCase().contains(filter) ||
                warehouse.getAddress().toLowerCase().contains(filter) ||
                warehouse.getContactPerson().toLowerCase().contains(filter) ||
                warehouse.getPhone().toLowerCase().contains(filter) ||
                warehouse.getEmail().toLowerCase().contains(filter);
    }

    /**
     * Настройка столбцов таблицы складов.
     * <p>
     * Выполняемые действия:
     * - Привязка свойств DTO к ячейкам таблицы
     * - Настройка сортировки для каждого столбца
     * <p>
     * Поддерживаемые столбцы:
     * - ID
     * - Название
     * - Адрес
     * - Контактное лицо
     * - Телефон
     * - Email
     * - Вместимость
     * - Статус
     */
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        contactPersonColumn.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        idColumn.setSortable(true);
        nameColumn.setSortable(true);
        addressColumn.setSortable(true);
        contactPersonColumn.setSortable(true);
        phoneColumn.setSortable(true);
        emailColumn.setSortable(true);
        capacityColumn.setSortable(true);
        statusColumn.setSortable(true);
    }

    /**
     * Загрузка списка складов с удаленного сервера.
     * <p>
     * Процесс загрузки:
     * 1. Формирование HTTP GET-запроса
     * 2. Отправка запроса на сервер
     * 3. Десериализация полученного JSON
     * 4. Обновление локального списка складов
     * <p>
     * Обработка сценариев:
     * - Успешная загрузка
     * - Ошибка подключения
     * - Ошибка получения данных
     */
    private void loadWarehousesFromServer() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/warehouses"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                List<WarehouseDTO> warehouses = objectMapper.readValue(response.body(), new TypeReference<List<WarehouseDTO>>() {});

                masterWarehouseList.clear();
                masterWarehouseList.addAll(warehouses);
                updateStatistics();
            } else {
                alertService.showErrorAlert("Ошибка загрузки", "Не удалось загрузить список складов");
            }
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка сети", "Не удалось подключиться к серверу");
        }
    }
}