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

import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.finding.Finding;
import java.util.List;
import java.util.Objects;

/**
 * Everything one plugin is handed for one run: what the analysis read, what it concluded, what the
 * author asked for, and where the result goes.
 *
 * <p>The findings travel here rather than being computed by whoever wants them, because a plugin
 * that judged an architecture itself would be a second judge — and the day it disagreed with the
 * gate, a build would pass while its own report condemned it. What a plugin does with them is
 * presentation: order them, count them, explain them. Never re-decide them.</p>
 *
 * @param model the classified model, complete and immutable
 * @param findings what the checks made of it, in a stable order
 * @param config the options stated for this plugin, opaque to every other stage
 * @param sinks where the contribution goes
 * @since 7.0.0
 */
public record Contribution(ArchModel model, List<Finding> findings, PluginConfig config, Sinks sinks) {

    /**
     * Validates every component and copies the findings.
     */
    public Contribution {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(sinks, "sinks must not be null");
        findings = List.copyOf(findings);
    }

    /**
     * Hands a document over to the run.
     *
     * @param path where it goes, relative to the host's output directory
     * @param content the whole content of the document
     */
    public void emit(String path, String content) {
        sinks.documents().emit(new Document(path, content));
    }
}
