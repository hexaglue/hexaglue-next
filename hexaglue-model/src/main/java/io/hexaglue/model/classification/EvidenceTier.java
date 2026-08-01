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

import java.util.Objects;

/**
 * The hierarchy of signal sources, by decreasing strength. The declaration order is the
 * lexicographic weighing order of the decision aggregator, and each tier carries the ceiling its
 * evidences can never exceed — naming, in particular, can inform but never decide.
 *
 * @since 7.0.0
 */
public enum EvidenceTier {

    /** S1 — declared intent: explicit configuration, intent annotations by exact qualified name. */
    DECLARED_INTENT("S1", Confidence.EXPLICIT),

    /** S2 — framework knowledge: what a supertype or annotation from a known framework implies. */
    FRAMEWORK_KNOWLEDGE("S2", Confidence.HIGH),

    /** S3 — graph relations: how the type is used by already-classified neighbours. */
    GRAPH_RELATION("S3", Confidence.HIGH),

    /** S4 — local structure: record-ness, immutability, identity field, shape. */
    LOCAL_STRUCTURE("S4", Confidence.HIGH),

    /** S5 — topology: packages, module roles. */
    TOPOLOGY("S5", Confidence.MEDIUM),

    /** S6 — naming: suffixes and verbs from the naming vocabulary, never decisive alone. */
    NAMING("S6", Confidence.MEDIUM);

    private final String code;
    private final Confidence maxConfidence;

    EvidenceTier(String code, Confidence maxConfidence) {
        this.code = Objects.requireNonNull(code);
        this.maxConfidence = Objects.requireNonNull(maxConfidence);
    }

    /**
     * Returns the compact tier code used in traces and explanations.
     *
     * @return the code (S1 to S6)
     */
    public String code() {
        return code;
    }

    /**
     * Returns the strongest confidence an evidence of this tier may carry.
     *
     * @return the confidence ceiling
     */
    public Confidence maxConfidence() {
        return maxConfidence;
    }
}
