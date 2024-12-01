package com.api.cargosimpleclient.DTO;

import javafx.beans.property.*;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) для представления информации о товаре.
 * <p>
 * Содержит детальную информацию о товаре с использованием
 * JavaFX Properties для обеспечения реактивности данных.
 *
 */
public class ProductDTO {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty articleNumber = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final StringProperty manufacturer = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> purchasePrice = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> sellingPrice = new SimpleObjectProperty<>();
    private final IntegerProperty minStockLevel = new SimpleIntegerProperty();
    private final IntegerProperty maxStockLevel = new SimpleIntegerProperty();

    public void setId(Long value) {
        id.set(value);
    }
    public Long getId() {
        return id.get();
    }

    public void setArticleNumber(String value) {
        articleNumber.set(value);
    }
    public String getArticleNumber() {
        return articleNumber.get();
    }


    public void setName(String value) {
        name.set(value);
    }
    public String getName() {
        return name.get();
    }

    public void setDescription(String value) {
        description.set(value);
    }
    public String getDescription() {
        return description.get();
    }

    public void setCategory(String value) {
        category.set(value);
    }
    public String getCategory() {
        return category.get();
    }

    public void setManufacturer(String value) {
        manufacturer.set(value);
    }
    public String getManufacturer() {
        return manufacturer.get();
    }

    public void setPurchasePrice(BigDecimal value) {
        purchasePrice.set(value);
    }
    public BigDecimal getPurchasePrice() {
        return purchasePrice.get();
    }

    public void setSellingPrice(BigDecimal value) {
        sellingPrice.set(value);
    }
    public BigDecimal getSellingPrice() {
        return sellingPrice.get();
    }

    public void setMinStockLevel(Integer value) {
        minStockLevel.set(value);
    }
    public Integer getMinStockLevel() {
        return minStockLevel.get();
    }

    public void setMaxStockLevel(Integer value) {
        maxStockLevel.set(value);
    }
    public Integer getMaxStockLevel() {
        return maxStockLevel.get();
    }

}
