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

package io.hexaglue.model.arch;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Classification;
import java.util.Objects;
import java.util.Optional;

/**
 * The categorized fallback: a type of the analysis scope that reached no kind with sufficient
 * confidence. Every such type states why — category, reason, and the candidates kept in its
 * classification — so no type ever disappears silently.
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind UNCLASSIFIED
 * @param category why the type stayed unclassified
 * @param reason a human-readable explanation, when one was produced
 * @since 7.0.0
 */
public record UnclassifiedType(
        TypeId id,
        TypeStructure structure,
        Classification classification,
        UnclassifiedCategory category,
        Optional<String> reason)
        implements ArchType {

    /**
     * Validates the kind coherence of the verdict.
     */
    public UnclassifiedType {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        KindCoherence.require(ArchKind.UNCLASSIFIED, classification, id);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.UNCLASSIFIED;
    }

    /**
     * Why a type of the scope stayed unclassified.
     *
     * @since 7.0.0
     */
    public enum UnclassifiedCategory {

        /** Contradictory evidences with no structural tie-breaker. */
        CONFLICTING,

        /** A utility holder, not an architectural participant. */
        UTILITY,

        /** Outside the hexagon on purpose (tests, fixtures, tooling). */
        OUT_OF_SCOPE,

        /** Technical plumbing (configuration, framework wiring). */
        TECHNICAL,

        /** Competing candidates too close to decide; candidates are kept. */
        AMBIGUOUS,

        /** No usable signal at all. */
        UNKNOWN
    }
}
