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

package io.hexaglue.model.classification;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import java.util.Objects;
import java.util.Optional;

/**
 * A concrete, type-specific suggestion to make a classification explicit or stronger — never a
 * generic three-line boilerplate.
 *
 * @param action the kind of change proposed
 * @param description the human-readable suggestion
 * @param impact what applying the suggestion would change
 * @param codeSnippet a ready-to-paste snippet, when one makes sense
 * @since 7.0.0
 */
public record RemediationHint(
        RemediationAction action, String description, RemediationImpact impact, Optional<String> codeSnippet) {

    /**
     * Validates the components.
     */
    public RemediationHint {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(impact, "impact must not be null");
        Objects.requireNonNull(codeSnippet, "codeSnippet must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }

    /**
     * Suggests adding an intent annotation, named in full.
     *
     * <p>The qualified name is not decoration: a hint reading {@code Add @Entity} on a type that
     * already carries {@code jakarta.persistence.Entity} would suggest what the reader believes is
     * already done. The snippet carries the import for the same reason.</p>
     *
     * @param annotation the annotation to add, by qualified name
     * @param targetKind the kind the annotation declares
     * @return a new hint reaching EXPLICIT confidence
     * @throws IllegalArgumentException when the annotation is named without its package
     */
    public static RemediationHint addAnnotation(TypeId annotation, ArchKind targetKind) {
        Objects.requireNonNull(annotation, "annotation must not be null");
        if (annotation.packageName().isEmpty()) {
            throw new IllegalArgumentException(
                    "the annotation must be qualified to be unambiguous, got: " + annotation.qualifiedName());
        }
        String importedName = annotation.qualifiedName().replace('$', '.');
        return new RemediationHint(
                RemediationAction.ADD_ANNOTATION,
                "Add @" + annotation.qualifiedName() + " on the type",
                RemediationImpact.explicit(targetKind),
                Optional.of("import " + importedName + ";\n\n@" + annotation.simpleName()));
    }

    /**
     * Suggests declaring the kind in the explicit classification configuration.
     *
     * @param type the type to declare
     * @param targetKind the declared kind
     * @return a new hint reaching EXPLICIT confidence
     */
    public static RemediationHint configureExplicit(TypeId type, ArchKind targetKind) {
        Objects.requireNonNull(type, "type must not be null");
        return new RemediationHint(
                RemediationAction.CONFIGURE_EXPLICIT,
                "Declare " + type.qualifiedName() + " as " + targetKind
                        + " in the explicit classification configuration",
                RemediationImpact.explicit(targetKind),
                Optional.empty());
    }
}
