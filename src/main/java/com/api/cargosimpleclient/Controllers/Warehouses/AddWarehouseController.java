package com.api.cargosimpleclient.Controllers.Warehouses;

import com.api.cargosimpleclient.DTO.WarehouseDTO;
import com.api.cargosimpleclient.DTO.WarehouseStatus;
import com.api.cargosimpleclient.Services.AlertService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Контроллер для добавления новых складов в систему.
 * <p>
 * Предоставляет функциональность:
 * - Ввод данных о новом складе
 * - Валидация введенных данных
 * - Отправка данных на сервер
 * - Обработка ответа сервера
 */
public class AddWarehouseController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField contactPersonField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField capacityField;

    @FXML
    private ComboBox<WarehouseStatus> statusComboBox;

    private WarehouseDTO warehouseDTO;

    private AlertService alertService;

    private WarehouseAddedListener warehouseAddedListener;

    /**
     * Обработчик сохранения нового склада.
     * <p>
     * Выполняет следующие действия:
     * - Сбор данных из полей ввода
     * - Подготовка JSON для отправки
     * - Отправка HTTP-запроса на сервер
     * - Обработка ответа сервера
     * - Уведомление слушателя о добавлении склада
     */
    @FXML
    private void handleSaveWarehouse() {
        try {
            collectWarehouseData();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            Map<String, Object> warehouseMap = objectMapper.convertValue(warehouseDTO, Map.class);
            warehouseMap.remove("id");

            String jsonWarehouse = objectMapper.writeValueAsString(warehouseMap);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/warehouses"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonWarehouse))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                WarehouseDTO savedWarehouse = objectMapper.readValue(response.body(), WarehouseDTO.class);

                if (warehouseAddedListener != null) {
                    warehouseAddedListener.onWarehouseAdded(savedWarehouse);
                }

                alertService.showSuccessAlert("Склад успешно добавлен");
                closeDialog();
            } else {
                alertService.showErrorAlert("Ошибка при добавлении склада", response.body());
            }
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Не удалось добавить склад: " + e.getMessage());
        }
    }

    /**
     * Закрытие диалогового окна добавления склада.
     * <p>
     * Получает текущую сцену из любого поля ввода и закрывает окно.
     */
    @FXML
    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    /**
     * Интерфейс для обратного вызова после добавления склада.
     * <p>
     * Позволяет внешним классам получать уведомление о успешном добавлении склада.
     */
    public interface WarehouseAddedListener {
        void onWarehouseAdded(WarehouseDTO warehouse);
    }

    /**
     * Установка слушателя событий добавления склада.
     *
     * @param listener Слушатель для получения уведомлений о добавлении
     */
    public void setOnWarehouseAddedListener(WarehouseAddedListener listener) {
        this.warehouseAddedListener = listener;
    }

    /**
     * Инициализация контроллера.
     * <p>
     * Выполняется автоматически после загрузки FXML:
     * - Создание нового объекта DTO
     * - Инициализация сервиса уведомлений
     * - Заполнение комбо-бокса статусов складов
     */
    public void initialize() {
        warehouseDTO = new WarehouseDTO();
        alertService = new AlertService();
        statusComboBox.getItems().addAll(WarehouseStatus.values());
    }

    /**
     * Сбор данных о складе из полей ввода.
     * <p>
     * Выполняет:
     * - Преобразование значений полей в соответствующие типы
     * - Установку значений в объект DTO
     * - Валидацию введенных данных
     *
     * @throws IllegalArgumentException при некорректных входных данных
     */
    private void collectWarehouseData() {
        warehouseDTO.setName(nameField.getText().trim());
        warehouseDTO.setAddress(addressField.getText().trim());
        warehouseDTO.setContactPerson(contactPersonField.getText().trim());
        warehouseDTO.setPhone(phoneField.getText().trim());
        warehouseDTO.setEmail(emailField.getText().trim());
        warehouseDTO.setCapacity(Integer.parseInt(capacityField.getText().trim()));
        warehouseDTO.setStatus(statusComboBox.getValue());

        validateFields();
    }

    /**
     * Валидация полей ввода склада.
     * <p>
     * Проверяет:
     * - Наличие названия склада
     * - Наличие адреса склада
     * - Корректность формата вместимости
     * - Выбор статуса склада
     * - Положительность вместимости
     *
     * @throws IllegalArgumentException при нарушении условий валидации
     */
    private void validateFields() {
        if (nameField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Название склада не может быть пустым");
        }

        if (addressField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Адрес склада не может быть пустым");
        }

        try {
            Integer.parseInt(capacityField.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат вместимости");
        }

        if (statusComboBox.getValue() == null) {
            throw new IllegalArgumentException("Статус склада должен быть выбран");
        }

        int capacity = Integer.parseInt(capacityField.getText().trim());
        if (capacity <= 0) {
            throw new IllegalArgumentException("Вместимость склада должна быть положительной");
        }
    }
}