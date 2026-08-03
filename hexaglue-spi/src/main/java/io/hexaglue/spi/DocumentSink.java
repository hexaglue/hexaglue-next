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
 * Where a plugin hands over a document it wants written.
 *
 * <p>Emitting is not writing: nothing touches a disk until the host decides it should. That is
 * what lets a contribution be replayed, compared or asserted on without a temporary directory.</p>
 *
 * @since 7.0.0
 */
@FunctionalInterface
public interface DocumentSink {

    /**
     * Hands a document over to the run.
     *
     * @param document what to write and where, relative to the host's output directory
     */
    void emit(Document document);
}
