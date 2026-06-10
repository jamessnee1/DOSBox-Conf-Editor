package com.dosboxeditor.ui;

import com.dosboxeditor.App;
import com.dosboxeditor.model.ConfFile;
import com.dosboxeditor.parser.ConfParser;
import com.dosboxeditor.parser.ConfWriter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * Root layout for the DOSBox Config Editor.
 *
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │  Toolbar  (Open | Save | Save As | New) │
 * ├─────────────────────────────────────────┤
 * │  Search bar                             │
 * ├─────────────────────────────────────────┤
 * │  SectionPanel (scrollable accordion)    │
 * ├─────────────────────────────────────────┤
 * │  Status bar                             │
 * └─────────────────────────────────────────┘
 * </pre>
 */
public class MainWindow extends BorderPane {

    private final Stage stage;
    private final ConfParser parser = new ConfParser();
    private final ConfWriter writer = new ConfWriter();

    private ConfFile currentConf;
    private PreviewPanel previewPanel;
    private boolean previewVisible = false;;
    private final SectionPanel sectionPanel = new SectionPanel();

    private VBox centerBox;

    // Search
    private final TextField searchField = new TextField();

    // Status bar
    private final Label statusLabel = new Label("Open a .conf file to get started.");
    private final Label fileLabel   = new Label("");

    public MainWindow(Stage stage) {
        this.stage = stage;
        setTop(buildTop());
        setCenter(buildCenter());
        setBottom(buildStatusBar());

        sectionPanel.setOnModified(v -> markModified());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layout builders
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildTop() {
        VBox top = new VBox(0, buildToolbar(), buildSearchBar());
        return top;
    }

    private ToolBar buildToolbar() {
        Button openBtn   = toolbarButton("Open…",    "⌘O");
        Button saveBtn   = toolbarButton("Save",     "⌘S");
        Button saveAsBtn = toolbarButton("Save As…", "⌘⇧S");
        Button newBtn    = toolbarButton("New",      null);

        openBtn.setOnAction(e -> openFile());
        saveBtn.setOnAction(e -> saveFile());
        saveAsBtn.setOnAction(e -> saveFileAs());
        newBtn.setOnAction(e -> newFile());

        ToolBar bar = new ToolBar(newBtn, openBtn, new Separator(), saveBtn, saveAsBtn, new Separator(), buildHelpButton());
        bar.getStyleClass().add("tool-bar");
        return bar;
    }

    private Button buildHelpButton() {
        Button helpBtn = toolbarButton("Help", null);
        helpBtn.setOnAction(e -> showHelpDialog());
        return helpBtn;
    }

    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("DOSBox Config Editor — Help");
        alert.setHeaderText("DOSBox Config Editor - By James Snee");
        alert.initOwner(stage);

        String content = """
        OPENING & SAVING
        ────────────────
        New       Create a new config seeded with common sections.
        Open…     Open an existing .conf file from disk.
        Save      Save changes back to the same file.
        Save As…  Save a copy to a new location.

        EDITING
        ───────
        Double-click any value cell to edit it inline.
        Press Enter to confirm, Escape to cancel.
        Values for known keys are validated automatically —
        a warning appears if the value looks wrong.

        Hover over a key name to see allowed values
        and a short description of what it controls.

        SEARCH
        ──────
        Type in the search bar to filter keys and values
        across all sections in real time. Sections with
        no matching entries are hidden automatically.

        SECTIONS
        ────────
        Click a section header to collapse or expand it.

        GRAPHICS PREVIEW
        ────────────────
        The preview panel reflects your graphic settings live.
        machine  — controls colour depth simulation
        scaler   — controls pixel smoothing / sharpening
        aspect   — toggles aspect ratio correction

        COMMON DOSBOX KEYS
        ──────────────────
        memsize    RAM available to DOS (MB)
        cycles     CPU speed — try "auto" or a number like 3000
        core       CPU core — "auto" works for most games
        machine    Graphics hardware to emulate
        sbtype     Sound Blaster model
        mpu401     MIDI interface mode (uart for most games)
        """;

        alert.setContentText(content);

        // Widen the dialog so the monospaced content isn't squashed
        alert.getDialogPane().setMinWidth(520);
        alert.getDialogPane().setMinHeight(480);

        alert.showAndWait();
    }

