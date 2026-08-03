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

package io.hexaglue.maven;

import io.hexaglue.model.finding.Diagnostic;
import java.util.Objects;

/**
 * A configuration document that cannot be honoured as written.
 *
 * <p>It carries a coded diagnostic rather than a bare message: what the build prints about its own
 * configuration is documented and looked up like everything else HexaGlue reports.</p>
 *
 * @since 7.0.0
 */
public final class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Diagnostic diagnostic;

    ConfigException(Diagnostic diagnostic) {
        super(diagnostic.message());
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic must not be null");
    }

    ConfigException(Diagnostic diagnostic, Throwable cause) {
        super(diagnostic.message(), cause);
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic must not be null");
    }

    /**
     * Returns the coded diagnostic of what the document states and this cannot honour.
     *
     * @return the diagnostic
     */
    public Diagnostic diagnostic() {
        return diagnostic;
    }
}
