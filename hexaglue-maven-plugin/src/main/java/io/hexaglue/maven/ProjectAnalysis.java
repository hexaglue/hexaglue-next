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
import io.hexaglue.engine.AnalysisResult;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.Validation;
import io.hexaglue.frontend.FrontendResult;
import io.hexaglue.frontend.SpoonFrontend;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.Finding;
import java.util.ArrayList;
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
     * @return the model, everything the run left out of it, and what the gates made of it
     */
    static Result run(MavenProject project, HexaGlueConfig config) {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(config, "config must not be null");
        FrontendResult read = SpoonFrontend.analyze(ProjectSources.request(project, config.analysis()));
        AnalysisResult analysed = Analysis.analyze(EngineContext.of(read.code(), KnowledgePacks.embedded(), config));
        ArchModel model = analysed.model();
        List<Diagnostic> diagnostics = new ArrayList<>(read.diagnostics());
        diagnostics.addAll(analysed.diagnostics());
        return new Result(
                model,
                analysed.findings(),
                diagnostics,
                Validation.of(model, analysed.findings(), config.validation()));
    }

    /**
     * What one run produced.
     *
     * @param model the classified model
     * @param findings what the checks made of it
     * @param diagnostics what was left out, by the reading then by the perimeter of the verdicts
     * @param validation what the gates made of the model and of the findings
     */
    record Result(ArchModel model, List<Finding> findings, List<Diagnostic> diagnostics, Validation validation) {

        /**
         * Validates and copies the components.
         */
        Result {
            Objects.requireNonNull(model, "model must not be null");
            Objects.requireNonNull(findings, "findings must not be null");
            Objects.requireNonNull(diagnostics, "diagnostics must not be null");
            Objects.requireNonNull(validation, "validation must not be null");
            findings = List.copyOf(findings);
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
