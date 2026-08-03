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
import java.util.List;
import java.util.Objects;

/**
 * One pass of the plugins over a model: what they produced, what ran, what did not, and why.
 *
 * <p>A run never fails as a whole. A plugin that throws, links against something that is not
 * there, or is handed an option nobody declared costs its own contribution and the contributions
 * that depended on it — never the others. Whoever hosts the run decides what to do with the
 * diagnostics; the contract only owes an account that leaves nothing silent.</p>
 *
 * @param documents what the plugins handed over, in execution order then emission order
 * @param diagnostics what the run refused or survived, coded
 * @param executed the plugins that ran, in execution order
 * @param skipped the plugins that did not run, in identifier order
 * @since 7.0.0
 */
public record PluginRun(
        List<Document> documents, List<Diagnostic> diagnostics, List<String> executed, List<String> skipped) {

    /**
     * Validates every component and copies the collections.
     */
    public PluginRun {
        Objects.requireNonNull(documents, "documents must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        Objects.requireNonNull(executed, "executed must not be null");
        Objects.requireNonNull(skipped, "skipped must not be null");
        documents = List.copyOf(documents);
        diagnostics = List.copyOf(diagnostics);
        executed = List.copyOf(executed);
        skipped = List.copyOf(skipped);
    }
}
