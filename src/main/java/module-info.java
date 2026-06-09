module com.dosboxeditor {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.dosboxeditor to javafx.fxml;
    opens com.dosboxeditor.ui to javafx.fxml;
    opens com.dosboxeditor.model to javafx.base;

    exports com.dosboxeditor;
    exports com.dosboxeditor.ui;
    exports com.dosboxeditor.model;
    exports com.dosboxeditor.parser;
    exports com.dosboxeditor.validation;
}
