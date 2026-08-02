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

package io.hexaglue.knowledge;

import io.hexaglue.model.finding.Diagnostic;
import java.util.Objects;

/**
 * Raised when a knowledge pack cannot be read.
 *
 * <p>The failure carries a coded {@link Diagnostic}, and it is a failure rather than a skipped
 * entry: knowledge silently dropped would come back as a type quietly classified on its name.</p>
 *
 * @since 7.0.0
 */
public final class KnowledgeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Diagnostic diagnostic;

    /**
     * Creates a failure carrying its diagnostic.
     *
     * @param diagnostic what went wrong, coded
     * @param cause the underlying failure, when there is one
     */
    KnowledgeException(Diagnostic diagnostic, Throwable cause) {
        super(diagnostic.message(), cause);
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic must not be null");
    }

    /**
     * Creates a failure carrying its diagnostic.
     *
     * @param diagnostic what went wrong, coded
     */
    KnowledgeException(Diagnostic diagnostic) {
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
