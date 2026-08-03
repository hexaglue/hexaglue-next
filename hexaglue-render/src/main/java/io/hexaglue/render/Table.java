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

package io.hexaglue.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A markdown table.
 *
 * <p>A cell is text, not markup: a pipe or a newline inside a value would end the row where the
 * author did not mean to, so both are neutralised as the cell is added. A row that does not have
 * as many cells as the table has columns is a mistake caught here rather than a table that renders
 * crooked.</p>
 *
 * @since 7.0.0
 */
public final class Table {

    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();

    private Table(List<String> headers) {
        this.headers = List.copyOf(headers);
    }

    /**
     * Starts a table with its column headings.
     *
     * @param headers the headings, one per column
     * @return a new table
     */
    public static Table withHeaders(String... headers) {
        Objects.requireNonNull(headers, "headers must not be null");
        if (headers.length == 0) {
            throw new IllegalArgumentException("a table needs at least one column");
        }
        return new Table(List.of(headers));
    }

    /**
     * Adds a row.
     *
     * @param cells the cells, one per column
     * @return this table
     */
    public Table row(String... cells) {
        Objects.requireNonNull(cells, "cells must not be null");
        if (cells.length != headers.size()) {
            throw new IllegalArgumentException(
                    "row has " + cells.length + " cells for " + headers.size() + " columns: " + List.of(cells));
        }
        rows.add(List.of(cells));
        return this;
    }

    /**
     * Returns whether any row was added.
     *
     * @return true when the table has no row
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Renders the table.
     *
     * @return the markdown, without a trailing newline
     */
    public String render() {
        List<String> written = new ArrayList<>();
        written.add(line(headers));
        written.add("|" + " --- |".repeat(headers.size()));
        rows.forEach(row -> written.add(line(row)));
        return String.join("\n", written);
    }

    private static String line(List<String> cells) {
        return cells.stream().map(cell -> " " + cellOf(cell) + " |").collect(Collectors.joining("", "|", ""));
    }

    private static String cellOf(String cell) {
        Objects.requireNonNull(cell, "cell must not be null");
        return cell.replace("|", "\\|").replaceAll("\\s*\\R\\s*", " ");
    }
}
