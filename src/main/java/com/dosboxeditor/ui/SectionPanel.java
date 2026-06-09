package com.dosboxeditor.ui;

import com.dosboxeditor.model.ConfigEntry;
import com.dosboxeditor.model.ConfigSection;
import com.dosboxeditor.model.ConfFile;
import com.dosboxeditor.validation.ConfValidator;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Builds an {@link Accordion} showing every section as a {@link TitledPane},
 * each containing a {@link TableView} of its key-value entries.
 * <p>
 * Supports live search filtering and reports edits via a {@link Consumer}.
 */
public class SectionPanel extends VBox {

    private final ConfValidator validator = new ConfValidator();
    private final Accordion accordion = new Accordion();
    private Consumer<Void> onModified;

    public SectionPanel() {
        getChildren().add(accordion);
        VBox.setVgrow(accordion, javafx.scene.layout.Priority.ALWAYS);
        setPadding(Insets.EMPTY);
    }

    public void setOnModified(Consumer<Void> handler) {
        this.onModified = handler;
    }

    /**
     * Rebuild the accordion contents from {@code conf}, applying the given
     * search filter to key and value text.
     */
    public void loadConf(ConfFile conf, String filter) {
        accordion.getPanes().clear();
        if (conf == null) return;

        String lowerFilter = filter == null ? "" : filter.toLowerCase().trim();

        for (ConfigSection section : conf.getSections()) {
            if ("__root__".equals(section.getName())) continue;

            // Wrap in filtered list
            FilteredList<ConfigEntry> filtered = new FilteredList<>(section.getEntries(), entry -> {
                if (lowerFilter.isEmpty()) return !entry.getKey().isEmpty();
                return (entry.getKey().toLowerCase().contains(lowerFilter)
                     || entry.getValue().toLowerCase().contains(lowerFilter));
            });

            if (!lowerFilter.isEmpty() && filtered.isEmpty()) continue; // hide empty sections in search

            TableView<ConfigEntry> table = buildTable(filtered);
            TitledPane pane = new TitledPane("[" + section.getName() + "]", table);
            pane.setExpanded(true);
            accordion.getPanes().add(pane);
        }
    }

    private TableView<ConfigEntry> buildTable(FilteredList<ConfigEntry> entries) {
        TableView<ConfigEntry> table = new TableView<>(entries);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(computeTableHeight(entries.size()));

        // Recalculate height dynamically
        entries.addListener((javafx.collections.ListChangeListener<ConfigEntry>) c ->
            table.setPrefHeight(computeTableHeight(entries.size())));

        // ── Key column ────────────────────────────────────────────────────────
        TableColumn<ConfigEntry, String> keyCol = new TableColumn<>("Key");
        keyCol.setMinWidth(160);
        keyCol.setPrefWidth(220);
        keyCol.setCellValueFactory(cd -> cd.getValue().keyProperty());
        keyCol.setCellFactory(col -> {
            TableCell<ConfigEntry, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setTooltip(null);
                    if (!empty && item != null) {
                        getStyleClass().add("key-cell");
                        String tip = ConfValidator.getTooltip(item);
                        if (tip != null) setTooltip(new Tooltip(tip));
                    }
                }
            };
            return cell;
        });

        // ── Value column ──────────────────────────────────────────────────────
        TableColumn<ConfigEntry, String> valCol = new TableColumn<>("Value");
        valCol.setMinWidth(150);
        valCol.setPrefWidth(250);
        valCol.setCellValueFactory(cd -> cd.getValue().valueProperty());
        valCol.setCellFactory(col -> new EditableValueCell(validator));
        valCol.setOnEditCommit(event -> {
            event.getRowValue().setValue(event.getNewValue());
            if (onModified != null) onModified.accept(null);
        });

        // ── Comment column ────────────────────────────────────────────────────
        TableColumn<ConfigEntry, String> cmtCol = new TableColumn<>("Comment");
        cmtCol.setMinWidth(80);
        cmtCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getComment() != null ? cd.getValue().getComment().replace("\n", " ") : ""));
        cmtCol.setCellFactory(col -> {
            TableCell<ConfigEntry, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setTooltip(null);
                    if (empty || item == null || item.isBlank()) {
                        setText(null);
                    } else {
                        setText(item);
                        getStyleClass().add("comment-cell");
                        Tooltip tip = new Tooltip(item);
                        tip.setWrapText(true);
                        tip.setMaxWidth(320);
                        setTooltip(tip);
                    }
                }
            };
            return cell;
        });

        table.getColumns().addAll(keyCol, valCol, cmtCol);
        return table;
    }

    private double computeTableHeight(int rowCount) {
        // 32px per row + 30px header + small buffer
        return Math.max(80, rowCount * 32.0 + 34);
    }
}
