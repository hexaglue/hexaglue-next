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

package io.hexaglue.engine;

import io.hexaglue.knowledge.FrameworkKnowledge;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.config.HexaGlueConfig;
import java.util.Objects;

/**
 * What every rule may read and none may change: the code model, the framework knowledge, the
 * user's configuration and the perimeter derived from it.
 *
 * <p>The code model is the base stratum of the inference — the syntactic facts, already indexed
 * by the frontend. Keeping it here rather than copying it into the fact base is what keeps the
 * base to what the engine <em>concluded</em>, and the proofs to what is worth explaining.</p>
 *
 * @param code the analyzed sources, classpath stubs included
 * @param knowledge what the packs recognize
 * @param config the user's configuration
 * @param perimeter the types owed a verdict
 * @since 7.0.0
 */
public record EngineContext(CodeModel code, FrameworkKnowledge knowledge, HexaGlueConfig config, Perimeter perimeter) {

    /**
     * Validates that every component is present.
     */
    public EngineContext {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(knowledge, "knowledge must not be null");
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(perimeter, "perimeter must not be null");
    }

    /**
     * Assembles the context, deriving the perimeter from the configured analysis scope.
     *
     * @param code the analyzed sources
     * @param knowledge what the packs recognize
     * @param config the user's configuration
     * @return a new context
     */
    public static EngineContext of(CodeModel code, FrameworkKnowledge knowledge, HexaGlueConfig config) {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(config, "config must not be null");
        return new EngineContext(code, knowledge, config, Perimeter.of(code, config.analysis()));
    }
}
