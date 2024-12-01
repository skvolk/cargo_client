module com.api.cargosimpleclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires jdk.jfr;
    requires static lombok;
    requires okhttp3;
    requires com.google.gson;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires java.xml;
    requires annotations;

    opens com.api.cargosimpleclient to javafx.fxml;
    opens com.api.cargosimpleclient.DTO to com.google.gson;
    opens com.api.cargosimpleclient.Services to javafx.fxml;
    opens com.api.cargosimpleclient.Controllers to javafx.fxml;
    opens com.api.cargosimpleclient.Controllers.Products to javafx.fxml;
    opens com.api.cargosimpleclient.Controllers.Warehouses to javafx.fxml;
    opens com.api.cargosimpleclient.Controllers.WarehousesInStock to javafx.fxml;

    exports com.api.cargosimpleclient;
    exports com.api.cargosimpleclient.DTO;
    exports com.api.cargosimpleclient.Services;
    exports com.api.cargosimpleclient.Controllers;
    exports com.api.cargosimpleclient.Controllers.Products;
    exports com.api.cargosimpleclient.Controllers.WarehousesInStock;
    exports com.api.cargosimpleclient.Controllers.Warehouses;

}