package com.dosboxeditor.parser;

import com.dosboxeditor.model.ConfFile;
import com.dosboxeditor.model.ConfigEntry;
import com.dosboxeditor.model.ConfigSection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a DOSBox .conf file from disk into a {@link ConfFile} object.
 * <p>
 * Handles:
 * <ul>
 *   <li>Leading file-level comment blocks</li>
 *   <li>[section] headers with preceding comments</li>
 *   <li>key=value lines with optional inline comments</li>
 *   <li>Blank lines (preserved as empty-key entries so round-trip output is faithful)</li>
 * </ul>
 */
public class ConfParser {

    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([^]]+)]\\s*(.*)$");
    private static final Pattern ENTRY_PATTERN   = Pattern.compile("^([^=]+)=(.*)$");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^[#;].*$");

    /**
     * Parse the given file and return a populated {@link ConfFile}.
     */
    public ConfFile parse(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        ConfFile conf = new ConfFile(file);

        StringBuilder pendingComment = new StringBuilder();
        ConfigSection currentSection = null;
        boolean inLeadingComment = true;

        for (String raw : lines) {
            String line = raw; // keep original for whitespace fidelity
            String trimmed = raw.trim();

            // ── Empty line ──────────────────────────────────────────────────
            if (trimmed.isEmpty()) {
                if (inLeadingComment) {
                    pendingComment.append("\n");
                } else if (currentSection != null) {
                    // Preserve blank lines inside sections as blank entries
                    currentSection.addEntry(new ConfigEntry("", "", null));
                }
                continue;
            }

            // ── Comment line ─────────────────────────────────────────────────
            if (COMMENT_PATTERN.matcher(trimmed).matches()) {
                if (pendingComment.length() > 0) pendingComment.append("\n");
                pendingComment.append(line);
                continue;
            }

            // ── Section header ───────────────────────────────────────────────
            Matcher sm = SECTION_PATTERN.matcher(trimmed);
            if (sm.matches()) {
                inLeadingComment = false;

                // Flush any accumulated file-level leading comment
                if (currentSection == null && conf.getLeadingComment() == null) {
                    conf.setLeadingComment(pendingComment.toString().stripTrailing());
                    pendingComment.setLength(0);
                }

                String sectionComment = pendingComment.toString().stripTrailing();
                pendingComment.setLength(0);

                currentSection = new ConfigSection(sm.group(1), sectionComment);
                conf.addSection(currentSection);
                continue;
            }

            // ── Key=Value line ───────────────────────────────────────────────
            Matcher em = ENTRY_PATTERN.matcher(trimmed);
            if (em.matches()) {
                inLeadingComment = false;
                String key   = em.group(1).trim();
                String value = em.group(2).trim();

                String comment = pendingComment.toString().stripTrailing();
                pendingComment.setLength(0);

                if (currentSection == null) {
                    // Orphan entry before any section – create implicit root
                    currentSection = new ConfigSection("__root__", "");
                    conf.addSection(currentSection);
                }
                currentSection.addEntry(new ConfigEntry(key, value, comment));
                continue;
            }

            // ── Unrecognised line (treat as raw comment) ─────────────────────
            if (pendingComment.length() > 0) pendingComment.append("\n");
            pendingComment.append(line);
        }

        // Flush any trailing comment
        if (pendingComment.length() > 0) {
            if (conf.getLeadingComment() == null && currentSection == null) {
                conf.setLeadingComment(pendingComment.toString());
            } else if (currentSection != null) {
                // Attach to last entry or section as a trailing comment entry
                currentSection.addEntry(new ConfigEntry("", "", pendingComment.toString()));
            }
        }

        return conf;
    }
}
