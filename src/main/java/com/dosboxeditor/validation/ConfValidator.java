package com.dosboxeditor.validation;

import java.util.*;

/**
 * Validates the values of well-known DOSBox .conf keys.
 * Returns a human-readable error message, or null if the value is valid.
 */
public class ConfValidator {

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult error(String msg) { return new ValidationResult(false, msg); }
    }

    // Known boolean keys across all sections
    private static final Set<String> BOOLEAN_KEYS = Set.of(
        "fullscreen", "fulldouble", "vsync", "output", "autolock", "clipboard",
        "waitonerror", "xms", "ems", "umb", "allowappswitch", "mpu401",
        "sbmixer", "tandy", "disney", "ipx"
    );

    // Enumerated value sets for specific keys
    private static final Map<String, Set<String>> ENUM_KEYS = new HashMap<>();
    static {
        ENUM_KEYS.put("machine",      Set.of("hercules","cga","ega","vgaonly","svga_s3","svga_et3000","svga_et4000","svga_paradise","vesa_nolfb","vesa_oldvbe"));
        ENUM_KEYS.put("core",         Set.of("auto","simple","normal","dynamic","full"));
        ENUM_KEYS.put("cputype",      Set.of("auto","386","386_slow","386_prefetch","486_slow","pentium_slow"));
        ENUM_KEYS.put("output",       Set.of("surface","overlay","opengl","openglnb","ddraw","textmode"));
        ENUM_KEYS.put("scaler",       Set.of("none","normal2x","normal3x","advmame2x","advmame3x","advinterp2x","advinterp3x","hq2x","hq3x","2xsai","super2xsai","supereagle","tv2x","tv3x","rgb2x","rgb3x","scan2x","scan3x"));
        ENUM_KEYS.put("mpu401",       Set.of("none","uart","intelligent"));
        ENUM_KEYS.put("mididevice",   Set.of("default","win32","alsa","oss","coreaudio","coremidi","none"));
        ENUM_KEYS.put("sbtype",       Set.of("sb1","sb2","sbpro1","sbpro2","sb16","gb","none"));
        ENUM_KEYS.put("oplmode",      Set.of("auto","cms","opl2","dualopl2","opl3","opl3gold","none"));
        ENUM_KEYS.put("oplemu",       Set.of("default","compat","fast","mame"));
        ENUM_KEYS.put("pcspeaker",    Set.of("true","false"));
        ENUM_KEYS.put("tandy",        Set.of("auto","on","off"));
        ENUM_KEYS.put("windowresolution", Set.of("original", "desktop"));
        ENUM_KEYS.put("output",           Set.of("surface", "overlay", "opengl", "openglnb", "ddraw", "textmode"));
    }

    // Integer-range keys: key -> [min, max]
    private static final Map<String, int[]> INT_RANGE_KEYS = new HashMap<>();
    static {
        INT_RANGE_KEYS.put("memsize",    new int[]{1, 384});
        INT_RANGE_KEYS.put("cycles",     new int[]{100, 1_000_000}); // numeric mode only
        INT_RANGE_KEYS.put("cycleup",    new int[]{1, 100_000});
        INT_RANGE_KEYS.put("cycledown",  new int[]{1, 100_000});
        INT_RANGE_KEYS.put("sbbase",     new int[]{0, 0xFFFF});
        INT_RANGE_KEYS.put("sbirq",      new int[]{0, 15});
        INT_RANGE_KEYS.put("sbdma",      new int[]{0, 7});
        INT_RANGE_KEYS.put("sbhdma",     new int[]{0, 7});
        INT_RANGE_KEYS.put("oplrate",    new int[]{8000, 96000});
    }

    /**
     * Returns a human-readable tooltip for a known key, or null if unknown.
     */
    public static String getTooltip(String key) {
        if (key == null) return null;
        return switch (key.trim().toLowerCase()) {
            // [dosbox]
            case "language"   -> "Path to a language file. Leave blank for English.";
            case "machine"    -> "Allowed: hercules, cga, ega, vgaonly, svga_s3, svga_et3000,\nsvga_et4000, svga_paradise, vesa_nolfb, vesa_oldvbe";
            case "captures"   -> "Directory where audio/video captures are saved.";
            case "memsize"    -> "RAM in MB (1–384). Default: 16";
            // [render]
            case "frameskip"  -> "Frames to skip rendering (reduces CPU load). Default: 0";
            case "aspect"     -> "Aspect ratio correction. true or false.";
            case "scaler"     -> "Allowed: none, normal2x, normal3x, advmame2x, advmame3x,\nadvinterp2x, advinterp3x, hq2x, hq3x, 2xsai, super2xsai,\nsupereagle, tv2x, tv3x, rgb2x, rgb3x, scan2x, scan3x";
            // [cpu]
            case "core"       -> "CPU core. Allowed: auto, simple, normal, dynamic, full";
            case "cputype"    -> "Allowed: auto, 386, 386_slow, 386_prefetch, 486_slow, pentium_slow";
            case "cycles"     -> "CPU speed. Use: auto, max, or a number (100–1000000).\nExamples: auto, max, 3000, max 80%";
            case "cycleup"    -> "Amount to increase cycles with keyboard shortcut (1–100000).";
            case "cycledown"  -> "Amount to decrease cycles with keyboard shortcut (1–100000).";
            // [mixer]
            case "nosound"    -> "Disable sound output. true or false.";
            case "rate"       -> "Mixer sample rate in Hz. Common: 22050, 44100, 48000.";
            case "blocksize"  -> "Mixer block size. Larger = more latency but more stable.";
            case "prebuffer"  -> "Milliseconds of audio to prebuffer (0–100).";
            // [midi]
            case "mpu401"     -> "MPU-401 mode. Allowed: none, uart, intelligent";
            case "mididevice" -> "Allowed: default, win32, alsa, oss, coreaudio, coremidi, none";
            case "midiconfig" -> "Extra config passed to the MIDI device (e.g. port number).";
            // [sblaster]
            case "sbtype"     -> "Sound Blaster type. Allowed: sb1, sb2, sbpro1, sbpro2, sb16, gb, none";
            case "sbbase"     -> "Sound Blaster I/O address (hex). Common: 220, 240.";
            case "sbirq"      -> "Sound Blaster IRQ (0–15). Common: 5, 7.";
            case "sbdma"      -> "Sound Blaster DMA channel (0–7). Common: 1.";
            case "sbhdma"     -> "Sound Blaster 16 high DMA channel (0–7). Common: 5.";
            case "sbmixer"    -> "Allow programs to change the SB mixer. true or false.";
            case "oplmode"    -> "OPL emulation mode. Allowed: auto, cms, opl2, dualopl2,\nopl3, opl3gold, none";
            case "oplemu"     -> "OPL emulator quality. Allowed: default, compat, fast, mame";
            case "oplrate"    -> "OPL sample rate in Hz (8000–96000). Default: 22050";
            // [speaker]
            case "pcspeaker"  -> "Enable PC speaker emulation. true or false.";
            case "pcrate"     -> "PC speaker sample rate in Hz.";
            case "tandy"      -> "Tandy sound. Allowed: auto, on, off";
            case "tandyrate"  -> "Tandy sound sample rate in Hz.";
            case "disney"     -> "Disney Sound Source emulation. true or false.";
            // [dos]
            case "xms"        -> "Enable XMS (extended memory) support. true or false.";
            case "ems"        -> "Enable EMS (expanded memory) support. true or false.";
            case "umb"        -> "Enable UMB (upper memory block) support. true or false.";
            case "keyboardlayout" -> "Keyboard layout code, e.g. us, uk, de. Leave blank for default.";
            // [ipx]
            case "ipx"        -> "Enable IPX network emulation. true or false.";
            // [sdl]
            case "fullscreen"        -> "Start in fullscreen. true or false.";
            case "fulldouble"        -> "Use double buffering in fullscreen. true or false.";
            case "fullresolution"    -> "Resolution to use in fullscreen, e.g. 1920x1080. 'desktop' uses your desktop res.";
            case "windowresolution"  -> "Scale the window to this size. 'original' = no scaling. e.g. 1280x960";
            case "output"            -> "Video output method. Allowed: surface, overlay, opengl, openglnb, ddraw, textmode";
            case "autolock"          -> "Automatically lock the mouse when clicking inside the window. true or false.";
            case "sensitivity"       -> "Mouse sensitivity (1–1000). Default: 100";
            case "waitonerror"       -> "Keep the console open after an error. true or false.";
            case "priority"          -> "Priority when focused/unfocused. e.g. higher,normal or normal,pause";
            case "mapperfile"        -> "Path to the keymapper file. Default: mapper-sdl2.map";
            case "usescancodes"      -> "Avoid translating scanscodes — leave on true unless you have issues. true or false.";
            default           -> null;
        };
    }

    /**
     * Validate a key=value pair.
     * @param key   the config key (case-insensitive)
     * @param value the raw value string
     */
    public ValidationResult validate(String key, String value) {
        if (key == null || key.isBlank()) return ValidationResult.ok();
        String k = key.trim().toLowerCase();
        String v = value == null ? "" : value.trim().toLowerCase();

        // Enum check
        if (ENUM_KEYS.containsKey(k)) {
            Set<String> allowed = ENUM_KEYS.get(k);
            if (!allowed.contains(v)) {
                return ValidationResult.error(
                    "Invalid value '" + value + "' for '" + key + "'.\nAllowed: " + String.join(", ", sorted(allowed)));
            }
            return ValidationResult.ok();
        }

        // Boolean check
        if (BOOLEAN_KEYS.contains(k)) {
            if (!v.equals("true") && !v.equals("false")) {
                return ValidationResult.error("'" + key + "' must be 'true' or 'false'.");
            }
            return ValidationResult.ok();
        }

        // Integer-range check
        if (INT_RANGE_KEYS.containsKey(k)) {
            // cycles accepts "auto", "max", or a number
            if (k.equals("cycles") && (v.startsWith("auto") || v.startsWith("max"))) {
                return ValidationResult.ok();
            }
            try {
                int num = Integer.parseInt(v);
                int[] range = INT_RANGE_KEYS.get(k);
                if (num < range[0] || num > range[1]) {
                    return ValidationResult.error(
                        "'" + key + "' must be between " + range[0] + " and " + range[1] + ".");
                }
            } catch (NumberFormatException e) {
                return ValidationResult.error("'" + key + "' must be a whole number.");
            }
            return ValidationResult.ok();
        }

        // Unknown key – no opinion
        return ValidationResult.ok();
    }

    private List<String> sorted(Set<String> set) {
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }
}
