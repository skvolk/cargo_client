package com.api.cargosimpleclient.Controllers.Products;

import com.api.cargosimpleclient.DTO.ProductDTO;
import com.api.cargosimpleclient.DTO.WarehouseInStockDTO;
import com.api.cargosimpleclient.Services.AlertService;
import com.api.cargosimpleclient.Services.LoadViewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Контроллер управления списком товаров.
 * <p>
 * Основные функции:
 * - Отображение таблицы товаров
 * - Фильтрация и поиск товаров
 * - Управление товарами (добавление, редактирование, удаление)
 * - Отслеживание складских остатков
 * - Статистический анализ товаров
 * <p>
 * Ключевые компоненты:
 * - Таблица товаров
 * - Фильтры и поиск
 * - Кнопки управления
 * - Статистические метки
 */
public class ProductsController {

    @FXML
    private TableView<ProductDTO> productsTable;

    @FXML
    private TableColumn<ProductDTO, Long> idColumn;

    @FXML
    private TableColumn<ProductDTO, String> articleNumberColumn;

    @FXML
    private TableColumn<ProductDTO, String> nameColumn;

    @FXML
    private TableColumn<ProductDTO, String> descriptionColumn;

    @FXML
    private TableColumn<ProductDTO, String> categoryColumn;

    @FXML
    private TableColumn<ProductDTO, String> manufacturerColumn;

    @FXML
    private TableColumn<ProductDTO, BigDecimal> purchasePriceColumn;

    @FXML
    private TableColumn<ProductDTO, BigDecimal> sellingPriceColumn;

    @FXML
    private TableColumn<ProductDTO, Integer> minStockLevelColumn;

    @FXML
    private TableColumn<ProductDTO, Integer> maxStockLevelColumn;

    @FXML
    private Button warehousesButton;

    @FXML
    private Button warehouseProductsButton;

    @FXML
    private Button addProductButton;

    @FXML
    private Button editProductButton;

    @FXML
    private Button deleteProductButton;

    @FXML
    private TextField filterTextField;

    @FXML
    private Button advancedFilterButton;

    @FXML
    private Label totalProductsLabel;

    @FXML
    private Label averagePriceLabel;

    @FXML
    private Label sellingPriceLabel;

    @FXML
    private Label categoriesLabel;

    @FXML
    private Label manufacturersLabel;

    @FXML
    private Label minStockLabel;

    @FXML
    private Label maxStockLabel;

    @FXML
    private Label marginLabel;

    private ObservableList<ProductDTO> masterProductList;

    private FilteredList<ProductDTO> filteredProductList;

    private final LoadViewService loadViewService = new LoadViewService();

    private final AlertService alertService = new AlertService();

    private final Map<Long, Integer> stockQuantityCache = new ConcurrentHashMap<>();

