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

package io.hexaglue.spi;

import io.hexaglue.model.finding.Diagnostic;

/**
 * Where a plugin says something about the run itself without giving up on it.
 *
 * <p>Failing already had a channel: a plugin that throws loses its contribution and the run says
 * so. What had none is the ordinary case of a backend that declines part of its work and can carry
 * on — a generator handed a type the analysis is not sure enough about writes no code for it, and
 * that has to be said, not passed over. Reporting is not failing: whatever a plugin reports here,
 * it goes on contributing.</p>
 *
 * <p>A diagnostic is coded, so what a plugin reports reads like everything else the run reports and
 * a host does not have to parse a sentence to know what happened.</p>
 *
 * @since 7.0.0
 */
@FunctionalInterface
public interface DiagnosticSink {

    /**
     * Reports something about the run.
     *
     * @param diagnostic what to say, coded
     */
    void report(Diagnostic diagnostic);
}
