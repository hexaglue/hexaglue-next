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

import io.hexaglue.model.finding.Diagnostic;
import java.util.Objects;

/**
 * Raised when a stated option has a shape the plugin cannot read: a flag that is neither true nor
 * false, a count that is not a number.
 *
 * <p>Distinct from a plugin simply failing, because the cause is in the configuration and the fix
 * belongs to whoever wrote it. The failure carries its coded {@link Diagnostic} so the run reports
 * the key and the value rather than a stack trace.</p>
 *
 * @since 7.0.0
 */
public final class PluginConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Diagnostic diagnostic;

    /**
     * Creates a failure carrying its diagnostic.
     *
     * @param diagnostic what could not be read, coded
     * @param cause the underlying failure, when reading the value raised one
     */
    PluginConfigException(Diagnostic diagnostic, Throwable cause) {
        super(diagnostic.message(), cause);
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic must not be null");
    }

    /**
     * Creates a failure carrying its diagnostic.
     *
     * @param diagnostic what could not be read, coded
     */
    PluginConfigException(Diagnostic diagnostic) {
        super(diagnostic.message());
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic must not be null");
    }

    /**
     * Returns what could not be read, as a diagnostic the run can report.
     *
     * @return the diagnostic
     */
    public Diagnostic diagnostic() {
        return diagnostic;
    }
}
