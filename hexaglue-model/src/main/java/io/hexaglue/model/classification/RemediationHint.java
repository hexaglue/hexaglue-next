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
     * Suggests adding an intent annotation.
     *
     * @param annotationSimpleName the annotation to add, without {@code @}
     * @param targetKind the kind the annotation declares
     * @return a new hint reaching EXPLICIT confidence
     */
    public static RemediationHint addAnnotation(String annotationSimpleName, ArchKind targetKind) {
        return new RemediationHint(
                RemediationAction.ADD_ANNOTATION,
                "Add @" + annotationSimpleName + " on the type",
                RemediationImpact.explicit(targetKind),
                Optional.of("@" + annotationSimpleName));
    }

    /**
     * Suggests declaring the kind in the explicit classification configuration.
     *
     * @param qualifiedName the type to declare
     * @param targetKind the declared kind
     * @return a new hint reaching EXPLICIT confidence
     */
    public static RemediationHint configureExplicit(String qualifiedName, ArchKind targetKind) {
        return new RemediationHint(
                RemediationAction.CONFIGURE_EXPLICIT,
                "Declare " + qualifiedName + " as " + targetKind + " in the explicit classification configuration",
                RemediationImpact.explicit(targetKind),
                Optional.empty());
    }
}
