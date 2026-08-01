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

package io.hexaglue.model.config;

import io.hexaglue.model.classification.Confidence;
import java.util.Objects;

/**
 * The certainty threshold of code generation: below it, a generating plugin emits a diagnostic
 * with the type's remediation instead of wrong code. The default is HIGH — generated code claims
 * strong grounds.
 *
 * <p>This record is shape only: enforcing the threshold belongs to the generating plugins
 * through the SPI.</p>
 *
 * @param minConfidence the weakest confidence a generating plugin accepts
 * @since 7.0.0
 */
public record GenerationConfig(Confidence minConfidence) {

    /**
     * Validates the threshold.
     */
    public GenerationConfig {
        Objects.requireNonNull(minConfidence, "minConfidence must not be null");
    }

    /**
     * Returns the default threshold: HIGH.
     *
     * @return the default generation configuration
     */
    public static GenerationConfig defaults() {
        return new GenerationConfig(Confidence.HIGH);
    }
}
