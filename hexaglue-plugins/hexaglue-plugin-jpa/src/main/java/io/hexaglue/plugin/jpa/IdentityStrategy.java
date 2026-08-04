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

package io.hexaglue.plugin.jpa;

/**
 * Who decides the value of an identity when the domain does not say.
 *
 * <p>The default is {@link #ASSIGNED}, and that is a statement about the domain rather than about
 * the database: a domain that owns its identities builds them itself — an {@code OrderId} exists
 * before there is a row — so the store records the value it is handed instead of inventing one.
 * The other strategies are for domains that have handed identity to the database, and asking for
 * one of them is asking the store to fill a field the domain left empty.</p>
 *
 * @since 7.0.0
 */
public enum IdentityStrategy {

    /** The domain hands the value over; the store writes it as it is. */
    ASSIGNED,

    /** The store picks whichever of its own strategies fits. */
    AUTO,

    /** An identity column of the table. */
    IDENTITY,

    /** A sequence of the database. */
    SEQUENCE,

    /** A table the database keeps for handing out identities. */
    TABLE
}
