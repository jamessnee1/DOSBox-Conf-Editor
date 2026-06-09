# DOSBox Config Editor

A JavaFX desktop application for editing DOSBox `.conf` files, with a clean dark terminal aesthetic.

## Features

- **Open / Save / Save As** — full round-trip editing with comment preservation
- **Section collapsing** — each `[section]` is an expandable accordion pane
- **Live search** — filter keys and values across all sections in real time
- **Inline value editing** — double-click any value cell to edit it
- **Validation** — known keys (machine type, CPU core, cycles, SB settings, etc.) are validated with helpful error messages
- **New file wizard** — seeds a fresh config with common DOSBox sections pre-filled

## Requirements

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |

JavaFX is downloaded automatically by Maven — no manual SDK install needed.

## Build & Run

```bash
# Clone / unzip the project, then:
cd dosbox-conf-editor

# Run directly (downloads JavaFX automatically on first run)
mvn javafx:run

# Or build a fat JAR
mvn package
java -jar target/dosbox-conf-editor-1.0.0.jar
```

> **Note:** On macOS you may need to add `--add-opens` flags. The JavaFX Maven plugin handles this automatically via `mvn javafx:run`.

## Project Structure

```
src/main/java/com/dosboxeditor/
├── App.java                     Entry point
├── model/
│   ├── ConfigEntry.java         Key=value pair with optional comment
│   ├── ConfigSection.java       [section] block
│   └── ConfFile.java            Full .conf file in memory
├── parser/
│   ├── ConfParser.java          .conf → model (preserves comments)
│   └── ConfWriter.java          model → .conf file on disk
├── validation/
│   └── ConfValidator.java       Validates known DOSBox keys
└── ui/
    ├── MainWindow.java          Root BorderPane layout
    ├── SectionPanel.java        Accordion of section tables
    └── EditableValueCell.java   Inline editing TableCell
src/main/resources/com/dosboxeditor/
└── styles.css                   Dark terminal theme
```

## Extending

### Adding more validated keys

Edit `ConfValidator.java`:
- Add to `BOOLEAN_KEYS` for true/false keys
- Add to `ENUM_KEYS` for enumerated choices
- Add to `INT_RANGE_KEYS` for numeric range keys

### Adding keyboard shortcuts

In `MainWindow.java`, attach `KeyCombination` accelerators to buttons or wire them to the `Scene`.

## License

MIT License

Copyright (c) 2026 James Snee.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
