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
 * Stable identity of a type, independent of its classification.
 *
 * <p>If heuristics evolve and {@code Order} is reclassified from ENTITY to AGGREGATE_ROOT, its
 * TypeId does not change: identity is the fully qualified name, nothing else. Nested types use the
 * binary convention ({@code com.example.Order$OrderLine}) so that a nested type and a same-named
 * top-level type never collide.</p>
 *
 * <p>The qualified name must be non-null and non-blank; construction rejects anything else.</p>
 *
 * @param qualifiedName the fully qualified name of the type (e.g. {@code com.example.Order})
 * @since 7.0.0
 */
// The record-generated equals/hashCode compare the single component that compareTo orders on,
// so the Comparable consistency contract holds without explicit overrides.
@SuppressWarnings("PMD.OverrideBothEqualsAndHashCodeOnComparable")
public record TypeId(String qualifiedName) implements Comparable<TypeId> {

    /**
     * Validates that the qualified name is non-null and non-blank.
     */
    public TypeId {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        if (qualifiedName.isBlank()) {
            throw new IllegalArgumentException("qualifiedName must not be blank");
        }
    }

    /**
     * Creates a TypeId from a qualified name.
     *
     * @param qualifiedName the fully qualified name (e.g. {@code com.example.Order})
     * @return a new TypeId
     */
    public static TypeId of(String qualifiedName) {
        return new TypeId(qualifiedName);
    }

    /**
     * Returns the simple name: the segment after the last {@code .} or {@code $}.
     *
     * @return the simple name (e.g. {@code OrderLine} for {@code com.example.Order$OrderLine})
     */
    public String simpleName() {
        return QualifiedNames.simpleName(qualifiedName);
    }

    /**
     * Returns the package of the top-level type. For a nested type this is the package of its
     * outermost enclosing type, not the enclosing type itself.
     *
     * @return the package name, or an empty string for an unpackaged type
     */
    public String packageName() {
        return QualifiedNames.packageName(qualifiedName);
    }

    /**
     * Returns whether this id designates a nested type.
     *
     * @return true when the qualified name carries a nesting separator
     */
    public boolean isNested() {
        return qualifiedName.indexOf('$') >= 0;
    }

    @Override
    public int compareTo(TypeId other) {
        return qualifiedName.compareTo(other.qualifiedName);
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