    /**
     * Инициализация контроллера при загрузке представления.
     * <p>
     * Выполняемые действия:
     * 1. Настройка списков данных
     * 2. Конфигурация таблицы
     * 3. Загрузка товаров с сервера
     * 4. Настройка обработчиков событий
     * 5. Первичное обновление статистики
     * 6. Настройка визуальных индикаторов складских остатков
     */
    @FXML
    public void initialize() {
        masterProductList = FXCollections.observableArrayList();
        filteredProductList = new FilteredList<>(masterProductList, p -> true);

        SortedList<ProductDTO> sortedProductList = new SortedList<>(filteredProductList);
        sortedProductList.comparatorProperty().bind(productsTable.comparatorProperty());

        productsTable.setItems(sortedProductList);

        setupTableColumns();
        loadProductsFromServer();

        editProductButton.setDisable(true);

        productsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    editProductButton.setDisable(newSelection == null);
                }
        );

        deleteProductButton.setDisable(true);

        productsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    deleteProductButton.setDisable(newSelection == null);
                }
        );

        advancedFilterButton.setOnAction(event -> showAdvancedFilterDialog());

        filterTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredProductList.setPredicate(product -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase().trim();
                return matchesSimpleFilter(product, lowerCaseFilter);
            });
        });

        warehousesButton.setOnAction(event -> openWarehousesView());
        warehouseProductsButton.setOnAction(event -> openWarehouseProductsView());
        addProductButton.setOnAction(event -> openAddProductDialog());
        deleteProductButton.setOnAction(event -> deleteProduct());

        updateStatistics();

        productsTable.setRowFactory(tv -> {
            TableRow<ProductDTO> row = new TableRow<ProductDTO>() {
                private Tooltip tooltip;

                @Override
                protected void updateItem(ProductDTO product, boolean empty) {
                    super.updateItem(product, empty);

                    if (empty || product == null) {
                        setStyle("");
                        if (tooltip != null) {
                            Tooltip.uninstall(this, tooltip);
                        }
                        return;
                    }

                    int totalStockQuantity = getTotalStockQuantity(product);

                    if (totalStockQuantity > product.getMaxStockLevel()) {
                        setStyle("-fx-background-color: #4CAF50;");
                        tooltip = new Tooltip(
                                String.format("Превышение максимального количества!\n" +
                                                "Текущее количество: %d\n" +
                                                "Максимальный уровень: %d",
                                        totalStockQuantity,
                                        product.getMaxStockLevel())
                        );
                        tooltip.setShowDelay(Duration.millis(100));
                        tooltip.setHideDelay(Duration.millis(500));
                        tooltip.setStyle("-fx-background-color: white; -fx-text-fill: black;");
                        Tooltip.install(this, tooltip);

                    } else if (totalStockQuantity < (product.getMinStockLevel() * 0.6)) {
                        setStyle("-fx-background-color: #FF8080;");
                        tooltip = new Tooltip(
                                String.format("Критически низкое количество!\n" +
                                                "Текущее количество: %d\n" +
                                                "Минимальный уровень: %d",
                                        totalStockQuantity,
                                        product.getMinStockLevel())
                        );
                        tooltip.setShowDelay(Duration.millis(100));
                        tooltip.setHideDelay(Duration.millis(500));
                        tooltip.setStyle("-fx-background-color: white; -fx-text-fill: black;");
                        Tooltip.install(this, tooltip);

                    } else if (totalStockQuantity < product.getMinStockLevel()) {
                        setStyle("-fx-background-color: #FFC266;");
                        tooltip = new Tooltip(
                                String.format("Низкое количество!\n" +
                                                "Текущее количество: %d\n" +
                                                "Минимальный уровень: %d",
                                        totalStockQuantity,
                                        product.getMinStockLevel())
                        );
                        tooltip.setShowDelay(Duration.millis(100));
                        tooltip.setHideDelay(Duration.millis(500));
                        tooltip.setStyle("-fx-background-color: white; -fx-text-fill: black;");
                        Tooltip.install(this, tooltip);

                    } else {
                        setStyle("");
                        if (tooltip != null) {
                            Tooltip.uninstall(this, tooltip);
                        }
                    }
                }
            };

            return row;
        });

        masterProductList.addListener((ListChangeListener<ProductDTO>) change -> {
            updateProductStockHighlighting();
        });
    }

    /**
     * Удаление выбранного товара из списка.
     * <p>
     * Последовательность действий:
     * 1. Проверка наличия выбранного товара
     * 2. Отображение диалога подтверждения
     * 3. При согласии - отправка запроса на удаление
     *
     * @throws NullPointerException если товар не выбран
     */
    @FXML
    private void deleteProduct() {
        ProductDTO selectedProduct = productsTable.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            alertService.showErrorAlert("Ошибка", "Выберите товар для удаления");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Вы уверены, что хотите удалить товар: " + selectedProduct.getName() + "?");
        alert.setContentText("Это действие нельзя будет отменить.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteProductFromServer(selectedProduct.getId());
            }
        });
    }

    /**
     * Открытие диалога добавления нового товара.
     * <p>
     * Основные этапы:
     * 1. Загрузка FXML-представления
     * 2. Настройка контроллера добавления
     * 3. Конфигурация модального окна
     * 4. Обработка события добавления товара
     * <p>
     * Особенности:
     * - Использование JavaFX модального окна
     * - Асинхронное добавление товара в список
     * - Обновление статистики после добавления
     */
    @FXML
    private void openAddProductDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/products/add-product.fxml"));
            Parent root = loader.load();

            AddProductController addController = loader.getController();

            addController.setOnProductAddedListener(newProduct -> {
                Platform.runLater(() -> {
                    if (masterProductList != null) {
                        masterProductList.add(newProduct);
                    }
                });
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Добавление товара");
            Scene scene = new Scene(root, 350, 920);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            updateStatistics();
        } catch (IOException e) {
            alertService.showErrorAlert("Ошибка", "Не удалось открыть диалог добавления товара");
        }
    }

    /**
     * Открытие диалога редактирования существующего товара.
     * <p>
     * Последовательность действий:
     * 1. Проверка выбора товара
     * 2. Загрузка FXML-представления
     * 3. Настройка контроллера редактирования
     * 4. Конфигурация модального окна
     * 5. Обработка события обновления товара
     * <p>
     * Особенности:
     * - Проверка выбора товара
     * - Асинхронное обновление списка
     * - Обновление статистики после редактирования
     *
     * @throws IllegalStateException если товар не выбран
     */
    @FXML
    private void openEditProductDialog() {
        ProductDTO selectedProduct = productsTable.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            alertService.showErrorAlert("Ошибка", "Выберите товар для редактирования");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/products/edit-product.fxml"));
            Parent root = loader.load();

            EditProductController editController = loader.getController();

            editController.setProductToEdit(selectedProduct);

            editController.setOnProductUpdatedListener(updatedProduct -> {
                Platform.runLater(() -> {
                    if (masterProductList != null) {
                        int index = masterProductList.indexOf(selectedProduct);
                        if (index != -1) {
                            masterProductList.set(index, updatedProduct);
                        }
                    }
                });
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Редактировать товар");
            Scene scene = new Scene(root, 500, 700);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            updateStatistics();
        } catch (IOException e) {
            alertService.showErrorAlert("Ошибка", "Не удалось открыть диалог редактирования");
        }
    }

    /**
     * Переход к представлению складов.
     * <p>
     * Действия:
     * - Загрузка представления складов
     * - Использование сервиса загрузки представлений
     *
     * @see LoadViewService
     */
    @FXML
    private void openWarehousesView() {
        loadViewService.loadView("/fxml/warehouses/warehouses.fxml", productsTable,"Информация о складах");
    }

    /**
     * Переход к представлению товаров на складах.
     * <p>
     * Действия:
     * - Загрузка представления товаров на складах
     * - Использование сервиса загрузки представлений
     *
     * @see LoadViewService
     */
    @FXML
    private void openWarehouseProductsView() {
        loadViewService.loadView("/fxml/warehousesInStock/warehouseInStock.fxml", productsTable,"Информация о товарах на складах");
    }

    /**
     * Возврат на главную страницу (панель управления).
     * <p>
     * Действия:
     * - Загрузка представления главной страницы
     * - Использование сервиса загрузки представлений
     *
     * @see LoadViewService
     */
    @FXML
    public void goToHomePage() {
        loadViewService.loadView("/fxml/dashboard.fxml", productsTable,"Об авторе");
    }

    /**
     * Выход из системы.
     * <p>
     * Последовательность действий:
     * 1. Загрузка страницы авторизации
     * 2. Закрытие текущего представления
     * <p>
     * Особенности:
     * - Использование сервиса загрузки представлений
     *
     * @see LoadViewService
     */
    @FXML
    private void handleLogout() {
        loadViewService.loadView("/fxml/login.fxml", productsTable,"Авторизация");
    }

    /**
     * Очистка фильтров товаров.
     * <p>
     * Действия:
     * 1. Очистка текстового поля фильтра
     * 2. Сброс фильтрации
     * 3. Обновление статистики
     * <p>
     * Особенности:
     * - Возврат к полному списку товаров
     * - Сброс всех applied фильтров
     */
    @FXML
    private void clearFilters() {
        filterTextField.clear();

        filteredProductList.setPredicate(product -> true);

        updateStatistics();
    }

    /**
     * Обновление визуального выделения товаров по складским остаткам.
     * <p>
     * Асинхронный процесс:
     * 1. Параллельный сбор данных о количестве товаров
     * 2. Обновление визуализации в основном потоке
     * <p>
     * Особенности:
     * - Использование CompletableFuture
     * - Потокобезопасное обновление интерфейса
     * - Кэширование количества товаров
     */
    private void updateProductStockHighlighting() {
        CompletableFuture.supplyAsync(() -> {
            Map<Long, Integer> stockQuantities = new HashMap<>();
            for (ProductDTO product : masterProductList) {
                stockQuantities.put(product.getId(), getTotalStockQuantity(product));
            }
            return stockQuantities;
        }).thenAcceptAsync(stockQuantities -> {
            Platform.runLater(() -> {
                productsTable.refresh();
            });
        }, Platform::runLater);
    }

    /**
     * Получение общего количества товара на складах.
     * <p>
     * Алгоритм работы:
     * 1. Проверка кэша складских остатков
     * 2. При отсутствии в кэше - запрос к серверу
     * 3. Суммирование количества товара по складам
     * 4. Кэширование результата
     * <p>
     * Особенности:
     * - Использование HTTP-клиента
     * - Кэширование результатов
     * - Обработка сетевых ошибок
     *
     * @param product Товар для проверки остатков
     * @return Общее количество товара на складах
     */
    private int getTotalStockQuantity(ProductDTO product) {
        try {
            if (stockQuantityCache.containsKey(product.getId())) {
                return stockQuantityCache.get(product.getId());
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/warehouse-stocks/product/" + product.getId()))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                List<WarehouseInStockDTO> warehouseStocks = objectMapper.readValue(
                        response.body(),
                        new TypeReference<List<WarehouseInStockDTO>>() {}
                );

                int totalQuantity = warehouseStocks.stream()
                        .mapToInt(WarehouseInStockDTO::getCurrentQuantity)
                        .sum();

                stockQuantityCache.put(product.getId(), totalQuantity);
                return totalQuantity;
            }
        } catch (Exception e) {
            alertService.showErrorAlert(
                    "Ошибка",
                    "Не удалось получить информацию о складских остатках для товара " + product.getName()
            );
        }
        return 0;
    }

    /**
     * Удаление товара с сервера.
     * <p>
     * Последовательность действий:
     * 1. Формирование HTTP DELETE-запроса
     * 2. Отправка запроса на удаление
     * 3. Обработка ответа сервера
     * 4. Обновление локального списка товаров
     * <p>
     * Сценарии обработки:
     * - Успешное удаление
     * - Ошибка на стороне сервера
     * - Сетевые ошибки
     *
     * @param productId Идентификатор товара для удаления
     */
    private void deleteProductFromServer(Long productId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/products/" + productId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                masterProductList.removeIf(product -> product.getId().equals(productId));
                updateStatistics();
                alertService.showSuccessAlert("Товар успешно удален");
            } else if (response.statusCode() == 400) {
                String errorMessage = response.body();
                alertService.showErrorAlert("Ошибка удаления", errorMessage);
            } else {
                alertService.showErrorAlert("Ошибка удаления", "Не удалось удалить товар");
            }
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка сети", "Не удалось подключиться к серверу: " + e.getMessage());
        }
    }

    /**
     * Обновление статистических показателей товаров.
     * <p>
     * Расчетные метрики:
     * - Общее количество товаров
     * - Средняя цена закупки и продажи
     * - Количество категорий и производителей
     * - Средние уровни складских запасов
     * - Средняя торговая маржа
     * <p>
     * Этапы обработки:
     * 1. Агрегация данных с использованием Stream API
     * 2. Расчет статистических показателей
     * 3. Обновление меток интерфейса
     * <p>
     * Особенности:
     * - Использование математических операций
     * - Обработка пустых коллекций
     * - Округление числовых значений
     */
    private void updateStatistics() {
        int totalProducts = masterProductList.size();

        BigDecimal totalPurchasePrice = masterProductList.stream()
                .map(ProductDTO::getPurchasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellingPrice = masterProductList.stream()
                .map(ProductDTO::getSellingPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averagePurchasePrice = totalProducts > 0
                ? totalPurchasePrice.divide(BigDecimal.valueOf(totalProducts), RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal averageSellingPrice = totalProducts > 0
                ? totalSellingPrice.divide(BigDecimal.valueOf(totalProducts), RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long categoryCount = masterProductList.stream()
                .map(ProductDTO::getCategory)
                .distinct()
                .count();

        long manufacturerCount = masterProductList.stream()
                .map(ProductDTO::getManufacturer)
                .distinct()
                .count();

        double averageMinStockLevel = masterProductList.stream()
                .mapToInt(ProductDTO::getMinStockLevel)
                .average()
                .orElse(0.0);

        double averageMaxStockLevel = masterProductList.stream()
                .mapToInt(ProductDTO::getMaxStockLevel)
                .average()
                .orElse(0.0);

        BigDecimal averageMargin = masterProductList.stream()
                .map(product -> {
                    BigDecimal purchasePrice = product.getPurchasePrice();
                    BigDecimal sellingPrice = product.getSellingPrice();
                    return sellingPrice.subtract(purchasePrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalProducts), RoundingMode.HALF_UP);

        totalProductsLabel.setText(String.format("Всего товаров: %d", totalProducts));

        averagePriceLabel.setText(String.format(
                "Средняя цена закупки: %.2f",
                averagePurchasePrice
        ));
        sellingPriceLabel.setText(String.format(
                "Средняя цена продажи: %.2f",
                averageSellingPrice
        ));
        categoriesLabel.setText(String.format(
                "Всего категорий: %d",
                categoryCount
        ));
        manufacturersLabel.setText(String.format(
                "Всего производителей: %d",
                manufacturerCount
        ));
        minStockLabel.setText(String.format(
                "Средний мин. уровень запасов: %.1f",
                averageMinStockLevel
        ));
        maxStockLabel.setText(String.format(
                "Средний макс. уровень запасов: %.1f",
                averageMaxStockLevel
        ));
        marginLabel.setText(String.format(
                "Средняя маржа: %.2f",
                averageMargin
        ));
    }

    /**
     * Простой фильтр товаров по тексту.
     * <p>
     * Критерии поиска:
     * - Название товара
     * - Артикульный номер
     * - Категория
     * - Производитель
     * <p>
     * Особенности:
     * - Нечувствительность к регистру
     * - Частичное совпадение
     *
     * @param product Товар для проверки
     * @param filter Текст фильтра
     * @return Результат соответствия фильтру
     */
    private boolean matchesSimpleFilter(ProductDTO product, String filter) {
        return product.getName().toLowerCase().contains(filter) ||
                product.getArticleNumber().toLowerCase().contains(filter) ||
                product.getCategory().toLowerCase().contains(filter) ||
                product.getManufacturer().toLowerCase().contains(filter);
    }

    /**
     * Открытие диалога расширенной фильтрации товаров.
     * <p>
     * Функциональность:
     * - Создание динамического диалогового окна
     * - Настройка множественных критериев фильтрации
     * - Интерактивное добавление и удаление фильтров
     * <p>
     * Основные компоненты:
     * - Выбор поля фильтрации
     * - Выбор условия сравнения
     * - Ввод значения для фильтрации
     * - Список активных фильтров
     * <p>
     * Этапы работы:
     * 1. Создание диалогового окна
     * 2. Настройка элементов управления
     * 3. Обработка пользовательского ввода
     * 4. Применение фильтров
     */
    private void showAdvancedFilterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Расширенный фильтр товаров");
        dialog.setHeaderText("Настройте критерии поиска");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> fieldComboBox = new ComboBox<>();
        fieldComboBox.getItems().addAll(
                "Название",
                "Артикул",
                "Категория",
                "Производитель",
                "Цена закупки",
                "Цена продажи",
                "Минимальный остаток",
                "Максимальный остаток"
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
     * Валидация введенных параметров фильтра.
     * <p>
     * Проверяемые условия:
     * - Наличие выбранного поля
     * - Наличие выбранного условия
     * - Наличие значения для фильтрации
     *
     * @param fieldComboBox Комбо-бокс выбора поля
     * @param operatorComboBox Комбо-бокс выбора условия
     * @param valueTextField Текстовое поле значения
     * @return Результат валидации
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

        if (valueTextField.getText().trim().isEmpty()) {
            alertService.showErrorAlert("Ошибка", "Введите значение для фильтра");
            return false;
        }

        return true;
    }

    /**
     * Создание текстового представления условия фильтра.
     * <p>
     * Формирование читаемой строки фильтра:
     * - Поле фильтрации
     * - Условие сравнения
     * - Значение
     *
     * @param field Поле фильтрации
     * @param operator Условие сравнения
     * @param value Значение фильтра
     * @return Форматированная строка условия
     */
    private String createFilterCondition(String field, String operator, String value) {
        return String.format("%s %s \"%s\"", field, operator, value);
    }

    /**
     * Применение фильтров к списку товаров.
     * <p>
     * Алгоритм фильтрации:
     * 1. Проверка каждого товара по всем активным фильтрам
     * 2. Товар проходит фильтрацию, если соответствует всем условиям
     *
     * @param activeFiltersList Список активных фильтров
     */
    private void applyFilters(ObservableList<String> activeFiltersList) {
        filteredProductList.setPredicate(product -> {
            if (activeFiltersList.isEmpty()) {
                return true;
            }

            for (String filterCondition : activeFiltersList) {
                if (!matchesFilter(product, filterCondition)) {
                    return false;
                }
            }

            return true;
        });
    }

    /**
     * Проверка соответствия товара условиям фильтра.
     * <p>
     * Типы сравнения:
     * - Строковые поля (содержит, равно)
     * - Числовые поля (больше, меньше, равно)
     * <p>
     * Поддерживаемые поля:
     * - Название
     * - Артикул
     * - Категория
     * - Производитель
     * - Цена закупки
     * - Цена продажи
     * - Минимальный/максимальный остаток
     *
     * @param product Проверяемый товар
     * @param filterCondition Условие фильтрации
     * @return Результат проверки
     */
    private boolean matchesFilter(ProductDTO product, String filterCondition) {
        String field, operator, value;
        String[] parts = filterCondition.split(" ", 6);
        if ((Objects.equals(parts[0], "Название")) || (Objects.equals(parts[0], "Артикул")) || (Objects.equals(parts[0], "Категория")) || (Objects.equals(parts[0], "Производитель"))){
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
        else
        {
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

        try {
            switch (field) {
                case "Название":
                    return compareString(product.getName(), operator, value);
                case "Артикул":
                    return compareString(product.getArticleNumber(), operator, value);
                case "Категория":
                    return compareString(product.getCategory(), operator, value);
                case "Производитель":
                    return compareString(product.getManufacturer(), operator, value);
                case "Цена закупки":
                    return compareDecimal(product.getPurchasePrice(), operator, new BigDecimal(value));
                case "Цена продажи":
                    return compareDecimal(product.getSellingPrice(), operator, new BigDecimal(value));
                case "Минимальный остаток":
                    return compareInteger(product.getMinStockLevel(), operator, Integer.parseInt(value));
                case "Максимальный остаток":
                    return compareInteger(product.getMaxStockLevel(), operator, Integer.parseInt(value));
            }
        } catch (NumberFormatException e) {
            alertService.showErrorAlert("Ошибка", "Неверный формат числа: " + value);
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Неверный формат фильтра: " + filterCondition);
        }

        return false;
    }

    /**
     * Сравнение строковых значений.
     * <p>
     * Поддерживаемые операторы:
     * - Содержит
     * - Равно
     *
     * @param productValue Значение товара
     * @param operator Условие сравнения
     * @param filterValue Значение фильтра
     * @return Результат сравнения
     */
    private boolean compareString(String productValue, String operator, String filterValue) {
        productValue = productValue.toLowerCase();
        filterValue = filterValue.toLowerCase();

        switch (operator) {
            case "Содержит":
                return productValue.contains(filterValue);
            case "Равно":
                return productValue.equals(filterValue);
            default:
                return false;
        }
    }

    /**
     * Сравнение десятичных значений (цен).
     * <p>
     * Поддерживаемые операторы:
     * - Больше
     * - Меньше
     * - Больше или равно
     * - Меньше или равно
     * - Равно
     *
     * @param productValue Значение товара
     * @param operator Условие сравнения
     * @param filterValue Значение фильтра
     * @return Результат сравнения
     */
    private boolean compareDecimal(BigDecimal productValue, String operator, BigDecimal filterValue) {
        switch (operator) {
            case "Больше":
                return productValue.compareTo(filterValue) > 0;
            case "Меньше":
                return productValue.compareTo(filterValue) < 0;
            case "Больше или равно":
                return productValue.compareTo(filterValue) >= 0;
            case "Меньше или равно":
                return productValue.compareTo(filterValue) <= 0;
            case "Равно":
                return productValue.compareTo(filterValue) == 0;
            default:
                return false;
        }
    }

    /**
     * Сравнение целочисленных значений (остатков).
     * <p>
     * Поддерживаемые операторы:
     * - Больше
     * - Меньше
     * - Больше или равно
     * - Меньше или равно
     * - Равно
     *
     * @param productValue Значение товара
     * @param operator Условие сравнения
     * @param filterValue Значение фильтра
     * @return Результат сравнения
     */
    private boolean compareInteger(int productValue, String operator, int filterValue) {
        switch (operator) {
            case "Больше":
                return productValue > filterValue;
            case "Меньше":
                return productValue < filterValue;
            case "Больше или равно":
                return productValue >= filterValue;
            case "Меньше или равно":
                return productValue <= filterValue;
            case "Равно":
                return productValue == filterValue;
            default:
                return false;
        }
    }

    /**
     * Настройка столбцов таблицы товаров.
     * <p>
     * Выполняемые действия:
     * - Привязка свойств DTO к столбцам
     * - Настройка сортировки
     * - Определение логики сравнения
     */
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        articleNumberColumn.setCellValueFactory(new PropertyValueFactory<>("articleNumber"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        manufacturerColumn.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        purchasePriceColumn.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        sellingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        minStockLevelColumn.setCellValueFactory(new PropertyValueFactory<>("minStockLevel"));
        maxStockLevelColumn.setCellValueFactory(new PropertyValueFactory<>("maxStockLevel"));

        nameColumn.setComparator((name1, name2) -> {
            if (name1 == null) return -1;
            if (name2 == null) return 1;
            return name1.compareToIgnoreCase(name2);
        });

        articleNumberColumn.setComparator((article1, article2) -> {
            if (article1 == null) return -1;
            if (article2 == null) return 1;
            return article1.compareToIgnoreCase(article2);
        });

        idColumn.setSortable(true);
        articleNumberColumn.setSortable(true);
        nameColumn.setSortable(true);
        descriptionColumn.setSortable(true);
        categoryColumn.setSortable(true);
        manufacturerColumn.setSortable(true);
        purchasePriceColumn.setSortable(true);
        sellingPriceColumn.setSortable(true);
        minStockLevelColumn.setSortable(true);
        maxStockLevelColumn.setSortable(true);
    }

    /**
     * Загрузка списка товаров с сервера.
     * <p>
     * Последовательность действий:
     * 1. Формирование HTTP-запроса
     * 2. Отправка GET-запроса
     * 3. Десериализация ответа
     * 4. Обновление локального списка
     * 5. Обновление статистики
     * <p>
     * Обработка ошибок:
     * - Сетевые ошибки
     * - Ошибки десериализации
     * - Ошибки ответа сервера
     */
    private void loadProductsFromServer() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/products"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                List<ProductDTO> products = objectMapper.readValue(response.body(), new TypeReference<List<ProductDTO>>() {});

                masterProductList.clear();
                masterProductList.addAll(products);
                updateStatistics();
            } else {
                alertService.showErrorAlert("Ошибка загрузки", "Не удалось загрузить список товаров");
            }
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка сети", "Не удалось подключиться к серверу");
        }
    }
}