    private HBox buildSearchBar() {
        searchField.setPromptText("🔍  Search keys and values…");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, o, n) -> refreshSections());

        Button clearBtn = new Button("✕");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5a6070; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> searchField.clear());

        HBox bar = new HBox(8, searchField, clearBtn);
        bar.getStyleClass().add("search-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 10, 6, 10));
        return bar;
    }

    private ScrollPane buildCenter() {
        ScrollPane scroll = new ScrollPane(sectionPanel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private HBox buildStatusBar() {
        statusLabel.getStyleClass().add("status-label");
        fileLabel.getStyleClass().add("status-label");
        fileLabel.setStyle("-fx-text-fill: #3a4050;");

        Button previewBtn = new Button("▲ Graphics Preview");
        previewBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #4ade80; " +
                        "-fx-border-color: transparent; -fx-font-size: 11px; " +
                        "-fx-font-family: 'Consolas', monospace; -fx-cursor: hand; -fx-padding: 0 8;");
        previewBtn.setOnMouseEntered(e -> previewBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #86efac; " +
                        "-fx-border-color: transparent; -fx-font-size: 11px; " +
                        "-fx-font-family: 'Consolas', monospace; -fx-cursor: hand; -fx-padding: 0 8;"));
        previewBtn.setOnMouseExited(e -> previewBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #4ade80; " +
                        "-fx-border-color: transparent; -fx-font-size: 11px; " +
                        "-fx-font-family: 'Consolas', monospace; -fx-cursor: hand; -fx-padding: 0 8;"));

        previewBtn.setOnAction(e -> {
            previewVisible = !previewVisible;
            if (previewVisible) {
                if (previewPanel == null) {
                    previewPanel = new PreviewPanel();
                    if (currentConf != null) previewPanel.updateFromConf(currentConf);
                }
                setBottom(null);
                VBox bottom = new VBox(previewPanel, buildStatusBarWith(previewBtn, "▼ Graphics Preview"));
                setBottom(bottom);
            } else {
                setBottom(buildStatusBarWith(previewBtn, "▲ Graphics Preview"));
            }
            previewBtn.setText(previewVisible ? "▼ Graphics Preview" : "▲ Graphics Preview");
        });

        return buildStatusBarWith(previewBtn, "▲ Graphics Preview");
    }

    private HBox buildStatusBarWith(Button previewBtn, String label) {
        previewBtn.setText(label);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(0, statusLabel, spacer, fileLabel, previewBtn);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setPadding(new javafx.geometry.Insets(4, 12, 4, 12));
        return bar;
    }

    private Button toolbarButton(String text, String tooltip) {
        Button btn = new Button(text);
        if (tooltip != null) btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File actions
    // ─────────────────────────────────────────────────────────────────────────

    private void openFile() {
        if (!confirmDiscardChanges()) return;

        FileChooser fc = confChooser("Open DOSBox Config");
        File file = fc.showOpenDialog(stage);
        if (file == null) return;
        if (previewPanel != null) previewPanel.updateFromConf(currentConf);

        try {
            currentConf = parser.parse(file);
            refreshSections();
            updateTitle();
            setStatus("Loaded " + file.getName(), "ok");
            fileLabel.setText(file.getAbsolutePath());
            previewPanel.updateFromConf(currentConf);
        } catch (IOException ex) {
            showError("Could not open file", ex.getMessage());
        }
    }

    private void saveFile() {
        if (currentConf == null) return;
        if (currentConf.getSourceFile() == null) { saveFileAs(); return; }
        try {
            writer.write(currentConf);
            updateTitle();
            setStatus("Saved " + currentConf.getSourceFile().getName(), "ok");
        } catch (IOException ex) {
            showError("Could not save file", ex.getMessage());
        }
    }

    private void saveFileAs() {
        if (currentConf == null) return;
        FileChooser fc = confChooser("Save DOSBox Config As…");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try {
            writer.write(currentConf, file);
            currentConf.setSourceFile(file);
            updateTitle();
            fileLabel.setText(file.getAbsolutePath());
            setStatus("Saved as " + file.getName(), "ok");
        } catch (IOException ex) {
            showError("Could not save file", ex.getMessage());
        }
    }

    private void newFile() {
        if (!confirmDiscardChanges()) return;
        currentConf = new ConfFile();
        currentConf.setLeadingComment("# DOSBox configuration file\n# Created by DOSBox Config Editor");
        // Seed with common sections
        seedNewConf();
        refreshSections();
        updateTitle();
        fileLabel.setText("(unsaved)");
        setStatus("New configuration created.", "ok");
    }

    public void handleCloseRequest(javafx.stage.WindowEvent event) {
        if (currentConf == null || !currentConf.isModified()) return;

        event.consume(); // prevent the window closing until we decide

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("Unsaved Changes");
        dialog.setResizable(false);

        // ── Icon + message ─────────────────────────────────────────────────
        Label icon = new Label("💾");
        icon.setStyle("-fx-font-size: 28px;");

        Label header = new Label("You have unsaved changes");
        header.setStyle("-fx-text-fill: #d4d8dd; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label body = new Label("Would you like to save \"" + currentConf.getDisplayName() + "\" before closing?");
        body.setStyle("-fx-text-fill: #8892a0; -fx-font-size: 12px;");
        body.setWrapText(true);
        body.setMaxWidth(320);

        VBox message = new VBox(6, header, body);
        message.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox top = new HBox(16, icon, message);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        top.setPadding(new javafx.geometry.Insets(20, 24, 12, 20));

        // ── Divider ────────────────────────────────────────────────────────
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color: #2e3238;");

        // ── Buttons ────────────────────────────────────────────────────────
        String btnBase = "-fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-font-size: 12px; -fx-padding: 5 18; -fx-cursor: hand; -fx-border-width: 1;";

        Button saveBtn = new Button("Save & Close");
        saveBtn.setStyle(btnBase +
                "-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa; -fx-border-color: #2a4a7f;");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(btnBase +
                "-fx-background-color: #24467a; -fx-text-fill: #60a5fa; -fx-border-color: #2a4a7f;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle(btnBase +
                "-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa; -fx-border-color: #2a4a7f;"));

        Button discardBtn = new Button("Discard & Close");
        discardBtn.setStyle(btnBase +
                "-fx-background-color: #2a2d32; -fx-text-fill: #f87171; -fx-border-color: #3a2020;");
        discardBtn.setOnMouseEntered(e -> discardBtn.setStyle(btnBase +
                "-fx-background-color: #2c3038; -fx-text-fill: #f87171; -fx-border-color: #3a2020;"));
        discardBtn.setOnMouseExited(e -> discardBtn.setStyle(btnBase +
                "-fx-background-color: #2a2d32; -fx-text-fill: #f87171; -fx-border-color: #3a2020;"));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(btnBase +
                "-fx-background-color: #2a2d32; -fx-text-fill: #d4d8dd; -fx-border-color: #2e3238;");
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(btnBase +
                "-fx-background-color: #2c3038; -fx-text-fill: #d4d8dd; -fx-border-color: #2e3238;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(btnBase +
                "-fx-background-color: #2a2d32; -fx-text-fill: #d4d8dd; -fx-border-color: #2e3238;"));

        HBox buttons = new HBox(10, saveBtn, discardBtn, cancelBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttons.setPadding(new javafx.geometry.Insets(12, 20, 16, 20));

        // ── Root layout ────────────────────────────────────────────────────
        VBox root = new VBox(top, sep, buttons);
        root.setStyle("-fx-background-color: #1a1c1e; -fx-border-color: #2e3238; -fx-border-width: 1;");

        Scene scene = new Scene(root, 420, 155);
        dialog.setScene(scene);

        saveBtn.setOnAction(e -> {
            dialog.close();
            saveFile();
            Platform.exit();
        });
        discardBtn.setOnAction(e -> {
            dialog.close();
            Platform.exit();
        });
        cancelBtn.setOnAction(e -> dialog.close());
        dialog.setOnCloseRequest(e -> {}); // X button = cancel

        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void seedNewConf() {
        // Programmatically add a few default sections
        addDefaultSection("dosbox",
            new String[]{"language", "machine", "captures", "memsize"},
            new String[]{"", "svga_s3", "capture", "16"});
        addDefaultSection("render",
            new String[]{"frameskip", "aspect", "scaler"},
            new String[]{"0", "false", "normal2x"});
        addDefaultSection("cpu",
            new String[]{"core", "cputype", "cycles", "cycleup", "cycledown"},
            new String[]{"auto", "auto", "3000", "500", "20"});
        addDefaultSection("mixer",
            new String[]{"nosound", "rate", "blocksize", "prebuffer"},
            new String[]{"false", "22050", "2048", "25"});
        addDefaultSection("midi",
                new String[]{"mpu401", "mididevice", "midiconfig"},
                new String[]{"intelligent", "default", ""});
        addDefaultSection("sblaster",
                new String[]{"sbtype", "sbbase", "irq", "dma", "hdma", "sbmixer", "oplmode", "oplemu", "oplrate"},
                new String[]{"sb16", "220", "7", "1", "5", "true", "auto", "default", "44100"});
        addDefaultSection("gus",
                new String[]{"gus", "gusrate", "gusbase", "gusirq", "gusdma", "ultradir"},
                new String[]{"false", "44100", "240", "5", "3", "C:\\ULTRASND"});
        addDefaultSection("speaker",
                new String[]{"pcspeaker", "pcrate", "tandy", "tandyrate", "disney"},
                new String[]{"false", "44100", "auto", "44100", "false"});
        addDefaultSection("joystick",
                new String[]{"joysticktype", "timed", "autofire", "swap34", "buttonwrap"},
                new String[]{"auto", "true", "false", "false", "false"});
        addDefaultSection("serial",
                new String[]{"serial1", "serial2", "serial3", "serial4"},
                new String[]{"dummy", "dummy", "disabled", "disabled"});
        addDefaultSection("dos",
                new String[]{"xms", "ems", "umb", "keyboardlayout"},
                new String[]{"true", "true", "true", "auto"});
        addDefaultSection("autoexec",
            new String[]{""},
            new String[]{""});
    }

    private void addDefaultSection(String name, String[] keys, String[] values) {
        com.dosboxeditor.model.ConfigSection s = new com.dosboxeditor.model.ConfigSection(name);
        for (int i = 0; i < keys.length; i++) {
            if (!keys[i].isEmpty())
                s.addEntry(new com.dosboxeditor.model.ConfigEntry(keys[i], values[i]));
        }
        currentConf.addSection(s);
    }

    private void refreshSections() {
        sectionPanel.loadConf(currentConf, searchField.getText());
    }

    private void markModified() {
        if (currentConf != null) {
            currentConf.setModified(true);
            updateTitle();
            setStatus("Unsaved changes", null);
            if (previewPanel != null) previewPanel.updateFromConf(currentConf);
        }
    }

    private void updateTitle() {
        if (currentConf == null) {
            stage.setTitle(App.APP_NAME);
            return;
        }
        String name = currentConf.getDisplayName();
        String mod  = currentConf.isModified() ? "*" : "";
        stage.setTitle(App.APP_NAME + " – " + name + mod);
    }

    private void setStatus(String msg, String styleClass) {
        statusLabel.getStyleClass().removeAll("ok", "error", "modified");
        statusLabel.setText(msg);
        if (styleClass != null) statusLabel.getStyleClass().add(styleClass);
    }

    private boolean confirmDiscardChanges() {
        if (currentConf != null && currentConf.isModified()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes.");
            alert.setContentText("Discard changes and continue?");
            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
            return alert.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
        }
        return true;
    }

    private void showError(String title, String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(detail);
        alert.showAndWait();
        setStatus("Error: " + detail, "error");
    }

    private FileChooser confChooser(String title) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("DOSBox Config (*.conf)", "*.conf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        return fc;
    }
}
