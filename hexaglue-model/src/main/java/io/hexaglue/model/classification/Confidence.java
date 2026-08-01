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

/**
 * The single confidence scale of the product — validation gates, generation thresholds and
 * reports all read this one enum.
 *
 * @since 7.0.0
 */
public enum Confidence {

    /** The classification was declared by the user (configuration or explicit annotation). */
    EXPLICIT,

    /** Strong converging signals (framework knowledge, graph relations, structure). */
    HIGH,

    /** Plausible signals only (topology, naming). */
    MEDIUM,

    /** Weak or conflicting signals. */
    LOW;

    /**
     * Returns whether this confidence is strong enough to act on without confirmation.
     *
     * @return true for EXPLICIT and HIGH
     */
    public boolean isReliable() {
        return this == EXPLICIT || this == HIGH;
    }

    /**
     * Returns whether this confidence reaches the given threshold.
     *
     * @param threshold the minimum accepted confidence
     * @return true when this confidence is at least the threshold
     */
    public boolean isAtLeast(Confidence threshold) {
        return this.ordinal() <= threshold.ordinal();
    }
}
