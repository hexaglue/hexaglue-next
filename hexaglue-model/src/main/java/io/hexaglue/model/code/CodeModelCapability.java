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

package io.hexaglue.model.code;

/**
 * Optional frontend capabilities recorded on the code model, so a consumer can distinguish
 * "no facts were extracted" from "extraction was off".
 *
 * @since 7.0.0
 */
public enum CodeModelCapability {

    /** Method bodies were traversed: invocation and instantiation facts are available. */
    METHOD_BODIES
}
