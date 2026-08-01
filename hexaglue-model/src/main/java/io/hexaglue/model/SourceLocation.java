/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.model;

import java.util.Objects;

/**
 * A location in source code: file path and 1-based line range.
 *
 * <p>Locations anchor evidences, findings and diagnostics to the code they talk about. When a
 * position is unknown the surrounding model uses an empty {@link java.util.Optional} rather than a
 * sentinel location; a constructed location is therefore always valid: non-blank file path, lines
 * at least 1, end line not before start line.</p>
 *
 * @param filePath the file path, absolute or relative to the analyzed project
 * @param lineStart the starting line number (1-based)
 * @param lineEnd the ending line number (1-based, {@code >= lineStart})
 * @since 7.0.0
 */
public record SourceLocation(String filePath, int lineStart, int lineEnd) {

    /**
     * Validates the file path and the line range.
     */
    public SourceLocation {
        Objects.requireNonNull(filePath, "filePath must not be null");
        if (filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must not be blank");
        }
        if (lineStart < 1) {
            throw new IllegalArgumentException("lineStart must be >= 1, got " + lineStart);
        }
        if (lineEnd < lineStart) {
            throw new IllegalArgumentException("lineEnd must be >= lineStart, got " + lineEnd + " < " + lineStart);
        }
    }

    /**
     * Returns the file name without its directory path, handling both separator styles.
     *
     * @return the file name
     */
    public String fileName() {
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash < 0 ? filePath : filePath.substring(lastSlash + 1);
    }

    /**
     * Returns a compact human-readable form (e.g. {@code Order.java:42}).
     *
     * @return the display string
     */
    public String toDisplayString() {
        return fileName() + ":" + lineStart;
    }
}
