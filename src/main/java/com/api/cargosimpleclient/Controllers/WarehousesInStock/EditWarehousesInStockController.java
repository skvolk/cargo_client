package com.api.cargosimpleclient.Controllers.WarehousesInStock;

import com.api.cargosimpleclient.DTO.WarehouseInStockDTO;
import com.api.cargosimpleclient.Services.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Контроллер для редактирования информации о товарах на складе.
 * <p>
 * Позволяет пользователю обновлять данные о товаре на складе и отправлять
 * обновления на сервер. Обрабатывает валидацию ввода и уведомления о
 * результате операции.
 */
public class EditWarehousesInStockController {

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

    private EditWarehousesInStockController.WarehouseInStockUpdatedListener warehouseInStockUpdatedListener;

    /**
     * Обрабатывает событие обновления товара на складе.
     * <p>
     * Выполняет валидацию введенных данных, формирует JSON-объект для
     * отправки на сервер и обрабатывает ответ. В случае успеха уведомляет
     * слушателя об обновлении и закрывает диалог.
     */
    @FXML
    private void handleUpdateWarehouseInStock() {
        try {
            validateAndUpdateWarehouseInStockData();

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonWarehouse = objectMapper.writeValueAsString(warehouseInStockDTO);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/warehouse-stocks/" + warehouseInStockDTO.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonWarehouse))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {

                WarehouseInStockDTO updatedWarehouse = objectMapper.readValue(response.body(), WarehouseInStockDTO.class);

                if (warehouseInStockUpdatedListener != null) {
                    Platform.runLater(() ->
                            warehouseInStockUpdatedListener.onWarehouseInStockUpdated(updatedWarehouse)
                    );
                }

                alertService.showSuccessAlert("Товар на складе успешно обновлен");

                closeDialog();
            } else {
                alertService.showErrorAlert("Ошибка обновления", response.body());
            }
        } catch (IllegalArgumentException e) {
            alertService.showErrorAlert("Ошибка валидации", e.getMessage());
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Не удалось обновить товар на складе: " + e.getMessage());
        }
    }

    /**
     * Закрывает диалог редактирования.
     */
    @FXML
    private void closeDialog() {
        Stage stage = (Stage) productIdField.getScene().getWindow();
        stage.close();
    }

    /**
     * Интерфейс для слушателя обновления товара на складе.
     */
    public interface WarehouseInStockUpdatedListener {
        void onWarehouseInStockUpdated(WarehouseInStockDTO warehouse);
    }

    /**
     * Устанавливает слушателя для обработки событий обновления товара на складе.
     *
     * @param listener Слушатель обновления
     */
    public void setOnWarehouseInStockUpdatedListener(EditWarehousesInStockController.WarehouseInStockUpdatedListener listener) {
        this.warehouseInStockUpdatedListener = listener;
    }

    /**
     * Устанавливает данные товара на складе для редактирования.
     *
     * @param warehouse Объект товара на складе для редактирования
     */
    public void setWarehouseInStockToEdit(WarehouseInStockDTO warehouse) {
        this.warehouseInStockDTO = warehouse;
        alertService = new AlertService();

        productIdField.setText(String.valueOf(warehouse.getProductId()));
        warehouseIdField.setText(String.valueOf(warehouse.getWarehouseId()));
        currentQuantityField.setText(String.valueOf(warehouse.getCurrentQuantity()));
        reservedQuantityField.setText(String.valueOf(warehouse.getReservedQuantity()));
        locationField.setText(warehouse.getLocation());
    }

    /**
     * Валидация и обновление данных о товаре на складе.
     * <p>
     * Проверяет введенные данные на корректность и обновляет объект
     * WarehouseInStockDTO с новыми значениями.
     * <p>
     * @throws IllegalArgumentException если одно из полей пустое
     */
    private void validateAndUpdateWarehouseInStockData() {
        String productId = validateAndTrimField(productIdField, "ID товара");
        String warehouseId = validateAndTrimField(warehouseIdField, "ID склада");
        String currentQuantity = validateAndTrimField(currentQuantityField, "Текущее количество");
        String reservedQuantity = validateAndTrimField(reservedQuantityField, "Зарезервированное количество");
        String email = validateAndTrimField(locationField, "Локация");

        warehouseInStockDTO.setProductId(Long.valueOf(productId));
        warehouseInStockDTO.setWarehouseId(Long.valueOf(warehouseId));
        warehouseInStockDTO.setCurrentQuantity(Integer.parseInt(currentQuantity));
        warehouseInStockDTO.setReservedQuantity(Integer.parseInt(reservedQuantity));
        warehouseInStockDTO.setLocation(email);
    }

    /**
     * Валидация и обрезка значения текстового поля.
     * <p>
     * Проверяет, что поле не пустое, и возвращает обрезанное значение.
     * <p>
     * @param field Поле для валидации
     * @param fieldName Название поля для отображения в сообщении об ошибке
     * @return Обрезанное значение поля
     * @throws IllegalArgumentException если поле пустое
     */
    private String validateAndTrimField(TextField field, String fieldName) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым");
        }
        return value;
    }
}