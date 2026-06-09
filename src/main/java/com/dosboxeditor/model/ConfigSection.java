package com.dosboxeditor.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Optional;

/**
 * Represents a [section] block in a DOSBox .conf file.
 * Each section has a name and an ordered list of ConfigEntry items.
 */
public class ConfigSection {

    private String name;
    private String headerComment; // comment lines appearing before the [section] header
    private final ObservableList<ConfigEntry> entries = FXCollections.observableArrayList();

    public ConfigSection(String name) {
        this.name = name;
    }

    public ConfigSection(String name, String headerComment) {
        this.name = name;
        this.headerComment = headerComment;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHeaderComment() { return headerComment; }
    public void setHeaderComment(String c) { this.headerComment = c; }

    public ObservableList<ConfigEntry> getEntries() { return entries; }

    public void addEntry(ConfigEntry entry) {
        entries.add(entry);
    }

    public Optional<ConfigEntry> findEntry(String key) {
        return entries.stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .findFirst();
    }

    public void setOrAdd(String key, String value) {
        Optional<ConfigEntry> existing = findEntry(key);
        if (existing.isPresent()) {
            existing.get().setValue(value);
        } else {
            entries.add(new ConfigEntry(key, value));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (headerComment != null && !headerComment.isEmpty()) {
            sb.append(headerComment).append("\n");
        }
        sb.append("[").append(name).append("]\n");
        for (ConfigEntry entry : entries) {
            sb.append(entry).append("\n");
        }
        return sb.toString();
    }
}
