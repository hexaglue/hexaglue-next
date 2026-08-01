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

package io.hexaglue.model.classification;

/**
 * The action a remediation hint proposes to make a classification explicit or stronger.
 *
 * @since 7.0.0
 */
public enum RemediationAction {

    /** Add an intent annotation on the type. */
    ADD_ANNOTATION,

    /** Declare the kind in the explicit classification configuration. */
    CONFIGURE_EXPLICIT,

    /** Rename the type to match the naming vocabulary. */
    RENAME,

    /** Move the type to the package its role expects. */
    MOVE_PACKAGE,

    /** Implement or extend a marker interface. */
    IMPLEMENT_INTERFACE,

    /** Exclude the type from the analysis scope. */
    EXCLUDE
}
