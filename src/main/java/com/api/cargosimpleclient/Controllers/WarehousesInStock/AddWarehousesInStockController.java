package com.api.cargosimpleclient.Controllers.WarehousesInStock;

import com.api.cargosimpleclient.DTO.WarehouseInStockDTO;
import com.api.cargosimpleclient.Services.AlertService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Контроллер для добавления новых товаров на склад.
 * <p>
 * Предоставляет функциональность:
 * - Ввод данных о товаре на складе
 * - Валидация введенных данных
 * - Отправка данных на сервер
 * - Обработка ответа сервера
 */
public class AddWarehousesInStockController {

    @FXML
    private TextField productIdField;

    @FXML
    private TextField warehouseIdField;

    @FXML
    private TextField currentQuantityField;

    @FXML
    private TextField reservedQuantityField;

    @FXML
    private TextField locationField;

    private WarehouseInStockDTO warehouseInStockDTO;

    private AlertService alertService;

    private AddWarehousesInStockController.WarehouseInStockAddedListener warehouseInStockAddedListener;

    /**
     * Обработчик сохранения товара на складе.
     * <p>
     * Выполняет следующие действия:
     * - Сбор данных из полей ввода
     * - Подготовка JSON для отправки
     * - Отправка HTTP-запроса на сервер
     * - Обработка ответа сервера
     * - Уведомление слушателя о добавлении товара
     */
    @FXML
    private void handleSaveWarehouseInStock() {
        try {
            collectWarehouseInStockData();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            Map<String, Object> warehouseMap = objectMapper.convertValue(warehouseInStockDTO, Map.class);
            warehouseMap.remove("id");

            String jsonWarehouse = objectMapper.writeValueAsString(warehouseMap);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/warehouse-stocks"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonWarehouse))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                WarehouseInStockDTO savedWarehouse = objectMapper.readValue(response.body(), WarehouseInStockDTO.class);

                if (warehouseInStockAddedListener != null) {
                    warehouseInStockAddedListener.onWarehouseAdded(savedWarehouse);
                }

                alertService.showSuccessAlert("Товар на склад успешно добавлен");
                closeDialog();
            } else {
                alertService.showErrorAlert("Ошибка при добавлении товара на склад", response.body());
            }
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Не удалось добавить товар на склад: " + e.getMessage());
        }
    }

    /**
     * Закрытие диалогового окна добавления товара.
     * <p>
     * Получает текущую сцену из любого поля ввода и закрывает окно.
     */
    @FXML
    private void closeDialog() {
        Stage stage = (Stage) productIdField.getScene().getWindow();
        stage.close();
    }

    /**
     * Инициализация контроллера.
     * <p>
     * Выполняется автоматически после загрузки FXML:
     * - Создание нового объекта DTO
     * - Инициализация сервиса уведомлений
     */
    public void initialize() {
        warehouseInStockDTO = new WarehouseInStockDTO();
        alertService = new AlertService();
    }

    /**
     * Интерфейс для обратного вызова после добавления товара на склад.
     * <p>
     * Позволяет внешним классам получать уведомление о успешном добавлении товара.
     */
    public interface WarehouseInStockAddedListener {
        void onWarehouseAdded(WarehouseInStockDTO warehouseInStockDTO);
    }

    /**
     * Установка слушателя событий добавления товара на склад.
     *
     * @param listener Слушатель для получения уведомлений о добавлении
     */
    public void setOnWarehouseInStockAddedListener(AddWarehousesInStockController.WarehouseInStockAddedListener listener) {
        this.warehouseInStockAddedListener = listener;
    }

    /**
     * Сбор данных о товаре на складе из полей ввода.
     * <p>
     * Выполняет:
     * - Преобразование значений полей в соответствующие типы
     * - Установку значений в объект DTO
     * - Валидацию введенных данных
     *
     * @throws IllegalArgumentException при некорректных входных данных
     */
    private void collectWarehouseInStockData() {
        warehouseInStockDTO.setProductId(Long.valueOf(productIdField.getText().trim()));
        warehouseInStockDTO.setWarehouseId(Long.valueOf(warehouseIdField.getText().trim()));
        warehouseInStockDTO.setCurrentQuantity(Integer.parseInt(currentQuantityField.getText().trim()));
        warehouseInStockDTO.setReservedQuantity(Integer.parseInt(reservedQuantityField.getText().trim()));
        warehouseInStockDTO.setLocation(locationField.getText().trim());

        validateFields();
    }

    /**
     * Валидация полей ввода.
     * <p>
     * Проверяет:
     * - Наличие ID товара
     * - Наличие ID склада
     *
     * @throws IllegalArgumentException при отсутствии обязательных полей
     */
    private void validateFields() {
        if (productIdField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Id товара не может быть пустым");
        }

        if (warehouseIdField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Id склада не может быть пустым");
        }
    }
}
