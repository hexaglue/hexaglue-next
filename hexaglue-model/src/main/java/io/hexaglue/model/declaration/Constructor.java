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

import io.hexaglue.model.EnumSets;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A constructor declaration.
 *
 * @param parameters the parameters, in declaration order
 * @param modifiers the constructor modifiers, iterated in natural order
 * @param annotations the annotations on this constructor, in declaration order
 * @param documentation the constructor's documentation, when present
 * @param thrownExceptions the declared thrown exception types, in declaration order
 * @param sourceLocation the source location, when known
 * @since 7.0.0
 */
public record Constructor(
        List<Parameter> parameters,
        Set<Modifier> modifiers,
        List<Annotation> annotations,
        Optional<String> documentation,
        List<TypeRef> thrownExceptions,
        Optional<SourceLocation> sourceLocation) {

    /**
     * Defensively copies every collection.
     */
    public Constructor {
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(modifiers, "modifiers must not be null");
        Objects.requireNonNull(annotations, "annotations must not be null");
        Objects.requireNonNull(documentation, "documentation must not be null");
        Objects.requireNonNull(thrownExceptions, "thrownExceptions must not be null");
        Objects.requireNonNull(sourceLocation, "sourceLocation must not be null");
        parameters = List.copyOf(parameters);
        modifiers = EnumSets.ordered(modifiers);
        annotations = List.copyOf(annotations);
        thrownExceptions = List.copyOf(thrownExceptions);
    }

    /**
     * Creates a constructor with the given parameters and no other information.
     *
     * @param parameters the parameters, in declaration order
     * @return a new Constructor
     */
    public static Constructor of(List<Parameter> parameters) {
        return new Constructor(parameters, Set.of(), List.of(), Optional.empty(), List.of(), Optional.empty());
    }

    /**
     * Creates a no-argument constructor.
     *
     * @return a new Constructor without parameters
     */
    public static Constructor noArg() {
        return of(List.of());
    }

    /**
     * Returns a compact display signature: the parameter type simple names.
     *
     * @return the signature (e.g. {@code (OrderId, String)})
     */
    public String signature() {
        String parameterTypes = parameters.stream()
                .map(parameter -> parameter.type().simpleName())
                .collect(Collectors.joining(", "));
        return "(" + parameterTypes + ")";
    }
}
