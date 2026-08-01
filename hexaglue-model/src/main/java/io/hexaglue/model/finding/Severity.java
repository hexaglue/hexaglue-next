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
 * The severity of a finding, from most to least severe. The audit reports it; the validation
 * gates consume it, with per-code thresholds overriding the default of a rule.
 *
 * @since 7.0.0
 */
public enum Severity {

    /** The build must fail, not overridable — a violation that corrupts the architecture. */
    BLOCKER,

    /** The build fails unless the configuration explicitly allows it — a serious violation. */
    CRITICAL,

    /** The build continues but the violation deserves a fix — an important issue. */
    MAJOR,

    /** A low-priority issue, nice to fix. */
    MINOR,

    /** Informational only, no action required. */
    INFO
}
