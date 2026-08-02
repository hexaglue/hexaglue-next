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

import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import java.util.Objects;

/**
 * Raised when the engine cannot reach a verdict it can stand behind.
 *
 * <p>Both causes are contract breaches by a rule, and both stop the analysis rather than
 * degrade it: a model built on a loop that never converged, or on a rule whose declaration
 * cannot be trusted, is a model whose verdicts mean nothing.</p>
 *
 * @since 7.0.0
 */
public final class EngineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The saturation loop hit its round ceiling: a rule keeps deriving facts it never held. */
    static final IssueCode NO_CONVERGENCE = IssueCode.of("HG-ENGINE-001");

    /** A rule derived a predicate it did not declare writing, which the scheduler relies on. */
    static final IssueCode UNDECLARED_PREDICATE = IssueCode.of("HG-ENGINE-002");

    private final transient Diagnostic diagnostic;

    private EngineException(Diagnostic diagnostic) {
        super(diagnostic.message());
        this.diagnostic = diagnostic;
    }

    /**
     * Creates a failure carrying a coded diagnostic.
     *
     * @param code the published issue code
     * @param message what went wrong, in plain terms
     * @return a new EngineException
     */
    static EngineException of(IssueCode code, String message) {
        Objects.requireNonNull(code, "code must not be null");
        return new EngineException(
                Diagnostic.builder(code, DiagnosticSeverity.ERROR, message).build());
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
