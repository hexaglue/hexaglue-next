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

import io.hexaglue.model.TypeRef;
import java.util.List;
import java.util.Objects;

/**
 * A method or constructor parameter.
 *
 * @param name the parameter name
 * @param type the parameter type
 * @param annotations the annotations on this parameter, in declaration order
 * @since 7.0.0
 */
public record Parameter(String name, TypeRef type, List<Annotation> annotations) {

    /**
     * Validates the name and copies the annotations.
     */
    public Parameter {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        annotations = List.copyOf(annotations);
    }

    /**
     * Creates a parameter without annotations.
     *
     * @param name the parameter name
     * @param type the parameter type
     * @return a new Parameter
     */
    public static Parameter of(String name, TypeRef type) {
        return new Parameter(name, type, List.of());
    }

    /**
     * Returns whether this parameter carries the given annotation.
     *
     * @param qualifiedName the fully qualified annotation type name
     * @return true when present
     */
    public boolean hasAnnotation(String qualifiedName) {
        return annotations.stream().anyMatch(annotation -> annotation.is(qualifiedName));
    }
}
