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
 * Java declaration modifiers, for types, methods, constructors and fields.
 *
 * @since 7.0.0
 */
public enum Modifier {

    /** The {@code public} access modifier. */
    PUBLIC,

    /** The {@code protected} access modifier. */
    PROTECTED,

    /** The {@code private} access modifier. */
    PRIVATE,

    /** The {@code static} modifier. */
    STATIC,

    /** The {@code final} modifier. */
    FINAL,

    /** The {@code abstract} modifier. */
    ABSTRACT,

    /** The {@code native} modifier. */
    NATIVE,

    /** The {@code synchronized} modifier. */
    SYNCHRONIZED,

    /** The {@code transient} modifier. */
    TRANSIENT,

    /** The {@code volatile} modifier. */
    VOLATILE,

    /** The {@code strictfp} modifier. */
    STRICTFP,

    /** The {@code sealed} modifier (Java 17+). */
    SEALED,

    /** The {@code non-sealed} modifier (Java 17+). */
    NON_SEALED,

    /** The {@code default} modifier on interface methods. */
    DEFAULT
}
