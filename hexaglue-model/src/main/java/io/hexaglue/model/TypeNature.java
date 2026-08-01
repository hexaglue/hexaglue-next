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
 * The Java form of a type declaration.
 *
 * <p>The nature is a syntactic fact, not a classification: a record can be a value object, an
 * identifier or plain data — the nature only says it was declared with {@code record}.</p>
 *
 * @since 7.0.0
 */
public enum TypeNature {

    /** A class that is not a record or an enum. */
    CLASS,

    /** An interface. */
    INTERFACE,

    /** A record (Java 16+). */
    RECORD,

    /** An enum. */
    ENUM,

    /** An annotation type. */
    ANNOTATION
}
