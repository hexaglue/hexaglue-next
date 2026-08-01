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

import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One signal supporting a classification: its tier, its force, the fact that produced it, a
 * human-readable justification, and where it comes from.
 *
 * <p>Construction enforces the doctrine: the force can never exceed the ceiling of the tier —
 * a naming evidence, for instance, can never claim HIGH confidence.</p>
 *
 * @param tier the signal source tier (S1 to S6)
 * @param force the confidence this evidence carries, at most the tier's ceiling
 * @param fact the fact that produced the evidence (e.g. {@code SPRING_DATA_REPOSITORY(Order)})
 * @param justification the human-readable why
 * @param sourceLocation the source location of the signal, when known
 * @param relatedTypes the other types involved in the signal, in relevance order
 * @since 7.0.0
 */
public record Evidence(
        EvidenceTier tier,
        Confidence force,
        String fact,
        String justification,
        Optional<SourceLocation> sourceLocation,
        List<TypeId> relatedTypes) {

    /**
     * Validates the tier ceiling and the texts, and copies the related types.
     */
    public Evidence {
        Objects.requireNonNull(tier, "tier must not be null");
        Objects.requireNonNull(force, "force must not be null");
        Objects.requireNonNull(fact, "fact must not be null");
        Objects.requireNonNull(justification, "justification must not be null");
        Objects.requireNonNull(sourceLocation, "sourceLocation must not be null");
        Objects.requireNonNull(relatedTypes, "relatedTypes must not be null");
        if (fact.isBlank() || justification.isBlank()) {
            throw new IllegalArgumentException("fact and justification must not be blank");
        }
        if (force.ordinal() < tier.maxConfidence().ordinal()) {
            throw new IllegalArgumentException(
                    "evidence force " + force + " exceeds the " + tier.code() + " ceiling " + tier.maxConfidence());
        }
        relatedTypes = List.copyOf(relatedTypes);
    }

    /**
     * Creates an evidence without location or related types.
     *
     * @param tier the signal source tier
     * @param force the confidence carried, at most the tier's ceiling
     * @param fact the fact that produced the evidence
     * @param justification the human-readable why
     * @return a new Evidence
     */
    public static Evidence of(EvidenceTier tier, Confidence force, String fact, String justification) {
        return new Evidence(tier, force, fact, justification, Optional.empty(), List.of());
    }
}
