package com.dosboxeditor.ui;

import com.dosboxeditor.validation.ConfValidator;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A {@link TableCell} that renders a value string and switches to an
 * editable {@link TextField} on double-click, applying validation.
 */
public class EditableValueCell extends TableCell<com.dosboxeditor.model.ConfigEntry, String> {

    private final ConfValidator validator;
    private TextField textField;

    public EditableValueCell(ConfValidator validator) {
        this.validator = validator;
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            createTextField();
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            textField.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
        getStyleClass().remove("invalid");
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else if (isEditing()) {
            if (textField != null) textField.setText(item);
            setText(null);
            setGraphic(textField);
        } else {
            setText(item);
            setGraphic(null);
            getStyleClass().add("value-cell");
        }
    }

    private void createTextField() {
        textField = new TextField(getItem() == null ? "" : getItem());

        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                commitIfValid();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitIfValid();
            }
        });

        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Live validation: highlight field red if invalid
            var tableRow = getTableRow();
            if (tableRow != null && tableRow.getItem() != null) {
                String key = ((com.dosboxeditor.model.ConfigEntry) tableRow.getItem()).getKey();
                var result = validator.validate(key, newVal);
                if (!result.valid()) {
                    textField.getStyleClass().add("invalid");
                    Tooltip.install(textField, new Tooltip(result.message()));
                } else {
                    textField.getStyleClass().remove("invalid");
                    Tooltip.uninstall(textField, null);
                }
            }
        });
    }

    private void commitIfValid() {
        String newValue = textField.getText();
        var tableRow = getTableRow();
        if (tableRow != null && tableRow.getItem() != null) {
            String key = ((com.dosboxeditor.model.ConfigEntry) tableRow.getItem()).getKey();
            var result = validator.validate(key, newValue);
            if (!result.valid()) {
                Stage dialog = new Stage();
                dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                dialog.setTitle("Validation Warning");
                dialog.setResizable(false);

                // ── Icon + message ────────────────────────────────────────
                Label icon = new Label("⚠");
                icon.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 28px;");

                Label header = new Label("Invalid value for '" + key + "'");
                header.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: bold;");

                Label body = new Label(result.message());
                body.setStyle("-fx-text-fill: #d4d8dd; -fx-font-size: 12px; " +
                        "-fx-font-family: 'Consolas', monospace;");
                body.setWrapText(true);
                body.setMaxWidth(340);

                VBox message = new VBox(6, header, body);
                message.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                HBox top = new HBox(16, icon, message);
                top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                top.setPadding(new javafx.geometry.Insets(20, 24, 12, 20));

                // ── Divider ───────────────────────────────────────────────
                javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
                sep.setStyle("-fx-background-color: #2e3238;");

                // ── Buttons ───────────────────────────────────────────────
                String btnBase = "-fx-background-radius: 5; -fx-border-radius: 5; " +
                        "-fx-font-size: 12px; -fx-padding: 5 18; -fx-cursor: hand; " +
                        "-fx-border-width: 1;";

                Button saveBtn = new Button("Save Anyway");
                saveBtn.setStyle(btnBase +
                        "-fx-background-color: #2a2d32; -fx-text-fill: #d4d8dd; " +
                        "-fx-border-color: #2e3238;");
                saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(btnBase +
                        "-fx-background-color: #2c3038; -fx-text-fill: #d4d8dd; " +
                        "-fx-border-color: #2e3238;"));
                saveBtn.setOnMouseExited(e -> saveBtn.setStyle(btnBase +
                        "-fx-background-color: #2a2d32; -fx-text-fill: #d4d8dd; " +
                        "-fx-border-color: #2e3238;"));

                Button cancelBtn = new Button("Cancel");
                cancelBtn.setStyle(btnBase +
                        "-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa; " +
                        "-fx-border-color: #2a4a7f;");
                cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(btnBase +
                        "-fx-background-color: #24467a; -fx-text-fill: #60a5fa; " +
                        "-fx-border-color: #2a4a7f;"));
                cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(btnBase +
                        "-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa; " +
                        "-fx-border-color: #2a4a7f;"));

                HBox buttons = new HBox(10, saveBtn, cancelBtn);
                buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                buttons.setPadding(new javafx.geometry.Insets(12, 20, 16, 20));

                // ── Root layout ───────────────────────────────────────────
                VBox root = new VBox(top, sep, buttons);
                root.setStyle("-fx-background-color: #1a1c1e; -fx-border-color: #2e3238; -fx-border-width: 1;");

                Scene scene = new Scene(root, 400, 160);
                dialog.setScene(scene);

                // Wire buttons
                final boolean[] saveChosen = {false};
                saveBtn.setOnAction(e -> { saveChosen[0] = true;  dialog.close(); });
                cancelBtn.setOnAction(e -> { saveChosen[0] = false; dialog.close(); });
                dialog.setOnCloseRequest(e -> saveChosen[0] = false);

                dialog.showAndWait();

                if (saveChosen[0]) {
                    commitEdit(newValue);
                } else {
                    cancelEdit();
                }
                return;
            }
        }
        commitEdit(newValue);
    }
}
