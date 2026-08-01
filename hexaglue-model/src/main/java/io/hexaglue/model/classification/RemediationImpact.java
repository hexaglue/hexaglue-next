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

/**
 * What applying a remediation would change: the resulting kind and confidence.
 *
 * @param resultingKind the kind the type would be classified as
 * @param resultingConfidence the confidence the classification would reach
 * @param description the human-readable effect
 * @since 7.0.0
 */
public record RemediationImpact(ArchKind resultingKind, Confidence resultingConfidence, String description) {

    /**
     * Validates the components.
     */
    public RemediationImpact {
        Objects.requireNonNull(resultingKind, "resultingKind must not be null");
        Objects.requireNonNull(resultingConfidence, "resultingConfidence must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }

    /**
     * Creates the impact of making a classification explicit.
     *
     * @param kind the kind the type would be declared as
     * @return an impact reaching EXPLICIT confidence
     */
    public static RemediationImpact explicit(ArchKind kind) {
        return new RemediationImpact(
                kind, Confidence.EXPLICIT, "Would be classified as " + kind + " with EXPLICIT confidence");
    }

    /**
     * Creates the impact of strengthening a classification.
     *
     * @param kind the kind the type would keep
     * @param confidence the confidence the classification would reach
     * @return an impact reaching the given confidence
     */
    public static RemediationImpact improved(ArchKind kind, Confidence confidence) {
        return new RemediationImpact(kind, confidence, "Would improve confidence to " + confidence);
    }
}
