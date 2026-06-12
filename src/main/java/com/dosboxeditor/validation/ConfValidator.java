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
            case "mcb_fault_strategy" -> "How software-corrupted memory chain blocks should be handled. Allowed: repair, report, allow, deny";
            case "vmemsize"   -> "Video memory in MB (1-8) or KB (256 to 8192). Allowed: auto, 1, 2, 4, 8, 256, 512, 1024, 2048, 4096, 8192.";
            case "vmem_delay" -> "Set video memory access delay emulation ('off' by default). Allowed: off, on, <value> (0 to 20000 ns)";
            case "dos_rate"   -> "Customize the emulated video mode's frame rate. default, host (see host_rate), <value> (between 24.000 - 100,000 Hz)";
            case "vesa_modes" -> "Controls the selection of VESA 1.2 and 2.0 modes offered. Allowed: compatible, halfline, all";
            case "vga_8dot_font" -> "Use 8-pixel-wide fonts on VGA adapters (disabled by default).";
            case "vga_render_per_scanline" -> "Emulate accurate per-scanline VGA rendering (enabled by default).";
            case "speed_mods" -> "Permit changes known to improve performance (enabled by default).";
            case "autoexec_section" -> "How autoexec sections are handled from multiple config files. Allowed: join, overwrite";
            case "automount"  -> "Mount 'drives/[c]' directories as drives on startup";
            case "startup_verbosity" -> "Controls verbosity prior to displaying the program ('auto' by default). Allowed: auto, high, low, quiet";
            case "allow_write_protected_files" -> "Lets you write protect files while still reading them (enabled by default)";
            case "shell_config_shortcuts" -> "Allow shortcuts for simpler configuration management (enabled by default).";
            // [render]
            case "frameskip"  -> "Frames to skip rendering (reduces CPU load). Default: 0";
            case "aspect"     -> "Aspect ratio correction. Allowed: auto, on, square-pixels, off, stretch";
            case "scaler"     -> "Allowed: none, normal2x, normal3x, advmame2x, advmame3x,\nadvinterp2x, advinterp3x, hq2x, hq3x, 2xsai, super2xsai,\nsupereagle, tv2x, tv3x, rgb2x, rgb3x, scan2x, scan3x";
            case "integer_scaling" -> "Constrain the horizontal or vertical scaling factor to the largest integer value so the image still fits in viewport. Allowed: auto, vertical, horizontal, off";
            case "viewport"   -> "Set the viewport size. Allowed: fit, WxH (eg 960x720), N% (eg 200%), relative H% V% (valid range 20%-300%)";
            case "monochrome_palette" -> "Set the palette for monochrome display emulation ('amber' by default). Works only with the 'hercules' and 'cga_mono' machine types.";
            case "cga_colors" -> "Set the interpretation of CGA RGBI colours. Built in presets: default, tandy <bl> (default to 50), tandy-warm, ibm5313 <c> (0-100), agi-amiga-v1, agi-amiga-v2, agi-amiga-v3, agi-amigaish, scumm-amiga, colodore, colodore-sat, dga16";
            case "glshader"   -> "Set an adaptive CRT monitor emulation shader or a regular GLSL shader in OpenGL output modes. Allowed: crt-auto, crt-auto-machine, crt-auto-arcade, crt-auto-arcade-sharp";
            // [composite]
            case "composite"  -> "Enable composite mode on start (only for 'cga', 'pcjr', and 'tandy' machine types, auto by default)";
            case "era"        -> "Era of composite technology ('auto' by default). Allowed: auto, old, new";
            case "hue"        -> "Hue of the RGB palette (0 by default).";
            case "saturation" -> "Intensity of colors, from washed out to vivid (100 by default).";
            case "contrast"   -> "Ratio between the dark and light area (100 by default).";
            case "brightness" -> "Luminosity of the image, from dark to light (0 by default).";
            case "convergence" -> "Convergence of subpixel elements, from blurry to sharp (0 by default).";
            // [cpu]
            case "core"       -> "CPU core. Allowed: auto, simple, normal, dynamic, full";
            case "cputype"    -> "Allowed: auto, 386, 386_slow, 386_prefetch, 486_slow, pentium_slow";
            case "cycles"     -> "CPU speed. Use: auto, max, or a number (100–1000000).\nExamples: auto, max, 3000, max 80%";
            case "cycleup"    -> "Amount to increase cycles with keyboard shortcut (1–100000).";
            case "cycledown"  -> "Amount to decrease cycles with keyboard shortcut (1–100000).";
            // [voodoo]
            case "voodoo"     -> "Enable 3dfx Voodoo emulation (enabled by default). Allowed: true, false";
            case "voodoo_memsize" -> "Set the amount of video memory for 3dfx Voodoo graphics, either 4 or 12 MB.";
            case "voodoo_multithreading" -> "Use threads to improve 3dfx Voodoo performance (enabled by default). Allowed: true, false";
            case "voodoo_bilinear_filtering" -> "Use bilinear filtering to emulate the 3dfx Voodoo's texture smoothing effect (disabled by default). Allowed: true, false";
            // [capture]
            case "capture_dir" -> "Directory where the various captures are saved, such as audio, video, MIDI and screenshot captures.";
            case "default_image_capture_formats" -> "Set the capture format of the default screenshot action (upscaled by default). Allowed: upscaled, rendered, raw";
            // [mouse]
            case "mouse_capture"              -> "When to capture the mouse. Allowed: onclick, onstart, seamless, nomouse";
            case "mouse_middle_release"       -> "Release captured mouse when middle button is clicked. true or false.";
            case "mouse_multi_display_aware"  -> "Allow mouse to move across multiple displays. true or false.";
            case "mouse_sensitivity"          -> "Mouse sensitivity (1–1000). Default: 100. " +
                    "Can also set horizontal and vertical separately e.g. 100,100";
            case "mouse_raw_input"            -> "Read mouse input directly, bypassing OS acceleration. true or false.";
            case "dos_mouse_driver"           -> "Enable the built-in DOS mouse driver (like MOUSE.COM). true or false.";
            case "dos_mouse_immediate"        -> "Report mouse movement immediately without waiting for DOS to poll. " +
                    "true or false. May cause issues in some games.";
            case "ps2_mouse_model"            -> "PS/2 mouse model to emulate.\n" +
                    "Allowed: none, standard, noscroll, wheel, explorer, explorer+";
            case "com_mouse_model"            -> "Serial (COM port) mouse model to emulate.\n" +
                    "Allowed: none, microsoft, wheel, mousesystems, logitech,\n" +
                    "msm, wheel+msm, mousesystems+msm, logitech+msm";
            case "vmware_mouse"               -> "Enable VMware mouse protocol for seamless host/guest mouse sharing. " +
                    "true or false.";
            case "virtualbox_mouse"           -> "Enable VirtualBox mouse protocol for seamless host/guest mouse sharing. " +
                    "true or false.";
            // [mixer]
            case "nosound"    -> "Disable sound output. true or false.";
            case "rate"       -> "Mixer sample rate in Hz. Common: 22050, 44100, 48000.";
            case "blocksize"  -> "Mixer block size. Larger = more latency but more stable.";
            case "prebuffer"  -> "Milliseconds of audio to prebuffer (0–100).";
            case "negotiate"   -> "Allow DOSBox to negotiate the sample rate with the host audio system. " +
                    "false = use the rate set in [mixer]. true or false.";
            case "compressor"  -> "Enable auto-levelling compressor on the master output. " +
                    "Prevents audio clipping on loud games. true or false.";
            case "crossfeed"   -> "Crossfeed between left and right audio channels, reducing ear fatigue " +
                    "on headphones. Allowed: off, weak, moderate, strong, custom";
            case "reverb"      -> "Add reverb to the master output to simulate room acoustics.\n" +
                    "Allowed: off, tiny, small, medium, large, huge";
            case "chorus"      -> "Add chorus effect to the master output for a richer, wider sound.\n" +
                    "Allowed: off, light, normal, strong, heavy";
            // [midi]
            case "mpu401"     -> "MPU-401 mode. Allowed: none, uart, intelligent";
            case "mididevice" -> "Allowed: default, win32, alsa, oss, coreaudio, coremidi, none";
            case "midiconfig" -> "Extra config passed to the MIDI device (e.g. port number).";
            case "raw_midi_output" -> "Enable raw, unaltered MIDI output (disabled by default). Allowed: true, false";
            // [fluidsynth]
            case "soundfont"     -> "Path to a SoundFont (.sf2) file for FluidSynth MIDI playback.\n" +
                    "Use 'default.sf2', an absolute path, or a filename inside the\n" +
                    "'soundfonts' folder in your DOSBox config directory.\n\n" +
                    "Optionally append a volume percentage (1–800) after the filename\n" +
                    "to scale the SoundFont's volume. Useful for normalising loudness\n" +
                    "across different SoundFonts.\n\n" +
                    "Example: 'my_soundfont.sf2 50' attenuates volume by 50%.";
            case "fsynth_chorus" -> "Chorus effect for FluidSynth.\n" +
                    "Allowed: auto, on, off, or five custom values space-separated:\n\n" +
                    "  voice-count level speed depth modulation-wave\n\n" +
                    "  voice-count   integer, 0–99\n" +
                    "  level         decimal, 0.0–10.0\n" +
                    "  speed         decimal in Hz, 0.1–5.0\n" +
                    "  depth         decimal, 0.0–21.0\n" +
                    "  modulation    sine or triangle\n\n" +
                    "Example: fsynth_chorus = 3 1.2 0.3 8.0 sine\n\n" +
                    "Note: Can be combined with mixer-level chorus on the\n" +
                    "FluidSynth channel, though results depend on the SoundFont.";
            case "fsynth_reverb" -> "Reverb effect for FluidSynth.\n" +
                    "Allowed: auto, on, off, or four custom values space-separated:\n\n" +
                    "  room-size damping width level\n\n" +
                    "  room-size   decimal, 0.0–1.0\n" +
                    "  damping     decimal, 0.0–1.0\n" +
                    "  width       decimal, 0.0–100.0\n" +
                    "  level       decimal, 0.0–1.0\n\n" +
                    "Example: fsynth_reverb = 0.61 0.23 0.76 0.56\n\n" +
                    "Note: Can be combined with mixer-level reverb on the\n" +
                    "FluidSynth channel, though results depend on the SoundFont.";
            case "fsynth_filter" -> "Audio filter applied to FluidSynth output.\n" +
                    "Allowed: off (default), or a custom filter definition.\n" +
                    "See 'sb_filter' in the [sblaster] section for filter syntax details.";
            // [MT32]
            case "model"       -> "Roland MT-32/CM-32L model to emulate.\n" +
                    "You must have the matching ROM files in 'romdir'.\n\n" +
                    "Allowed:\n" +
                    "  auto       Pick the first available model (default)\n" +
                    "  cm32l      First available CM-32L model\n" +
                    "  mt32_old   First available 'old' MT-32 (v1.0x)\n" +
                    "  mt32_new   First available 'new' MT-32 (v2.0x)\n" +
                    "  mt32       First available MT-32 model\n" +
                    "  <version>  Exact version e.g. mt32_204, cm32l_102\n\n" +
                    "All versions: auto, cm32l, cm32l_102, cm32l_100,\n" +
                    "cm32ln_100, mt32, mt32_old, mt32_107, mt32_106,\n" +
                    "mt32_105, mt32_104, mt32_bluer, mt32_new, mt32_207,\n" +
                    "mt32_206, mt32_204, mt32_203";
            case "romdir"      -> "Directory containing the Roland MT-32/CM-32L ROM files.\n" +
                    "Can be an absolute or relative path, or leave unset to\n" +
                    "use the 'mt32-roms' folder in your DOSBox config directory.\n\n" +
                    "Notes:\n" +
                    "  - Filenames do not matter; ROMs are identified by checksum\n" +
                    "  - Both interleaved and non-interleaved ROM files are supported\n" +
                    "  - Common system locations are also searched automatically";
            case "mt32_filter" -> "Audio filter applied to MT-32/CM-32L output.\n" +
                    "Allowed: off (default), or a custom filter definition.\n" +
                    "See 'sb_filter' in the [sblaster] section for filter syntax details.";
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
            case "display"           -> "Number of display to use; values depend on OS and user settings (0 by default).";
            case "fulldouble"        -> "Use double buffering in fullscreen. true or false.";
            case "fullresolution"    -> "Resolution to use in fullscreen, e.g. 1920x1080. 'desktop' uses your desktop res.";
            case "windowresolution"  -> "Scale the window to this size. 'original' = no scaling. e.g. 1280x960";
            case "window_position"   -> "Set initial window position for windowed mode. Allowed: auto, X,Y";
            case "window_decorations"-> "Enable window decorations in windowed mode (enabled by default).";
            case "transparency"      -> "Set the transparency of the DOSBox Staging screen (0 by default).";
            case "host_rate"         -> "Set the host's refresh rate. Allowed: auto, sdi, vrr, N";
            case "vsync"             -> "Set the host video driver's vertical synchronization (vsync) mode. Allowed: auto, on, adaptive, off, yield";
            case "vsync_skip"        -> "Number of microseconds to allow rendering to block before skipping the next frame. For example, a value of 7000 is roughly half the frame time at 70 Hz. 0 disables this and will always render (default).";
            case "presentation_mode" -> "Select the frame presentation mode. Allowed: auto, cfr, vfr";
            case "output"            -> "Video output method. Allowed: surface, overlay, opengl, openglnb, ddraw, textmode";
            case "texture_renderer"  -> "Render driver to use in 'texture' output mode ('auto' by default).";
            case "autolock"          -> "Automatically lock the mouse when clicking inside the window. true or false.";
            case "sensitivity"       -> "Mouse sensitivity (1–1000). Default: 100";
            case "waitonerror"       -> "Keep the console open after an error. true or false.";
            case "priority"          -> "Priority when focused/unfocused. e.g. higher,normal or normal,pause";
            case "mapperfile"        -> "Path to the keymapper file. Default: mapper-sdl2.map";
            case "usescancodes"      -> "Avoid translating scanscodes — leave on true unless you have issues. true or false.";
            case "mute_when_inactive"-> "Mute the sound when the window is inactive (disabled by default).";
            case "pause_when_inactive" -> "Pause emulation when the window is inactive (disabled by default).";
            case "screensaver"       -> ")verride the SDL_VIDEO_ALLOW_SCREENSAVER environment variable. Allowed: auto, allow, block.";
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
