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
 * A condition a classified model is held to. Each one is armed on its own in the configuration,
 * and each answers about one type at a time, so a build that fails says which condition failed on
 * which type rather than that "validation failed".
 *
 * <p>The declaration order is the order refusals are reported in, from the type having no kind at
 * all to the kind being deduced rather than stated: what a reader should look at first comes
 * first.</p>
 *
 * @since 7.0.0
 */
public enum Gate {

    /** No kind could be decided for the type. */
    UNCLASSIFIED,

    /** The decision is less certain than the configuration accepts. */
    CONFIDENCE,

    /** The decision kept competing candidates rather than separating them. */
    AMBIGUOUS,

    /** The kind was deduced where the configuration requires the sources to state it. */
    INFERRED
}
