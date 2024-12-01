package com.api.cargosimpleclient.Controllers.Products;

import com.api.cargosimpleclient.DTO.ProductDTO;
import com.api.cargosimpleclient.Services.AlertService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Контроллер для добавления нового товара в систему.
 * <p>
 * Функциональность:
 * - Сбор данных о товаре из пользовательского интерфейса
 * - Валидация введенных данных
 * - Отправка HTTP-запроса на сервер для создания товара
 * - Обработка ответа сервера
 * <p>
 * Основные компоненты:
 * - Текстовые поля для ввода характеристик товара
 * - Механизм валидации полей
 * - Слушатель события добавления товара
 */
public class AddProductController {

    @FXML
    private TextField articleNumberField;

    @FXML
    private TextField nameField;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField categoryField;

    @FXML
    private TextField manufacturerField;

    @FXML
    private TextField purchasePriceField;

    @FXML
    private TextField sellingPriceField;

    @FXML
    private TextField minStockLevelField;

    @FXML
    private TextField maxStockLevelField;

    private ProductDTO productDTO;

    private AlertService alertService;

    private ProductAddedListener productAddedListener;

    /**
     * Обработчик события сохранения товара.
     * <p>
     * Последовательность действий:
     * 1. Сбор данных из полей ввода
     * 2. Валидация введенных данных
     * 3. Подготовка JSON для отправки
     * 4. Отправка HTTP-запроса на сервер
     * 5. Обработка ответа сервера
     */
    @FXML
    private void handleSaveProduct() {
        try {
            collectProductData();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            Map<String, Object> productMap = objectMapper.convertValue(productDTO, Map.class);
            productMap.remove("id");


            String jsonProduct = objectMapper.writeValueAsString(productMap);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/products"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonProduct))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                ProductDTO savedProduct = objectMapper.readValue(response.body(), ProductDTO.class);

                if (productAddedListener != null) {
                    productAddedListener.onProductAdded(savedProduct);
                }

                alertService.showSuccessAlert("Товар успешно добавлен");
                closeDialog();
            } else {
                alertService.showErrorAlert("Ошибка при добавлении товара", response.body());
            }
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Не удалось добавить товар: " + e.getMessage());
        }
    }

    /**
     * Закрытие диалогового окна добавления товара.
     * <p>
     * Действия:
     * - Получение текущего окна
     * - Закрытие окна
     */
    @FXML
    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    /**
     * Интерфейс для обратного вызова после успешного добавления товара.
     * <p>
     * Позволяет:
     * - Уведомлять родительский компонент о создании нового товара
     * - Обновлять списки или таблицы с товарами
     */
    public interface ProductAddedListener {
        void onProductAdded(ProductDTO product);
    }

    /**
     * Установка слушателя события добавления товара.
     *
     * @param listener Обработчик события добавления
     */
    public void setOnProductAddedListener(ProductAddedListener listener) {
        this.productAddedListener = listener;
    }

    /**
     * Инициализация контроллера при загрузке.
     * <p>
     * Выполняет подготовительные действия:
     * - Создание нового экземпляра DTO товара
     * - Инициализация сервиса оповещений
     */
    public void initialize() {
        productDTO = new ProductDTO();
        alertService = new AlertService();
    }

    /**
     * Сбор данных о товаре из полей ввода.
     * <p>
     * Преобразование текстовых значений:
     * - Очистка от пробелов
     * - Конвертация в соответствующие типы данных
     * - Заполнение объекта DTO
     *
     * @throws IllegalArgumentException При ошибках преобразования или валидации
     */
    private void collectProductData() {
        productDTO.setArticleNumber(articleNumberField.getText().trim());
        productDTO.setName(nameField.getText().trim());
        productDTO.setDescription(descriptionField.getText().trim());
        productDTO.setCategory(categoryField.getText().trim());
        productDTO.setManufacturer(manufacturerField.getText().trim());

        productDTO.setPurchasePrice(new BigDecimal(purchasePriceField.getText().trim()));
        productDTO.setSellingPrice(new BigDecimal(sellingPriceField.getText().trim()));
        productDTO.setMinStockLevel(Integer.parseInt(minStockLevelField.getText().trim()));
        productDTO.setMaxStockLevel(Integer.parseInt(maxStockLevelField.getText().trim()));

        validateFields();
    }

    /**
     * Валидация полей ввода товара.
     * <p>
     * Проверки:
     * - Обязательность заполнения ключевых полей
     * - Корректность числовых значений
     * - Логические ограничения (например, неотрицательность цен)
     *
     * @throws IllegalArgumentException При нарушении правил валидации
     */
    private void validateFields() {
        if (nameField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Название товара не может быть пустым");
        }

        if (articleNumberField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Артикул не может быть пустым");
        }

        try {
            new BigDecimal(purchasePriceField.getText().trim());
            new BigDecimal(sellingPriceField.getText().trim());
            Integer.parseInt(minStockLevelField.getText().trim());
            Integer.parseInt(maxStockLevelField.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат числовых полей");
        }

        BigDecimal purchasePrice = new BigDecimal(purchasePriceField.getText().trim());
        BigDecimal sellingPrice = new BigDecimal(sellingPriceField.getText().trim());

        if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Цена закупки не может быть отрицательной");
        }

        if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Цена продажи не может быть отрицательной ");
        }
    }
}