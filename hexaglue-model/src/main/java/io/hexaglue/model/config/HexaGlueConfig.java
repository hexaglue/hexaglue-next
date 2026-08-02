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

import java.util.Objects;

/**
 * The typed configuration of an analysis: the perimeter, the intent the user declares out of the
 * code, the validation gates and the generation threshold. Plugin-specific options are not
 * represented here — they reach each plugin through the SPI, opaque to the model.
 *
 * <p>This record is the shape the strict YAML binding will fill: an unknown key or a malformed
 * value is the loader's error, never a silently ignored entry.</p>
 *
 * @param analysis the perimeter of the analysis
 * @param classification the kinds the user declares for their own types
 * @param validation the gates the validate goal applies
 * @param generation the certainty threshold of code generation
 * @since 7.0.0
 */
public record HexaGlueConfig(
        AnalysisScope analysis,
        ClassificationConfig classification,
        ValidationConfig validation,
        GenerationConfig generation) {

    /**
     * Validates the four blocks.
     */
    public HexaGlueConfig {
        Objects.requireNonNull(analysis, "analysis must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(validation, "validation must not be null");
        Objects.requireNonNull(generation, "generation must not be null");
    }

    /**
     * Returns the documented default posture: analyze everything, declare nothing, gate nothing,
     * generate at HIGH confidence.
     *
     * @return the default configuration
     */
    public static HexaGlueConfig defaults() {
        return new HexaGlueConfig(
                AnalysisScope.everything(),
                ClassificationConfig.empty(),
                ValidationConfig.defaults(),
                GenerationConfig.defaults());
    }
}
