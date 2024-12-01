package com.api.cargosimpleclient.DTO;

import javafx.beans.property.*;

/**
 * DTO (Data Transfer Object) для представления товара на складе.
 * <p>
 * Содержит информацию о количестве и расположении товара
 * в конкретном складском помещении с использованием JavaFX Properties.
 *
 */
public class WarehouseInStockDTO {

    private final LongProperty id = new SimpleLongProperty();
    private final LongProperty productId = new SimpleLongProperty();
    private final LongProperty warehouseId = new SimpleLongProperty();
    private final IntegerProperty currentQuantity = new SimpleIntegerProperty();
    private final IntegerProperty reservedQuantity = new SimpleIntegerProperty();
    private final StringProperty location = new SimpleStringProperty();

    public void setId(Long value){
        id.set(value);
    }

    public Long getId() {
        return id.get();
    }

    public void setProductId(Long value){
        productId.set(value);
    }

    public Long getProductId() {
        return productId.get();
    }

    public Long getWarehouseId() {
        return warehouseId.get();
    }

    public int getCurrentQuantity() {
        return currentQuantity.get();
    }

    public int getReservedQuantity() {
        return reservedQuantity.get();
    }

    public String getLocation() {
        return location.get();
    }

    public void setWarehouseId(Long value){
        warehouseId.set(value);
    }

    public void setCurrentQuantity(int value){
        currentQuantity.set(value);
    }

    public void setReservedQuantity(int value){
        reservedQuantity.set(value);
    }

    public void setLocation(String value){
        location.set(value);
    }

}
