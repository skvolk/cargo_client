package com.api.cargosimpleclient.Controllers.WarehousesInStock;

import com.api.cargosimpleclient.DTO.WarehouseInStockDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Контроллер для управления товарами на складах.
 * <p>
 * Обеспечивает функционал:
 * - Отображение списка товаров на складах
 * - Фильтрация и поиск товаров
 * - Добавление, редактирование и удаление товаров на складах
 * - Статистический анализ складских остатков
 *
 */
public class WarehousesInStockController {

    @FXML
    private TableView<WarehouseInStockDTO> warehouseInStockTable;

    @FXML
    private TableColumn<WarehouseInStockDTO, Long> idColumn;
    @FXML
    private TableColumn<WarehouseInStockDTO, Long> productIdColumn;
    @FXML
    private TableColumn<WarehouseInStockDTO, Long> warehouseIdColumn;
    @FXML
    private TableColumn<WarehouseInStockDTO, Integer> currentQuantityColumn;
    @FXML
    private TableColumn<WarehouseInStockDTO, Integer> reservedQuantityColumn;
    @FXML
    private TableColumn<WarehouseInStockDTO, String> locationColumn;

    @FXML
    private Label uniqueProductCount;
    @FXML
    private Label uniqueWarehouseCount;
    @FXML
    private Label totalProductQuantity;
    @FXML
    private Label totalReservedQuantity;


    @FXML
    private Button productsButton;
    @FXML
    private Button warehousesButton;
    @FXML
    private Button addWarehouseInStockButton;
    @FXML
    private Button editWarehouseInStockButton;
    @FXML
    private Button deleteWarehouseInStockButton;

    @FXML
    private TextField filterTextField;
    @FXML
    private Button advancedFilterButton;

    private ObservableList<WarehouseInStockDTO> masterInStockList;
    private FilteredList<WarehouseInStockDTO> filteredInStockList;

    private final LoadViewService loadViewService = new LoadViewService();
    private final AlertService alertService = new AlertService();

