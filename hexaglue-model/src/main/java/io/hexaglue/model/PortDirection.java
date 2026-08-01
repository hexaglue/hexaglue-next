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

package io.hexaglue.model;

/**
 * The direction of a port relative to the hexagon.
 *
 * @since 7.0.0
 */
public enum PortDirection {

    /** The outside world drives the hexagon through this port (primary / inbound). */
    DRIVING,

    /** The hexagon drives the outside world through this port (secondary / outbound). */
    DRIVEN
}
