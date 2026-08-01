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

package io.hexaglue.model.finding;

/**
 * The severity of a tool diagnostic. Findings judge the analyzed architecture; diagnostics
 * report the tool's own condition on a compiler-like scale.
 *
 * @since 7.0.0
 */
public enum DiagnosticSeverity {

    /** The tool could not complete an operation — analysis failed, generation refused. */
    ERROR,

    /** The tool completed in a degraded way and the result deserves attention. */
    WARNING,

    /** Neutral information about what the tool did. */
    INFO
}
