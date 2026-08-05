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

package io.hexaglue.engine;

/**
 * What a {@link Relation} says. The list grows with the rules that need to state something: a
 * relation exists because a consumer of the model would otherwise have to guess it.
 *
 * @since 7.0.0
 */
public enum RelationKind {

    /** The subject is a driven port through which the object aggregate is stored and retrieved. */
    MANAGES,

    /** The identity of the subject is carried by the object type. */
    IDENTIFIED_BY,

    /** The subject is made of the object type, which lives and dies inside it. */
    OWNS,

    /** The subject aggregate hands the object type back as something that has happened. */
    ANNOUNCES,

    /** The use cases the subject driving port declares are about the object aggregate. */
    CONCERNS
}
