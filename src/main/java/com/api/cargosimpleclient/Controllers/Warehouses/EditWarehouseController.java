package com.api.cargosimpleclient.Controllers.Warehouses;

import com.api.cargosimpleclient.DTO.WarehouseDTO;
import com.api.cargosimpleclient.DTO.WarehouseStatus;
import com.api.cargosimpleclient.Services.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Контроллер для редактирования существующих складов.
 * <p>
 * Обеспечивает функциональность:
 * - Загрузка данных существующего склада
 * - Редактирование информации о складе
 * - Валидация введенных данных
 * - Отправка обновленных данных на сервер
 */
public class EditWarehouseController {

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

    private WarehouseUpdatedListener warehouseUpdatedListener;

    /**
     * Обработчик обновления информации о складе.
     * <p>
     * Выполняет следующие действия:
     * - Валидация и сбор данных из полей ввода
     * - Сериализация данных в JSON
     * - Отправка HTTP-запроса на обновление склада
     * - Обработка ответа сервера
     * - Уведомление слушателя об успешном обновлении
     */
    @FXML
    private void handleUpdateWarehouse() {
        try {
            validateAndUpdateWarehouseData();

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonWarehouse = objectMapper.writeValueAsString(warehouseDTO);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/api/warehouses/" + warehouseDTO.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonWarehouse))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                WarehouseDTO updatedWarehouse = objectMapper.readValue(response.body(), WarehouseDTO.class);

                if (warehouseUpdatedListener != null) {
                    Platform.runLater(() ->
                            warehouseUpdatedListener.onWarehouseUpdated(updatedWarehouse)
                    );
                }

                alertService.showSuccessAlert("Склад успешно обновлен");

                closeDialog();
            } else {
                alertService.showErrorAlert("Ошибка обновления", response.body());
            }
        } catch (IllegalArgumentException e) {
            alertService.showErrorAlert("Ошибка валидации", e.getMessage());
        } catch (Exception e) {
            alertService.showErrorAlert("Ошибка", "Не удалось обновить склад: " + e.getMessage());
        }
    }

    /**
     * Закрытие диалогового окна редактирования склада.
     * <p>
     * Получает текущую сцену из любого поля ввода и закрывает окно.
     */
    @FXML
    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    /**
     * Интерфейс для обратного вызова после обновления склада.
     * <p>
     * Позволяет внешним классам получать уведомление об успешном обновлении склада.
     */
    public interface WarehouseUpdatedListener {
        void onWarehouseUpdated(WarehouseDTO warehouse);
    }

    /**
     * Установка слушателя событий обновления склада.
     *
     * @param listener Слушатель для получения уведомлений об обновлении
     */
    public void setOnWarehouseUpdatedListener(WarehouseUpdatedListener listener) {
        this.warehouseUpdatedListener = listener;
    }

    /**
     * Подготовка склада к редактированию.
     * <p>
     * Устанавливает текущие значения склада в поля ввода:
     * - Загружает данные склада
     * - Инициализирует сервис уведомлений
     * - Заполняет поля текущими значениями
     * - Настраивает комбо-бокс статусов
     *
     * @param warehouse Склад, который будет редактироваться
     */
    public void setWarehouseToEdit(WarehouseDTO warehouse) {
        this.warehouseDTO = warehouse;
        alertService = new AlertService();

        nameField.setText(warehouse.getName());
        addressField.setText(warehouse.getAddress());
        contactPersonField.setText(warehouse.getContactPerson());
        phoneField.setText(warehouse.getPhone());
        emailField.setText(warehouse.getEmail());
        capacityField.setText(String.valueOf(warehouse.getCapacity()));
        statusComboBox.setValue(warehouse.getStatus());
        statusComboBox.getItems().addAll(WarehouseStatus.values());
        statusComboBox.setValue(warehouse.getStatus());
    }

    /**
     * Валидация и обновление данных склада.
     * <p>
     * Выполняет:
     * - Проверку и очистку значений полей ввода
     * - Парсинг числовых значений
     * - Проверку выбора статуса
     * - Обновление объекта DTO новыми значениями
     *
     * @throws IllegalArgumentException при нарушении условий валидации
     */
    private void validateAndUpdateWarehouseData() {
        String name = validateAndTrimField(nameField, "Название");
        String address = validateAndTrimField(addressField, "Адрес");
        String contactPerson = validateAndTrimField(contactPersonField, "Контактное лицо");
        String phone = validateAndTrimField(phoneField, "Телефон");
        String email = validateAndTrimField(emailField, "Email");

        int capacity = parseAndValidateInteger(capacityField);

        if (statusComboBox.getValue() == null) {
            throw new IllegalArgumentException("Статус склада должен быть выбран");
        }

        warehouseDTO.setName(name);
        warehouseDTO.setAddress(address);
        warehouseDTO.setContactPerson(contactPerson);
        warehouseDTO.setPhone(phone);
        warehouseDTO.setEmail(email);
        warehouseDTO.setCapacity(capacity);
        warehouseDTO.setStatus(statusComboBox.getValue());
    }

    /**
     * Валидация и очистка значения текстового поля.
     *
     * @param field Поле для валидации
     * @param fieldName Название поля для сообщения об ошибке
     * @return Очищенное значение поля
     * @throws IllegalArgumentException если поле пустое
     */
    private String validateAndTrimField(TextField field, String fieldName) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым");
        }
        return value;
    }

    /**
     * Парсинг и валидация целочисленного значения из текстового поля.
     *
     * @param field Поле для парсинга
     * @return Валидное целочисленное значение
     * @throws IllegalArgumentException при некорректном формате или отрицательном значении
     */
    private int parseAndValidateInteger(TextField field) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value < 0) {
                throw new IllegalArgumentException("Вместимость" + " не может быть меньше " + 0);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат для " + "Вместимость");
        }
    }
}
