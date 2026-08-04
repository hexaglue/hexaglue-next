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

package io.hexaglue.plugin.jpa;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * How a Java name becomes a database name, in one place.
 *
 * <p>There is exactly one way of writing a name in snake case here, and it is used by everything
 * that needs one. The carrière had two — one that handled runs of capitals and one that did not —
 * and the shorter one won by being the one a caller happened to reach for, which cost every column
 * of every value object both its capitals and its escaping. Two implementations of one idea do not
 * disagree loudly; they disagree in whichever call sites picked the wrong one.</p>
 *
 * <p>Reserved words are dealt with differently on either side, on purpose. A table is
 * <strong>pluralised</strong>, because {@code orders} reads like a table and needs no quoting on
 * any engine; a column is <strong>suffixed</strong>, because pluralising a column would rename the
 * thing it holds.</p>
 */
final class SqlNames {

    /**
     * The words an engine is liable to refuse as a bare identifier. Not a complete list of any
     * dialect's keywords — the ones a domain actually names things after.
     */
    private static final List<String> RESERVED = List.of(
            "check",
            "column",
            "constraint",
            "default",
            "from",
            "grant",
            "group",
            "index",
            "join",
            "key",
            "limit",
            "offset",
            "order",
            "revoke",
            "role",
            "select",
            "session",
            "table",
            "transaction",
            "user",
            "value",
            "values",
            "where");

    private SqlNames() {}

    /**
     * Returns the snake case of a Java name.
     *
     * <p>Two boundaries make an underscore, not one: the ordinary {@code camelCase} to
     * {@code camel_case}, and the end of a run of capitals — {@code XMLParser} is
     * {@code xml_parser} rather than {@code xmlparser}.</p>
     *
     * @param name the Java name
     * @return the same name in snake case
     */
    static String snake(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the table a type is stored in.
     *
     * @param typeName the simple name of the type
     * @param prefix what every table of this project is prefixed with, possibly empty
     * @return the table name
     */
    static String table(String typeName, String prefix) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        return prefix + unreserved(snake(typeName));
    }

    /**
     * A table gets out of the way by being pluralised, which reads like a table and needs no
     * quoting. Twice, that is not enough: {@code values} is already plural, and {@code value}
     * pluralises into {@code values}, which is reserved in its own right. Both end up suffixed —
     * the point is a name an engine accepts, and the pluralising is only the pleasant way of
     * getting one.
     */
    private static String unreserved(String table) {
        if (!isReserved(table)) {
            return table;
        }
        String plural = table + "s";
        return table.endsWith("s") || isReserved(plural) ? table + "_tbl" : plural;
    }

    /**
     * Returns the column a field is stored in.
     *
     * @param fieldName the field name
     * @return the column name
     */
    static String column(String fieldName) {
        String column = snake(fieldName);
        return isReserved(column) ? column + "_col" : column;
    }

    /**
     * Answers whether a name would have to be quoted to be used as it is.
     *
     * @param name the name, already in snake case
     * @return true when an engine is liable to refuse it
     */
    static boolean isReserved(String name) {
        return RESERVED.contains(name);
    }

    /**
     * Returns the words this plugin escapes, in the order they are declared.
     *
     * @return the reserved words
     */
    static List<String> reservedWords() {
        return RESERVED;
    }
}
