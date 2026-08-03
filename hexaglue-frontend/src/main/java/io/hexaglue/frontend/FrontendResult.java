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

package io.hexaglue.frontend;

import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.finding.Diagnostic;
import java.util.List;
import java.util.Objects;

/**
 * One reading of the sources: the model of what was read, and the coded diagnostics accounting for
 * what was not.
 *
 * <p>The model is still complete or the call fails — a diagnostic never describes a half-built
 * model. It accounts for what the frontend deliberately left out: a type outside the configured
 * perimeter, a type a generator wrote. Nothing downstream can say it in its place, since the
 * engine only ever sees what was read; without this, the only trace of a narrower reading is a
 * smaller model, and a smaller model reads as a smaller code base.</p>
 *
 * <p>A diagnostic is never a verdict: no rule consumes this list, and the host alone decides how
 * loudly to say it.</p>
 *
 * @param code the model of what was read
 * @param diagnostics what the reading left out, in type order
 * @since 7.0.0
 */
public record FrontendResult(CodeModel code, List<Diagnostic> diagnostics) {

    /**
     * Validates both components and copies the diagnostics.
     */
    public FrontendResult {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        diagnostics = List.copyOf(diagnostics);
    }
}
