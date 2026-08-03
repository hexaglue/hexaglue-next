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

import java.util.Objects;

/**
 * Everything a plugin can hand over during one contribution.
 *
 * <p>One sink per kind of output, and no sink before a plugin emits into it: what a run can
 * receive is exactly what some backend produces.</p>
 *
 * @param documents where documentation and reports go
 * @since 7.0.0
 */
public record Sinks(DocumentSink documents) {

    /**
     * Validates the sink.
     */
    public Sinks {
        Objects.requireNonNull(documents, "documents must not be null");
    }
}
