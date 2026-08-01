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
 * The typed relations of the code graph. Every edge is a syntactic fact read from a declaration —
 * never an interpretation.
 *
 * @since 7.0.0
 */
public enum EdgeKind {

    /** The source class extends the target class. */
    EXTENDS,

    /** The source type implements (or the source interface extends) the target interface. */
    IMPLEMENTS,

    /** The source sealed type permits the target subtype. */
    PERMITS,

    /** The source type is annotated with the target annotation type. */
    ANNOTATED_BY,

    /** The source type declares the target nested type. */
    DECLARES,

    /** A field of the source type is declared with the target type. */
    FIELD_TYPE,

    /** A method of the source type returns the target type. */
    RETURN_TYPE,

    /** A method or constructor of the source type takes the target type as parameter. */
    PARAMETER_TYPE,

    /** A method or constructor of the source type declares the target exception type. */
    THROWS_TYPE,

    /** The target type appears as a type argument in a declaration of the source type. */
    TYPE_ARGUMENT,

    /** A method body of the source type invokes a member of the target type (body fact). */
    INVOKES,

    /** A method body of the source type instantiates the target type (body fact). */
    INSTANTIATES
}
