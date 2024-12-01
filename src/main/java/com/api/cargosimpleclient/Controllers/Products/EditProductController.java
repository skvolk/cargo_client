package com.api.cargosimpleclient.Controllers.Products;

import com.api.cargosimpleclient.DTO.ProductDTO;
import com.api.cargosimpleclient.Services.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Контроллер редактирования существующего товара.
 * <p>
 * Функциональности:
 * - Загрузка данных товара в форму редактирования
 * - Валидация и обновление информации о товаре
 * - Отправка HTTP-запроса на обновление товара
 * - Обработка ответа сервера
 * <p>
 * Ключевые возможности:
 * - Проверка корректности введенных данных
 * - Преобразование текстовых полей в соответствующие типы
 * - Уведомление слушателей об успешном обновлении
 */
public class EditProductController {

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

    private ProductUpdatedListener productUpdatedListener;

    /**
     * Обработчик события обновления товара.
     * <p>
     * Последовательность действий:
     * 1. Валидация и сбор данных из полей
     * 2. Сериализация объекта в JSON
     * 3. Отправка HTTP PUT-запроса
     * 4. Обработка ответа сервера
     * 5. Уведомление слушателей при успешном обновлении
     */
    @FXML
    private void handleUpdateProduct() {
        try {
            validateAndUpdateProductData();

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonProduct = objectMapper.writeValueAsString(productDTO);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/products/" + productDTO.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonProduct))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ProductDTO updatedProduct = objectMapper.readValue(response.body(), ProductDTO.class);

                if (productUpdatedListener != null) {
                    Platform.runLater(() ->
                            productUpdatedListener.onProductUpdated(updatedProduct)
                    );
                }

                alertService.showSuccessAlert("Товар успешно обновлен");

                closeDialog();
            } else {
                alertService.showErrorAlert("Ошибка обновления", response.body());
            }
        } catch (IllegalArgumentException e) {
            alertService.showErrorAlert("Ошибка валидации", e.getMessage());
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Не удалось обновить товар: " + e.getMessage());
        }
    }

    /**
     * Закрытие диалогового окна редактирования.
     * <p>
     * Действия:
     * - Получение текущего окна
     * - Закрытие окна
     */
    @FXML
    private void closeDialog() {
        Stage stage = (Stage) articleNumberField.getScene().getWindow();
        stage.close();
    }

    /**
     * Установка слушателя события обновления товара.
     *
     * @param listener Обработчик события обновления
     */
    public void setOnProductUpdatedListener(ProductUpdatedListener listener) {
        this.productUpdatedListener = listener;
    }

    /**
     * Интерфейс обратного вызова для уведомления об обновлении товара.
     * <p>
     * Позволяет:
     * - Информировать родительский компонент об изменении товара
     * - Синхронизировать состояние интерфейса после обновления
     */
    public interface ProductUpdatedListener {
        void onProductUpdated(ProductDTO product);
    }

    /**
     * Подготовка формы редактирования для конкретного товара.
     * <p>
     * Действия:
     * - Сохранение ссылки на редактируемый товар
     * - Инициализация сервиса оповещений
     * - Заполнение полей формы данными товара
     *
     * @param product Товар для редактирования
     */
    public void setProductToEdit(ProductDTO product) {
        this.productDTO = product;
        alertService = new AlertService();

        articleNumberField.setText(product.getArticleNumber());
        nameField.setText(product.getName());
        descriptionField.setText(product.getDescription());
        categoryField.setText(product.getCategory());
        manufacturerField.setText(product.getManufacturer());
        purchasePriceField.setText(product.getPurchasePrice().toString());
        sellingPriceField.setText(product.getSellingPrice().toString());
        minStockLevelField.setText(String.valueOf(product.getMinStockLevel()));
        maxStockLevelField.setText(String.valueOf(product.getMaxStockLevel()));
    }

    /**
     * Валидация и обновление данных товара.
     * <p>
     * Этапы обработки:
     * 1. Проверка обязательных текстовых полей
     * 2. Парсинг и проверка числовых значений
     * 3. Логический контроль (например, диапазон складских запасов)
     * 4. Обновление объекта DTO
     *
     * @throws IllegalArgumentException При нарушении правил валидации
     */
    private void validateAndUpdateProductData() {

        String articleNumber = validateAndTrimField(articleNumberField, "Артикул");
        String name = validateAndTrimField(nameField, "Название");
        String description = validateAndTrimField(descriptionField, "Описание");
        String category = validateAndTrimField(categoryField, "Категория");
        String manufacturer = validateAndTrimField(manufacturerField, "Производитель");

        BigDecimal purchasePrice = parseAndValidateBigDecimal(
                purchasePriceField, "Цена закупки");
        BigDecimal sellingPrice = parseAndValidateBigDecimal(
                sellingPriceField, "Цена продажи");

        int minStockLevel = parseAndValidateInteger(
                minStockLevelField, "Минимальный уровень запаса");
        int maxStockLevel = parseAndValidateInteger(
                maxStockLevelField, "Максимальный уровень запаса");

        if (minStockLevel > maxStockLevel) {
            throw new IllegalArgumentException(
                    "Минимальный уровень запаса не может превышать максимальный");
        }

        productDTO.setArticleNumber(articleNumber);
        productDTO.setName(name);
        productDTO.setDescription(description);
        productDTO.setCategory(category);
        productDTO.setManufacturer(manufacturer);
        productDTO.setPurchasePrice(purchasePrice);
        productDTO.setSellingPrice(sellingPrice);
        productDTO.setMinStockLevel(minStockLevel);
        productDTO.setMaxStockLevel(maxStockLevel);
    }

    /**
     * Валидация и очистка текстового поля.
     * <p>
     * Проверки:
     * - Непустое значение
     * - Удаление лишних пробелов
     *
     * @param field Текстовое поле для проверки
     * @param fieldName Название поля для сообщения об ошибке
     * @return Провалидированное значение
     *
     * @throws IllegalArgumentException При пустом значении
     */
    private String validateAndTrimField(TextField field, String fieldName) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым");
        }
        return value;
    }

    /**
     * Парсинг и валидация числового поля типа BigDecimal.
     * <p>
     * Проверки:
     * - Корректность числового формата
     * - Неотрицательность значения
     *
     * @param field Текстовое поле для парсинга
     * @param fieldName Название поля для сообщения об ошибке
     * @return Провалидированное значение
     *
     * @throws IllegalArgumentException При ошибках парсинга или отрицательном значении
     */
    private BigDecimal parseAndValidateBigDecimal(
            TextField field, String fieldName) {
        try {
            BigDecimal value = new BigDecimal(field.getText().trim());
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        fieldName + " не может быть меньше " + BigDecimal.ZERO);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Некорректный формат для " + fieldName);
        }
    }

    /**
     * Парсинг и валидация целочисленного поля.
     * <p>
     * Проверки:
     * - Корректность числового формата
     * - Неотрицательность значения
     *
     * @param field Текстовое поле для парсинга
     * @param fieldName Название поля для сообщения об ошибке
     * @return Провалидированное значение
     *
     * @throws IllegalArgumentException При ошибках парсинга или отрицательном значении
     */
    private int parseAndValidateInteger(
            TextField field, String fieldName) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value < 0) {
                throw new IllegalArgumentException(
                        fieldName + " не может быть меньше " + 0);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Некорректный формат для " + fieldName);
        }
    }
}