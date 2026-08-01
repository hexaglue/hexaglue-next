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

package io.hexaglue.model.declaration;

import io.hexaglue.model.TypeId;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A single use of an annotation, with its attribute values fully typed.
 *
 * <p>There is exactly one representation of the values — {@link AnnotationValue} — so nothing is
 * ever degraded to a string on the way to the plugins. Attributes iterate in attribute-name order,
 * which makes any rendering of an annotation deterministic by construction.</p>
 *
 * @param qualifiedName the fully qualified annotation type name
 * @param values the attribute values by attribute name, iterated in name order
 * @since 7.0.0
 */
public record Annotation(String qualifiedName, Map<String, AnnotationValue> values) {

    /**
     * Validates the name and copies the values into a name-ordered immutable map.
     */
    public Annotation {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(values, "values must not be null");
        if (qualifiedName.isBlank()) {
            throw new IllegalArgumentException("qualifiedName must not be blank");
        }
        values = Collections.unmodifiableSortedMap(new TreeMap<>(values));
    }

    /**
     * Creates an annotation use without attributes.
     *
     * @param qualifiedName the fully qualified annotation type name
     * @return a new Annotation
     */
    public static Annotation of(String qualifiedName) {
        return new Annotation(qualifiedName, Map.of());
    }

    /**
     * Creates an annotation use with typed attribute values.
     *
     * @param qualifiedName the fully qualified annotation type name
     * @param values the attribute values by attribute name
     * @return a new Annotation
     */
    public static Annotation of(String qualifiedName, Map<String, AnnotationValue> values) {
        return new Annotation(qualifiedName, values);
    }

    /**
     * Returns the simple name of the annotation type.
     *
     * @return the simple name (e.g. {@code Table} for {@code jakarta.persistence.Table})
     */
    public String simpleName() {
        return TypeId.of(qualifiedName).simpleName();
    }

    /**
     * Returns whether an attribute is present.
     *
     * @param name the attribute name
     * @return true when the attribute has a value
     */
    public boolean hasValue(String name) {
        return values.containsKey(name);
    }

    /**
     * Returns the typed value of an attribute, when present.
     *
     * @param name the attribute name
     * @return the value, or empty when the attribute is absent
     */
    public Optional<AnnotationValue> value(String name) {
        return Optional.ofNullable(values.get(name));
    }

    /**
     * Returns whether this annotation is the given type.
     *
     * @param annotationQualifiedName the fully qualified annotation type name to compare with
     * @return true on exact match
     */
    public boolean is(String annotationQualifiedName) {
        return qualifiedName.equals(annotationQualifiedName);
    }
}
