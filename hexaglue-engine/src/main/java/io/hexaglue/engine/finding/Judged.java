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

package io.hexaglue.engine.finding;

import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.Finding;
import java.util.List;
import java.util.Objects;

/**
 * What the checks made of an architecture: what they found, and what they chose not to say.
 *
 * <p>The second half exists because a check falling silent is itself a fact about the run. A report
 * that simply showed fewer findings than the sources warrant would be indistinguishable from a
 * check that stopped working — so what was left unsaid, and on whose word, travels beside what was
 * said.</p>
 *
 * @param findings what the checks found, in a stable order
 * @param diagnostics what the checks left unsaid, and why
 * @since 7.0.0
 */
public record Judged(List<Finding> findings, List<Diagnostic> diagnostics) {

    /**
     * Validates and copies both lists.
     */
    public Judged {
        Objects.requireNonNull(findings, "findings must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        findings = List.copyOf(findings);
        diagnostics = List.copyOf(diagnostics);
    }
}
