package com.dosboxeditor.ui;

import com.dosboxeditor.model.ConfFile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.io.InputStream;

/**
 * Preview panel showing a bundled title screen image with visual filters
 * applied based on the current DOSBox graphics settings.
 *
 * Reacts to: machine, scaler, aspect
 */
public class PreviewPanel extends VBox {

    private final ImageView imageView = new ImageView();
    private final Canvas scanlineOverlay = new Canvas();
    private final StackPane imageStack = new StackPane();
    private final Label settingsLabel = new Label();

    private Image originalImage;


    // Current settings
    private String machine = "svga_s3";
    private String scaler  = "none";
    private boolean aspect = false;
    private String output          = "surface";
    private boolean fullscreen     = false;
    private boolean fulldouble     = false;
    private String windowRes       = "original";

    public PreviewPanel() {
        setStyle("-fx-background-color: #0d0e10; -fx-border-color: #2e3238; -fx-border-width: 1 0 0 0;");
        setPrefHeight(420);
        setMinHeight(350);
        setPadding(new Insets(10, 16, 10, 16));
        setSpacing(8);

        buildImageArea();
        buildSettingsBar();
        loadBundledImage();
    }

    private void buildHeader() {
        Label title = new Label("Graphics Preview");
        title.setStyle("-fx-text-fill: #4ade80; -fx-font-family: 'Consolas', monospace;" +
                " -fx-font-size: 12px; -fx-font-weight: bold;");

        Button minimizeBtn = new Button("─");
        minimizeBtn.setStyle(
                "-fx-background-color: #2a2d32; -fx-text-fill: #d4d8dd; " +
                        "-fx-border-color: #2e3238; -fx-border-radius: 3; -fx-background-radius: 3; " +
                        "-fx-font-size: 11px; -fx-padding: 0 6 2 6; -fx-cursor: hand;");
        minimizeBtn.setTooltip(new Tooltip("Minimise preview"));

        minimizeBtn.setOnMouseEntered(e -> minimizeBtn.setStyle(
                "-fx-background-color: #2c3038; -fx-text-fill: #4ade80; " +
                        "-fx-border-color: #4ade80; -fx-border-radius: 3; -fx-background-radius: 3; " +
                        "-fx-font-size: 11px; -fx-padding: 0 6 2 6; -fx-cursor: hand;"));
        minimizeBtn.setOnMouseExited(e -> minimizeBtn.setStyle(
                "-fx-background-color: #2a2d32; -fx-text-fill: #d4d8dd; " +
                        "-fx-border-color: #2e3238; -fx-border-radius: 3; -fx-background-radius: 3; " +
                        "-fx-font-size: 11px; -fx-padding: 0 6 2 6; -fx-cursor: hand;"));

        final boolean[] minimised = {false};
        minimizeBtn.setOnAction(e -> {
            minimised[0] = !minimised[0];
            if (minimised[0]) {
                imageStack.setVisible(false);
                imageStack.setManaged(false);
                settingsLabel.setVisible(false);
                settingsLabel.setManaged(false);
                setPrefHeight(5);
                minimizeBtn.setText("□");
                minimizeBtn.setTooltip(new Tooltip("Restore preview"));
            } else {
                imageStack.setVisible(true);
                imageStack.setManaged(true);
                settingsLabel.setVisible(true);
                settingsLabel.setManaged(true);
                setPrefHeight(420);
                imageView.setFitHeight(380);
                minimizeBtn.setText("─");
                minimizeBtn.setTooltip(new Tooltip("Minimise preview"));
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, title, spacer, minimizeBtn);
        header.setMinHeight(1);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().add(header);
    }

    private void buildImageArea() {
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(380);
        imageView.setSmooth(true);

        scanlineOverlay.setMouseTransparent(true);
        scanlineOverlay.setOpacity(0.18);

        imageStack.getChildren().addAll(imageView, scanlineOverlay);
        imageStack.setAlignment(Pos.CENTER);
        imageStack.setStyle("-fx-background-color: #000000;");
        VBox.setVgrow(imageStack, Priority.ALWAYS);

        getChildren().add(imageStack);
    }

    private void buildSettingsBar() {
        settingsLabel.setStyle("-fx-text-fill: #3a4050; -fx-font-family: 'Consolas'," +
                " monospace; -fx-font-size: 10px;");
        getChildren().add(settingsLabel);
    }

    // ── Image loading ─────────────────────────────────────────────────────────

    /**
     * Loads the bundled image from the classpath.
     * Place your image at:
     *   src/main/resources/com/dosboxeditor/preview.png
     */
    private void loadBundledImage() {
        InputStream is = getClass().getResourceAsStream("/com/dosboxeditor/preview.png");
        if (is == null) {
            // Fallback: try jpg
            is = getClass().getResourceAsStream("/com/dosboxeditor/preview.jpg");
        }
        if (is == null) {
            settingsLabel.setText("No preview image found — add preview.png to resources/com/dosboxeditor/");
            return;
        }
        originalImage = new Image(is);
        imageView.setImage(originalImage);
        applyEffects();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void updateFromConf(ConfFile conf) {
        if (conf == null) return;

        conf.findSection("render").ifPresent(s -> {
            s.findEntry("scaler").ifPresent(e -> scaler = e.getValue());
            s.findEntry("aspect").ifPresent(e -> aspect = "true".equalsIgnoreCase(e.getValue()));
        });
        conf.findSection("dosbox").ifPresent(s ->
                s.findEntry("machine").ifPresent(e -> machine = e.getValue())
        );
        conf.findSection("sdl").ifPresent(s -> {
            s.findEntry("output").ifPresent(e      -> output     = e.getValue());
            s.findEntry("fullscreen").ifPresent(e  -> fullscreen = "true".equalsIgnoreCase(e.getValue()));
            s.findEntry("fulldouble").ifPresent(e  -> fulldouble = "true".equalsIgnoreCase(e.getValue()));
            s.findEntry("windowresolution").ifPresent(e -> windowRes = e.getValue());
        });

        applyEffects();
    }

    // ── Effect engine ─────────────────────────────────────────────────────────

    private void applyEffects() {
        if (originalImage == null) return;

        Effect effect = buildMachineEffect();
        effect = chainScalerEffect(effect);
        imageView.setEffect(effect);

        if (aspect) {
            imageView.setFitWidth(originalImage.getWidth() * 1.2);
        } else {
            imageView.setFitWidth(0);
        }

        drawScanlines();
        updateSettingsLabel();
    }

    private Effect buildMachineEffect() {
        return switch (machine.toLowerCase()) {
            case "hercules" -> {
                ColorAdjust grey = new ColorAdjust();
                grey.setSaturation(-1.0);
                grey.setContrast(0.3);
                yield grey;
            }
            case "cga" -> {
                ColorAdjust cga = new ColorAdjust();
                cga.setSaturation(-0.6);
                cga.setContrast(0.8);
                cga.setHue(0.15);
                cga.setBrightness(-0.05);
                yield cga;
            }
            case "ega" -> {
                ColorAdjust ega = new ColorAdjust();
                ega.setSaturation(0.5);
                ega.setContrast(0.5);
                yield ega;
            }
            default -> {
                ColorAdjust vga = new ColorAdjust();
                vga.setSaturation(0.1);
                yield vga;
            }
        };
    }

    private Effect chainScalerEffect(Effect input) {
        // First apply scaler effect
        Effect result = switch (scaler.toLowerCase()) {
            case "hq2x", "hq3x" -> {
                GaussianBlur blur = new GaussianBlur(0.6);
                blur.setInput(input);
                yield blur;
            }
            case "2xsai", "super2xsai", "supereagle" -> {
                GaussianBlur blur = new GaussianBlur(1.0);
                blur.setInput(input);
                Bloom bloom = new Bloom(0.85);
                bloom.setInput(blur);
                yield bloom;
            }
            case "tv2x", "tv3x", "scan2x", "scan3x" -> {
                GaussianBlur blur = new GaussianBlur(0.4);
                blur.setInput(input);
                yield blur;
            }
            default -> input;
        };

        // Then layer SDL output effect on top
        result = switch (output.toLowerCase()) {
            case "opengl" -> {
                // OpenGL: slight sharpen via lighting effect
                Lighting lighting = new Lighting();
                lighting.setDiffuseConstant(1.1);
                lighting.setSpecularConstant(0.2);
                lighting.setSpecularExponent(8);
                lighting.setSurfaceScale(0.5);
                lighting.setLight(new javafx.scene.effect.Light.Distant(45, 30, Color.WHITE));
                lighting.setContentInput(result);
                yield lighting;
            }
            case "openglnb" -> {
                // OpenGL no bilinear: pixelated hard edges
                // Simulate by boosting contrast sharply
                ColorAdjust sharp = new ColorAdjust();
                sharp.setContrast(0.4);
                sharp.setInput(result);
                yield sharp;
            }
            case "overlay" -> {
                // Overlay: slight warm tint + brightness boost
                ColorAdjust warm = new ColorAdjust();
                warm.setBrightness(0.05);
                warm.setHue(-0.05);
                warm.setSaturation(0.15);
                warm.setInput(result);
                yield warm;
            }
            case "ddraw" -> {
                // DirectDraw: slight blurring artifact
                GaussianBlur blur = new GaussianBlur(0.8);
                blur.setInput(result);
                yield blur;
            }
            case "textmode" -> {
                // Text mode: heavy pixelation + desaturate
                ColorAdjust text = new ColorAdjust();
                text.setSaturation(-0.8);
                text.setContrast(0.6);
                text.setInput(result);
                yield text;
            }
            default -> result; // surface — no extra effect
        };

        // fulldouble: ghosting/blur to simulate double buffering
        if (fulldouble) {
            GaussianBlur ghost = new GaussianBlur(0.5);
            ghost.setInput(result);
            MotionBlur motion = new MotionBlur(0, 2.0);
            motion.setInput(ghost);
            result = motion;
        }

        return result;
    }

    private void drawScanlines() {
        double w = imageView.getBoundsInLocal().getWidth();
        double h = imageView.getBoundsInLocal().getHeight();
        if (w <= 0 || h <= 0) { w = 640; h = 180; }

        scanlineOverlay.setWidth(w);
        scanlineOverlay.setHeight(h);

        GraphicsContext gc = scanlineOverlay.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        boolean isTvScaler = scaler.startsWith("tv") || scaler.startsWith("scan");
        boolean isHerc     = machine.equalsIgnoreCase("hercules");
        boolean isCga      = machine.equalsIgnoreCase("cga");

        if (isHerc || isCga) {
            scanlineOverlay.setOpacity(0);
            return;
        }

        scanlineOverlay.setOpacity(isTvScaler ? 0.35 : 0.10);
        gc.setFill(Color.BLACK);
        double step = isTvScaler ? 2.0 : 3.0;
        for (double y = 0; y < h; y += step) {
            gc.fillRect(0, y, w, 1);
        }
    }

    private void updateSettingsLabel() {
        String scalerDisplay = scaler.isEmpty() || scaler.equals("none") ? "none" : scaler;
        settingsLabel.setText(String.format(
                "machine=%s  |  scaler=%s  |  aspect=%s  |  output=%s  |  fulldouble=%s",
                machine, scalerDisplay, aspect ? "true" : "false", output, fulldouble ? "true" : "false"));
    }
}