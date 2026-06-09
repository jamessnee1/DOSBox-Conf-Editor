package com.dosboxeditor.parser;

import com.dosboxeditor.model.ConfFile;
import com.dosboxeditor.model.ConfigEntry;
import com.dosboxeditor.model.ConfigSection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Serialises a {@link ConfFile} back to disk, preserving comments and
 * the original key ordering.
 */
public class ConfWriter {

    /**
     * Write {@code conf} to its associated {@link ConfFile#getSourceFile()}.
     */
    public void write(ConfFile conf) throws IOException {
        write(conf, conf.getSourceFile());
    }

    /**
     * Write {@code conf} to an arbitrary target file (Save As).
     */
    public void write(ConfFile conf, File target) throws IOException {
        StringBuilder sb = new StringBuilder();

        // Leading file comment
        String leading = conf.getLeadingComment();
        if (leading != null && !leading.isEmpty()) {
            sb.append(leading).append("\n\n");
        }

        for (ConfigSection section : conf.getSections()) {
            // Skip the implicit root section header
            boolean isRoot = "__root__".equals(section.getName());

            // Section header comment
            String hc = section.getHeaderComment();
            if (hc != null && !hc.isEmpty()) {
                sb.append(hc).append("\n");
            }

            if (!isRoot) {
                sb.append("[").append(section.getName()).append("]\n");
            }

            for (ConfigEntry entry : section.getEntries()) {
                String key = entry.getKey();

                // Blank separator lines
                if (key == null || key.isEmpty()) {
                    String ec = entry.getComment();
                    if (ec != null && !ec.isEmpty()) {
                        sb.append(ec).append("\n");
                    } else {
                        sb.append("\n");
                    }
                    continue;
                }

                // Preceding comment
                String ec = entry.getComment();
                if (ec != null && !ec.isEmpty()) {
                    sb.append(ec).append("\n");
                }

                sb.append(key).append("=").append(entry.getValue()).append("\n");
            }

            sb.append("\n");
        }

        Files.writeString(target.toPath(), sb.toString(), StandardCharsets.UTF_8);
        conf.setModified(false);
    }
}
