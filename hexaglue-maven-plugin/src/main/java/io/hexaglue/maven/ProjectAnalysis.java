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
import io.hexaglue.engine.Dependencies;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.Perimeter;
import io.hexaglue.engine.Validation;
import io.hexaglue.frontend.FrontendRequest;
import io.hexaglue.frontend.FrontendResult;
import io.hexaglue.frontend.SpoonFrontend;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.Measurements;
import io.hexaglue.spi.PluginExecutor;
import io.hexaglue.spi.PluginRun;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        return run(ProjectSources.request(project, config.analysis()), config);
    }

    /**
     * Runs the chain over a whole reactor, in one reading.
     *
     * <p>One analysis rather than one per module: the modules of a reactor are the places its
     * architecture is split across, and a reading that stopped at each boundary would report on
     * each module as if the others were third parties.</p>
     *
     * @param modules the projects of the reactor, in build order
     * @param config what the build states about the analysis
     * @return the model, everything the run left out of it, and what the gates made of it
     */
    static Result runReactor(List<MavenProject> modules, HexaGlueConfig config) {
        Objects.requireNonNull(modules, "modules must not be null");
        Objects.requireNonNull(config, "config must not be null");
        return run(ProjectSources.reactorRequest(modules, config.analysis()), config);
    }

    private static Result run(FrontendRequest request, HexaGlueConfig config) {
        FrontendResult read = SpoonFrontend.analyze(request);
        AnalysisResult analysed = Analysis.analyze(EngineContext.of(read.code(), KnowledgePacks.embedded(), config));
        ArchModel model = analysed.model();
        List<Diagnostic> diagnostics = new ArrayList<>(read.diagnostics());
        diagnostics.addAll(analysed.diagnostics());
        Dependencies dependencies = Dependencies.of(read.code(), Perimeter.of(read.code(), config.analysis()));
        return new Result(
                model,
                analysed.findings(),
                new Measurements(dependencies.stabilities(), dependencies.cycles()),
                diagnostics,
                Validation.of(model, analysed.findings(), config.validation()));
    }

    /**
     * Runs the discovered backends over what one analysis concluded.
     *
     * <p>The measures the report shows are the ones the engine took; handing them over rather than
     * letting a plugin walk the references again is what keeps a report from disagreeing with the
     * gate that read the same codebase.</p>
     *
     * @param analysed what the analysis concluded
     * @param plugins the backends found on the classpath
     * @param minConfidence how sure the analysis must be before a backend generates from it
     * @param options what the document asks of each of them
     * @return what the backends produced and what the run refused
     */
    static PluginRun contribute(
            Result analysed,
            List<HexaGluePlugin> plugins,
            Confidence minConfidence,
            Map<String, Map<String, String>> options) {
        Objects.requireNonNull(analysed, "analysed must not be null");
        Objects.requireNonNull(plugins, "plugins must not be null");
        Objects.requireNonNull(minConfidence, "minConfidence must not be null");
        Objects.requireNonNull(options, "options must not be null");
        return PluginExecutor.run(
                plugins, analysed.model(), analysed.findings(), analysed.measurements(), minConfidence, options);
    }

    /**
     * What one run produced.
     *
     * @param model the classified model
     * @param findings what the checks made of it
     * @param measurements what was measured about the shape of the codebase
     * @param diagnostics what was left out, by the reading then by the perimeter of the verdicts
     * @param validation what the gates made of the model and of the findings
     */
    record Result(
            ArchModel model,
            List<Finding> findings,
            Measurements measurements,
            List<Diagnostic> diagnostics,
            Validation validation) {

        /**
         * Validates and copies the components.
         */
        Result {
            Objects.requireNonNull(model, "model must not be null");
            Objects.requireNonNull(findings, "findings must not be null");
            Objects.requireNonNull(measurements, "measurements must not be null");
            Objects.requireNonNull(diagnostics, "diagnostics must not be null");
            Objects.requireNonNull(validation, "validation must not be null");
            findings = List.copyOf(findings);
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
