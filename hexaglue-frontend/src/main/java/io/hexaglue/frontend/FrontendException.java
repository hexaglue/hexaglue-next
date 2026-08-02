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

package io.hexaglue.frontend;

import io.hexaglue.model.finding.Diagnostic;
import java.util.Objects;

/**
 * Raised when the sources cannot be read into a code model.
 *
 * <p>The failure carries a coded {@link Diagnostic} rather than a bare message, so a host can
 * report it the same way it reports everything else the tool has to say. An analysis that cannot
 * read its input never answers with an empty model: an empty model reads as a clean, unremarkable
 * code base.</p>
 *
 * @since 7.0.0
 */
public final class FrontendException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Diagnostic diagnostic;

    /**
     * Creates a failure carrying its diagnostic.
     *
     * @param diagnostic what went wrong, coded and localized
     * @param cause the underlying failure, when there is one
     */
    FrontendException(Diagnostic diagnostic, Throwable cause) {
        super(diagnostic.message(), cause);
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic must not be null");
    }

    /**
     * Creates a failure carrying its diagnostic.
     *
     * @param diagnostic what went wrong, coded and localized
     */
    FrontendException(Diagnostic diagnostic) {
        super(diagnostic.message());
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic must not be null");
    }

    /**
     * Returns what went wrong, as a diagnostic the host can report.
     *
     * @return the diagnostic
     */
    public Diagnostic diagnostic() {
        return diagnostic;
    }
}
