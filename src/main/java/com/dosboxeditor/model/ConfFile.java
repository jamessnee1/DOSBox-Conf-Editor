package com.dosboxeditor.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.Optional;

/**
 * Represents a complete DOSBox .conf file in memory.
 */
public class ConfFile {

    private File sourceFile;
    private String leadingComment; // top-of-file comment block
    private boolean modified = false;
    private final ObservableList<ConfigSection> sections = FXCollections.observableArrayList();

    public ConfFile() {}

    public ConfFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    // --- File source ---
    public File getSourceFile() { return sourceFile; }
    public void setSourceFile(File f) { sourceFile = f; }

    // --- Leading comment ---
    public String getLeadingComment() { return leadingComment; }
    public void setLeadingComment(String c) { leadingComment = c; }

    // --- Modified flag ---
    public boolean isModified() { return modified; }
    public void setModified(boolean m) { modified = m; }

    // --- Sections ---
    public ObservableList<ConfigSection> getSections() { return sections; }

    public void addSection(ConfigSection section) {
        sections.add(section);
    }

    public Optional<ConfigSection> findSection(String name) {
        return sections.stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public String getDisplayName() {
        if (sourceFile != null) return sourceFile.getName();
        return "Untitled.conf";
    }

    /**
     * Serialises the entire file back to its .conf text representation,
     * preserving comments and ordering.
     */
    public String toFileContent() {
        StringBuilder sb = new StringBuilder();
        if (leadingComment != null && !leadingComment.isEmpty()) {
            sb.append(leadingComment).append("\n");
        }
        for (ConfigSection section : sections) {
            sb.append(section);
            sb.append("\n");
        }
        return sb.toString();
    }
}
