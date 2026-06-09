package com.dosboxeditor.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a single key=value line within a DOSBox config section.
 * Comments and blank lines associated with the entry are preserved.
 */
public class ConfigEntry {

    private final StringProperty key = new SimpleStringProperty();
    private final StringProperty value = new SimpleStringProperty();
    private String comment; // inline or preceding comment, preserved verbatim

    public ConfigEntry(String key, String value, String comment) {
        this.key.set(key);
        this.value.set(value);
        this.comment = comment;
    }

    public ConfigEntry(String key, String value) {
        this(key, value, null);
    }

    // --- Key ---
    public String getKey() { return key.get(); }
    public void setKey(String k) { key.set(k); }
    public StringProperty keyProperty() { return key; }

    // --- Value ---
    public String getValue() { return value.get(); }
    public void setValue(String v) { value.set(v); }
    public StringProperty valueProperty() { return value; }

    // --- Comment ---
    public String getComment() { return comment; }
    public void setComment(String c) { comment = c; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (comment != null && !comment.isEmpty()) {
            sb.append(comment).append("\n");
        }
        sb.append(key.get()).append("=").append(value.get());
        return sb.toString();
    }
}
