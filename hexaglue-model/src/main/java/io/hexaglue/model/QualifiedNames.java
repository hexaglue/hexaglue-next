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

/**
 * Single implementation of qualified-name decomposition, shared by {@link TypeId} and
 * {@link TypeRef}. Nested types use the binary convention: {@code com.example.Order$OrderLine}.
 */
final class QualifiedNames {

    private QualifiedNames() {}

    /**
     * Returns the last name segment, after the final {@code .} or {@code $} separator.
     *
     * @param qualifiedName the qualified name to decompose
     * @return the simple name
     */
    static String simpleName(String qualifiedName) {
        int lastSeparator = Math.max(qualifiedName.lastIndexOf('.'), qualifiedName.lastIndexOf('$'));
        return lastSeparator >= 0 ? qualifiedName.substring(lastSeparator + 1) : qualifiedName;
    }

    /**
     * Returns the package of the top-level type: the text before the last {@code .} that precedes
     * any nesting separator. For {@code com.example.Order$OrderLine} this is {@code com.example},
     * not the enclosing type.
     *
     * @param qualifiedName the qualified name to decompose
     * @return the package name, or an empty string for an unpackaged type
     */
    static String packageName(String qualifiedName) {
        int firstNesting = qualifiedName.indexOf('$');
        String topLevel = firstNesting >= 0 ? qualifiedName.substring(0, firstNesting) : qualifiedName;
        int lastDot = topLevel.lastIndexOf('.');
        return lastDot >= 0 ? topLevel.substring(0, lastDot) : "";
    }
}
