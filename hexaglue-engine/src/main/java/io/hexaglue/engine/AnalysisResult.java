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

import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.finding.Diagnostic;
import java.util.List;
import java.util.Objects;

/**
 * One run of the engine: the classified model, and the coded diagnostics accounting for the types
 * it read without classifying.
 *
 * <p>The reading perimeter and the verdict perimeter are not the same, on purpose: a type outside
 * the configured scope is still read, because an adapter nobody classifies is what makes the port
 * it implements recognizable. What that costs is a type present in the sources, absent from the
 * model, and — without this — indistinguishable from one nobody ever wrote. The stage that leaves
 * a type out is the only one that can say so, which is why this sits here and not in the host.</p>
 *
 * <p>A diagnostic is never a verdict: no rule consumes this list, and the model is exactly what it
 * would have been without it.</p>
 *
 * @param model the classified model, one entry per type of the perimeter
 * @param diagnostics what was read and not classified, in type order
 * @since 7.0.0
 */
public record AnalysisResult(ArchModel model, List<Diagnostic> diagnostics) {

    /**
     * Validates both components and copies the diagnostics.
     */
    public AnalysisResult {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        diagnostics = List.copyOf(diagnostics);
    }
}
