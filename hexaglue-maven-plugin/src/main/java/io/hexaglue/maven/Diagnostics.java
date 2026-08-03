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

package io.hexaglue.maven;

import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;
import org.apache.maven.plugin.logging.Log;

/**
 * Says what a run left out, in the terms a build log can carry.
 *
 * <p>A degraded reading is said once and plainly, because it is rare and it explains wrong
 * verdicts. What was deliberately left out is <em>counted</em> rather than listed: a perimeter
 * doing its job leaves out most of a code base, and a build log burying its own conclusions under
 * thousands of expected exclusions says less than one that offers them on request.</p>
 *
 * <p>Counting it rather than staying silent is what a case study taught: on a project whose
 * adapters HexaGlue itself had generated into the sources, fifty-one types were left out as
 * generated code, and a report written against the rest looked complete.</p>
 *
 * <p>Every goal says it the same way. Three goals with three ideas of what is worth mentioning
 * would make the same run read differently depending on which one a build happened to invoke.</p>
 */
final class Diagnostics {

    private Diagnostics() {}

    /**
     * Reports what the reading and the classification left out.
     *
     * @param diagnostics what the run left out, in the order it left it out
     * @param log where to say it
     */
    static void report(List<Diagnostic> diagnostics, Log log) {
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        Objects.requireNonNull(log, "log must not be null");
        long expected = diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.INFO)
                .count();
        diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() != DiagnosticSeverity.INFO)
                .forEach(diagnostic -> log.warn(diagnostic.code() + ": " + diagnostic.message()));
        if (expected > 0) {
            log.info(expected + " type(s) were not analysed; run with -X to see which and why");
        }
        diagnostics.forEach(diagnostic -> log.debug(diagnostic.code() + ": " + diagnostic.message()));
    }
}
