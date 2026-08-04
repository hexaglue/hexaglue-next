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

package io.hexaglue.spi;

/**
 * Where a plugin hands over a Java type it wants written.
 *
 * <p>Kept apart from the documents a run also produces, because the host does two different things
 * with them: prose is written and read by people, whereas generated sources have to be written
 * somewhere the compiler will then be told about. A single sink would leave the host to tell the
 * two apart by looking at a path.</p>
 *
 * @since 7.0.0
 */
@FunctionalInterface
public interface SourceSink {

    /**
     * Hands a source file over to the run.
     *
     * @param source the type to write, named rather than placed
     */
    void emit(SourceFile source);
}
