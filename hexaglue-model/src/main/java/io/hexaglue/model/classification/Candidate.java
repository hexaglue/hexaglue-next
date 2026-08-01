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
import java.util.List;
import java.util.Objects;

/**
 * A kind that competed for a type, kept with its score and evidences when the decision could not
 * separate candidates with enough margin — ambiguity never destroys information.
 *
 * @param kind the candidate kind
 * @param score the aggregated score the candidate reached
 * @param evidences the evidences supporting this candidate, in tier order
 * @since 7.0.0
 */
public record Candidate(ArchKind kind, int score, List<Evidence> evidences) {

    /**
     * Validates the score and copies the evidences.
     */
    public Candidate {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(evidences, "evidences must not be null");
        if (score < 0) {
            throw new IllegalArgumentException("score must be >= 0, got " + score);
        }
        evidences = List.copyOf(evidences);
    }
}
