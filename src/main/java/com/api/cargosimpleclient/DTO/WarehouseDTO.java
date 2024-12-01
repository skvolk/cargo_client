package com.api.cargosimpleclient.DTO;

import javafx.beans.property.*;

/**
 * DTO (Data Transfer Object) для представления информации о складе.
 * <p>
 * Содержит детальную информацию о складском помещении с использованием
 * JavaFX Properties для обеспечения реактивности данных.
 *
 */
public class WarehouseDTO {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty contactPerson = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final IntegerProperty capacity = new SimpleIntegerProperty();
    private final ObjectProperty<WarehouseStatus> status = new SimpleObjectProperty<>();

    public void setId(Long value) {
        id.set(value);
    }
    public Long getId() {
        return id.get();
    }

    public void setName(String value) {
        name.set(value);
    }
    public String getName() {
        return name.get();
    }

    public void setAddress(String value) {
        address.set(value);
    }
    public String getAddress() {
        return address.get();
    }

    public void setContactPerson(String value) {
        contactPerson.set(value);
    }
    public String getContactPerson() {
        return contactPerson.get();
    }

    public void setPhone(String value) {
        phone.set(value);
    }
    public String getPhone() {
        return phone.get();
    }

    public void setEmail(String value) {
        email.set(value);
    }
    public String getEmail() {
        return email.get();
    }

    public void setCapacity(Integer value) {
        capacity.set(value);
    }
    public Integer getCapacity() {
        return capacity.get();
    }

    public void setStatus(WarehouseStatus value) {
        status.set(value);
    }
    public WarehouseStatus getStatus() {
        return status.get();
    }
}