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

import io.hexaglue.engine.Analysis;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.Validation;
import io.hexaglue.frontend.FrontendResult;
import io.hexaglue.frontend.SpoonFrontend;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.finding.Diagnostic;
import java.util.List;
import java.util.Objects;
import org.apache.maven.project.MavenProject;

/**
 * One run of the whole chain over one project: read the sources, classify them, hold the result to
 * the configured gates.
 *
 * <p>It lives beside the goal rather than inside it because a goal is an adapter — parameters in,
 * log lines and an exit condition out — and everything worth testing without a running build is
 * here. Nothing in it is Maven-specific but where the sources are, which is the one thing the
 * project knows and the analysis does not.</p>
 */
final class ProjectAnalysis {

    private ProjectAnalysis() {}

    /**
     * Runs the chain over a project.
     *
     * @param project the project being built
     * @param config what the build states about the analysis
     * @return the model, what the reading left out, and what the gates made of it
     */
    static Result run(MavenProject project, HexaGlueConfig config) {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(config, "config must not be null");
        FrontendResult read = SpoonFrontend.analyze(ProjectSources.request(project, config.analysis()));
        ArchModel model = Analysis.analyze(EngineContext.of(read.code(), KnowledgePacks.embedded(), config));
        return new Result(model, read.diagnostics(), Validation.of(model, config.validation()));
    }

    /**
     * What one run produced.
     *
     * @param model the classified model
     * @param diagnostics what the reading left out
     * @param validation what the gates made of the model
     */
    record Result(ArchModel model, List<Diagnostic> diagnostics, Validation validation) {

        /**
         * Validates and copies the components.
         */
        Result {
            Objects.requireNonNull(model, "model must not be null");
            Objects.requireNonNull(diagnostics, "diagnostics must not be null");
            Objects.requireNonNull(validation, "validation must not be null");
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