    /**
     * Инициализация контроллера товаров на складах.
     * <p>
     * Выполняет ключевые настройки:
     * - Создание observable списков
     * - Настройка фильтрации и сортировки
     * - Установка обработчиков событий
     * - Загрузка данных с сервера
     * - Первоначальная настройка интерфейса
     */
    @FXML
    public void initialize() {
        masterInStockList = FXCollections.observableArrayList();
        filteredInStockList = new FilteredList<>(masterInStockList, _ -> true);

        SortedList<WarehouseInStockDTO> sortedWarehouseList = new SortedList<>(filteredInStockList);
        sortedWarehouseList.comparatorProperty().bind(warehouseInStockTable.comparatorProperty());

        warehouseInStockTable.setItems(sortedWarehouseList);

        setupTableColumns();
        loadWarehousesInStockFromServer();

        editWarehouseInStockButton.setDisable(true);

        warehouseInStockTable.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newSelection) -> {
                    editWarehouseInStockButton.setDisable(newSelection == null);
                }
        );

        advancedFilterButton.setOnAction(event -> showAdvancedFilterDialog());

        filterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredInStockList.setPredicate(warehouse -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();
                return matchesSimpleFilter(warehouse, lowerCaseFilter);
            });
        });

        productsButton.setOnAction(event -> openProductsView());
        warehousesButton.setOnAction(event -> openWarehousesView());
        addWarehouseInStockButton.setOnAction(event -> openAddWarehouseInStockDialog());
        editWarehouseInStockButton.setOnAction(event -> openEditWarehouseInStockDialog());

        deleteWarehouseInStockButton.setOnAction(event -> deleteSelectedWarehouseInStock());
        deleteWarehouseInStockButton.setDisable(true);

        warehouseInStockTable.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newSelection) -> {
                    editWarehouseInStockButton.setDisable(newSelection == null);
                    deleteWarehouseInStockButton.setDisable(newSelection == null);
                }
        );

        updateStatistics();
    }

    /**
     * Удаление выбранного товара со склада.
     * <p>
     * Процесс удаления включает:
     * - Проверку выбора товара
     * - Отображение диалога подтверждения
     * - Отправку HTTP-запроса на удаление
     * - Обновление локального списка и статистики
     */
    @FXML
    private void deleteSelectedWarehouseInStock() {
        WarehouseInStockDTO selectedProduct = warehouseInStockTable.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            alertService.showErrorAlert("Ошибка", "Выберите товар на складе для удаления");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Подтверждение удаления");
        confirmDialog.setHeaderText("Вы уверены, что хотите удалить выбранный товар со склада?");
        confirmDialog.setContentText("Это действие нельзя будет отменить.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/warehouse-stocks/" + selectedProduct.getId()))
                        .DELETE()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 204) {
                    Platform.runLater(() -> {
                        masterInStockList.remove(selectedProduct);
                        alertService.showSuccessAlert("Товар успешно удален со склада");
                        updateStatistics();
                    });
                } else {
                    alertService.showErrorAlert("Ошибка", "Не удалось удалить товар со склада");
                }
            } catch (Exception e) {
                alertService.showErrorAlert("Ошибка сети", "Не удалось подключиться к серверу");
            }
        }
    }

    /**
     * Открытие диалога добавления нового товара на склад.
     * <p>
     * Выполняет следующие действия:
     * - Загрузка FXML формы добавления
     * - Настройка контроллера диалога
     * - Создание модального окна
     * - Установка слушателя успешного добавления
     */
    @FXML
    private void openAddWarehouseInStockDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/warehousesInStock/add-warehouseInStock.fxml"));
            Parent root = loader.load();

            AddWarehousesInStockController addController = loader.getController();

            addController.setOnWarehouseInStockAddedListener(newWarehouse -> {
                Platform.runLater(() -> {
                    if (masterInStockList != null) {
                        masterInStockList.add(newWarehouse);
                    }
                });
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Добавление товара на склад");
            Scene scene = new Scene(root, 350, 750);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            updateStatistics();
        } catch (IOException e) {
            alertService.showErrorAlert("Ошибка", "Не удалось открыть диалог добавления товара на склад");
        }
    }

    /**
     * Открытие диалога редактирования товара на складе.
     * <p>
     * Процесс редактирования включает:
     * - Проверку выбора товара
     * - Загрузку FXML формы редактирования
     * - Передачу данных выбранного товара
     * - Настройку слушателя обновления
     */
    @FXML
    private void openEditWarehouseInStockDialog() {
        WarehouseInStockDTO selectedWarehouse = warehouseInStockTable.getSelectionModel().getSelectedItem();

        if (selectedWarehouse == null) {
            alertService.showErrorAlert("Ошибка", "Выберите товар на складе для редактирования");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/warehousesInStock/edit-warehouseInStock.fxml"));
            Parent root = loader.load();

            EditWarehousesInStockController editController = loader.getController();
            editController.setWarehouseInStockToEdit(selectedWarehouse);

            editController.setOnWarehouseInStockUpdatedListener(updatedWarehouse -> {
                Platform.runLater(() -> {
                    if (masterInStockList != null) {
                        int index = masterInStockList.indexOf(selectedWarehouse);
                        if (index != -1) {
                            masterInStockList.set(index, updatedWarehouse);
                        }
                    }
                });
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Редактировать товар на складе");
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
     * Переход к представлению списка товаров.
     * <p>
     * Использует сервис LoadViewService для загрузки
     * представления списка товаров
     */
    @FXML
    private void openProductsView() {
        loadViewService.loadView("/fxml/products/products.fxml", warehouseInStockTable, "Информация о товарах");
    }

    /**
     * Переход к представлению списка складов.
     * <p>
     * Использует сервис LoadViewService для загрузки
     * представления списка складов
     */
    @FXML
    private void openWarehousesView() {
        loadViewService.loadView("/fxml/warehouses/warehouses.fxml", warehouseInStockTable,"Информация о складах");
    }

    /**
     * Очистка фильтров отображения товаров на складах.
     * <p>
     * Выполняет следующие действия:
     * - Очистка текстового поля фильтра
     * - Сброс фильтрации
     * - Обновление статистических показателей
     */
    @FXML
    private void clearFilters() {
        filterTextField.clear();
        filteredInStockList.setPredicate(warehouse -> true);
        updateStatistics();
    }

    /**
     * Возврат на главную страницу (приборную панель).
     * <p>
     * Использует сервис LoadViewService для перехода
     * на страницу dashboard
     */
    @FXML
    public void goToHomePage() {
        loadViewService.loadView("/fxml/dashboard.fxml", warehouseInStockTable, "Об авторе");
    }

    /**
     * Выход из системы.
     * <p>
     * Использует сервис LoadViewService для перехода
     * на страницу авторизации, effectively завершая сеанс
     */
    @FXML
    private void handleLogout() {
        loadViewService.loadView("/fxml/login.fxml", warehouseInStockTable, "Авторизация");
    }

    /**
     * Настройка столбцов таблицы товаров на складах.
     * <p>
     * Выполняет следующие действия:
     * - Привязка свойств модели к столбцам таблицы
     * - Установка возможности сортировки для каждого столбца
     * <p>
     * Используемые столбцы:
     * - ID товара
     * - ID склада
     * - Текущее количество
     * - Зарезервированное количество
     * - Локация
     */
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productIdColumn.setCellValueFactory(new PropertyValueFactory<>("productId"));
        warehouseIdColumn.setCellValueFactory(new PropertyValueFactory<>("warehouseId"));
        currentQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("currentQuantity"));
        reservedQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("reservedQuantity"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));

        idColumn.setSortable(true);
        productIdColumn.setSortable(true);
        warehouseIdColumn.setSortable(true);
        currentQuantityColumn.setSortable(true);
        reservedQuantityColumn.setSortable(true);
        locationColumn.setSortable(true);
    }

    /**
     * Открытие диалога расширенной фильтрации товаров на складах.
     * <p>
     * Предоставляет пользовательский интерфейс для:
     * - Выбора поля фильтрации
     * - Установки условий фильтра
     * - Добавления и удаления множественных фильтров
     * <p>
     * Основные компоненты диалога:
     * - ComboBox для выбора поля
     * - ComboBox для выбора условия
     * - Текстовое поле для ввода значения
     * - Список активных фильтров
     * - Кнопки управления фильтрами
     */
    private void showAdvancedFilterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Расширенный фильтр товаров на складах");
        dialog.setHeaderText("Настройте критерии поиска");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> fieldComboBox = new ComboBox<>();
        fieldComboBox.getItems().addAll(
                "ID товара",
                "ID склада",
                "Текущее количество",
                "Зарезервированное количество",
                "Локация"
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

        ListView<String> activeFiltersListView = new ListView<>();
        ObservableList<String> activeFiltersList = FXCollections.observableArrayList();
        activeFiltersListView.setItems(activeFiltersList);

        Button addFilterButton = new Button("Добавить фильтр");
        addFilterButton.setOnAction(e -> {
            if (validateFilterInput(fieldComboBox, operatorComboBox, valueTextField)) {
                String filterCondition = createFilterCondition(
                        fieldComboBox.getValue(),
                        operatorComboBox.getValue(),
                        valueTextField.getText()
                );
                activeFiltersList.add(filterCondition);

                fieldComboBox.setValue(null);
                operatorComboBox.setValue(null);
                valueTextField.clear();
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
     * Проверка необходимости ввода значения для фильтра.
     * <p>
     * Определяет, требуется ли значение для выбранного оператора.
     * <p>
     * @param operator Оператор сравнения
     * @return true, если для оператора требуется значение, иначе false
     */
    private boolean isValueRequired(String operator) {
        return !Arrays.asList("Пустое", "Не пустое").contains(operator);
    }

    /**
     * Валидация входных данных для фильтра.
     * <p>
     * Проверяет корректность выбора и заполнения полей фильтра:
     * - Наличие выбранного поля
     * - Наличие выбранного условия
     * - Наличие значения для фильтрации (при необходимости)
     * <p>
     * @param fieldComboBox ComboBox выбора поля
     * @param operatorComboBox ComboBox выбора условия
     * @param valueTextField Текстовое поле ввода значения
     * @return true, если входные данные корректны, иначе false
     */
    private boolean validateFilterInput(
            ComboBox<String> fieldComboBox,
            ComboBox<String> operatorComboBox,
            TextField valueTextField
    ) {
        if (fieldComboBox.getValue() == null) {
            alertService.showErrorAlert("Ошибка", "Выберите поле для фильтрации");
            return false;
        }

        if (operatorComboBox.getValue() == null) {
            alertService.showErrorAlert("Ошибка", "Выберите условие фильтрации");
            return false;
        }

        String value = valueTextField.getText().trim();
        String selectedOperator = operatorComboBox.getValue();

        if (isValueRequired(selectedOperator) && value.isEmpty()) {
            alertService.showErrorAlert("Ошибка", "Введите значение для фильтрации");
            return false;
        }

        return true;
    }

    /**
     * Создание строкового представления условия фильтра.
     * <p>
     * Формирует читаемое описание фильтра в формате:
     * "Поле Условие "Значение""
     * <p>
     * @param field Выбранное поле фильтрации
     * @param operator Выбранное условие фильтрации
     * @param value Значение для фильтрации
     * @return Строковое представление условия фильтра
     */
    private String createFilterCondition(
            String field,
            String operator,
            String value
    ) {
        return String.format("%s %s \"%s\"", field, operator, value);
    }

    /**
     * Применение фильтров к списку товаров на складах.
     * <p>
     * Устанавливает предикат для фильтрации списка товаров:
     * - Если фильтры отсутствуют, возвращает все элементы
     * - Проверяет соответствие каждого товара всем активным фильтрам
     * <p>
     * @param activeFiltersList Список активных условий фильтрации
     */
    private void applyFilters(ObservableList<String> activeFiltersList) {
        filteredInStockList.setPredicate(warehouse -> {
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
     * Проверка соответствия товара условиям фильтра.
     * <p>
     * Выполняет разбор условия фильтра и сравнение значений:
     * - Определение поля фильтрации
     * - Выбор оператора сравнения
     * - Преобразование и сравнение значений
     * <p>
     * Поддерживаемые типы фильтрации:
     * - ID товара
     * - ID склада
     * - Текущее количество
     * - Зарезервированное количество
     * - Локация
     *
     * @param warehouse Товар на складе для проверки
     * @param filterCondition Строковое условие фильтрации
     * @return true, если товар соответствует фильтру, иначе false
     */
    private boolean matchesFilter(WarehouseInStockDTO warehouse, String filterCondition) {
        String field, operator, value;
        String[] parts = filterCondition.split(" ", 6);
        if (Objects.equals(parts[0], "Локация")){
            field = parts[0];
            if (Objects.equals(parts[2], "или")){
                operator = parts[1] + " " + parts[2] + " " + parts[3];
                value = parts[4].replaceAll("^\"|\"$", "");
            }
            else {
                operator = parts[1];
                value = parts[2].replaceAll("^\"|\"$", "");
            }
        }
        else
        {
            field = parts[0] + " " + parts[1];
            if (Objects.equals(parts[3], "или")){
                operator = parts[2] + " " + parts[3] + " " + parts[4];
                value = parts[5].replaceAll("^\"|\"$", "");
            }
            else {
                operator = parts[2];
                value = parts[3].replaceAll("^\"|\"$", "");
            }
        }

        try {
            switch (field) {
                case "ID товара":
                    return compareLong(warehouse.getProductId(), operator, Long.valueOf(value));
                case "ID склада":
                    return compareLong(warehouse.getWarehouseId(), operator, Long.valueOf(value));
                case "Текущее количество":
                    return compareInteger(warehouse.getCurrentQuantity(), operator, Integer.parseInt(value));
                case "Зарезервированное количество":
                    return compareInteger(warehouse.getReservedQuantity(), operator, Integer.parseInt(value));
                case "Локация":
                    return compareString(warehouse.getLocation(), operator, value);
            }
        } catch (NumberFormatException e) {
            alertService.showErrorAlert("Ошибка", "Неверный формат числа: " + value);
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Неверный формат фильтра: " + filterCondition);
        }

        return false;
    }

    /**
     * Сравнение строковых значений с учетом регистра.
     * <p>
     * Поддерживаемые операторы:
     * - "Содержит": частичное совпадение
     * - "Равно": полное совпадение
     * <p>
     * @param warehouseValue Значение из объекта товара
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
     * Сравнение Long значений.
     * <p>
     * Поддерживаемые операторы:
     * - "Больше"
     * - "Меньше"
     * - "Больше или равно"
     * - "Меньше или равно"
     * - "Равно"
     * <p>
     * @param productValue Значение из объекта товара
     * @param operator Оператор сравнения
     * @param filterValue Значение фильтра
     * @return Результат сравнения
     */
    private boolean compareLong(Long productValue, String operator, Long filterValue) {
        return switch (operator) {
            case "Больше" -> productValue > filterValue;
            case "Меньше" -> productValue < filterValue;
            case "Больше или равно" -> productValue >= filterValue;
            case "Меньше или равно" -> productValue <= filterValue;
            case "Равно" -> Objects.equals(productValue, filterValue);
            default -> false;
        };
    }

    /**
     * Сравнение целочисленных значений.
     * <p>
     * Поддерживаемые операторы:
     * - "Больше"
     * - "Меньше"
     * - "Больше или равно"
     * - "Меньше или равно"
     * - "Равно"
     * <p>
     * @param warehouseValue Значение из объекта товара
     * @param operator Оператор сравнения
     * @param filterValue Значение фильтра
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
     * Обновление статистических показателей товаров на складах.
     * <p>
     * Вычисляет и отображает:
     * - Количество уникальных товаров
     * - Количество уникальных складов
     * - Общее количество товаров
     * - Количество зарезервированных товаров
     * <p>
     * При пустом списке устанавливает нулевые значения
     */
    private void updateStatistics() {
        if (masterInStockList == null || masterInStockList.isEmpty()) {
            uniqueProductCount.setText("Всего товаров: 0");
            uniqueWarehouseCount.setText("Всего уникальных складов: 0");
            totalProductQuantity.setText("Общее количество товаров: 0");
            totalReservedQuantity.setText("Зарезервировано товаров: 0");
            return;
        }

        long uniqueProducts = masterInStockList.stream()
                .map(WarehouseInStockDTO::getProductId)
                .distinct()
                .count();

        long uniqueWarehouses = masterInStockList.stream()
                .map(WarehouseInStockDTO::getWarehouseId)
                .distinct()
                .count();

        int totalProducts = masterInStockList.stream()
                .mapToInt(WarehouseInStockDTO::getCurrentQuantity)
                .sum();

        int totalReserved = masterInStockList.stream()
                .mapToInt(WarehouseInStockDTO::getReservedQuantity)
                .sum();

        uniqueProductCount.setText(String.format("Всего уникальных товаров: %d", uniqueProducts));
        uniqueWarehouseCount.setText(String.format("Всего уникальных складов: %d", uniqueWarehouses));
        totalProductQuantity.setText(String.format("Общее количество товаров: %d", totalProducts));
        totalReservedQuantity.setText(String.format("Зарезервировано товаров: %d", totalReserved));
    }

    /**
     * Простая фильтрация товаров по строке поиска.
     * <p>
     * Выполняет поиск по:
     * - ID товара
     * - ID склада
     * - Локации
     * <p>
     * @param warehouse Товар на складе для проверки
     * @param filter Строка поиска
     * @return true, если товар соответствует фильтру, иначе false
     */
    private boolean matchesSimpleFilter(WarehouseInStockDTO warehouse, String filter) {
        return warehouse.getProductId().toString().toLowerCase().contains(filter) ||
                warehouse.getWarehouseId().toString().toLowerCase().contains(filter) ||
                warehouse.getLocation().toLowerCase().contains(filter);
    }

    /**
     * Загрузка списка товаров на складах с сервера.
     * <p>
     * Выполняет:
     * - HTTP-запрос к серверу
     * - Десериализацию JSON-ответа
     * - Обновление локального списка
     * - Обновление статистики
     * <p>
     * Обработка ошибок:
     * - Отображение диалогов об ошибках
     * - Логирование исключительных ситуаций
     */
    private void loadWarehousesInStockFromServer() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/warehouse-stocks"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                List<WarehouseInStockDTO> warehouses = objectMapper.readValue(response.body(), new TypeReference<List<WarehouseInStockDTO>>() {});

                masterInStockList.clear();
                masterInStockList.addAll(warehouses);
                updateStatistics();
            } else {
                alertService.showErrorAlert("Ошибка загрузки", "Не удалось загрузить список товаров на складах");
            }
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка сети", "Не удалось подключиться к серверу");
        }
    }
}
