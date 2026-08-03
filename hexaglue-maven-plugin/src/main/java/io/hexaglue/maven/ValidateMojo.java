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

import io.hexaglue.engine.Explanation;
import io.hexaglue.engine.Outcome;
import io.hexaglue.engine.Validation;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.config.ValidationConfig;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import java.util.List;
import java.util.Optional;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Analyzes the project's sources and holds the result to the configured gates.
 *
 * <p>The goal produces a verdict and no content: it says what the analysis concluded, what it left
 * out, and — when a gate is armed and a type fails it — why the build stops, with the remediation
 * the engine wrote for that very type.</p>
 *
 * @since 7.0.0
 */
@Mojo(
        name = "validate",
        defaultPhase = LifecyclePhase.VALIDATE,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class ValidateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Whether to skip HexaGlue entirely.
     */
    @Parameter(property = "hexaglue.skip", defaultValue = "false")
    private boolean skip;

    /**
     * The package the analysis is scoped to. Overrides {@code analysis.basePackage} of the
     * configuration document when both are stated.
     */
    @Parameter(property = "hexaglue.basePackage")
    private String basePackage;

    /**
     * Whether a type the analysis could not classify fails the build. Overrides
     * {@code validation.failOnUnclassified} of the configuration document when both are stated.
     */
    @Parameter(property = "hexaglue.failOnUnclassified")
    private Boolean failOnUnclassified;

    /**
     * Creates the goal. The build instantiates it and fills its parameters before running it.
     */
    // Written out rather than left implicit: the documentation gate requires every public
    // constructor to say what it is for, including the one the compiler would generate.
    @SuppressWarnings({"PMD.UnnecessaryConstructor", "PMD.CallSuperInConstructor"})
    public ValidateMojo() {
        // Nothing to set up: the build fills the parameter fields after instantiating the goal.
    }

    @Override
    public void execute() throws MojoFailureException {
        Log log = getLog();
        if (skip) {
            log.info("HexaGlue is skipped");
            return;
        }
        HexaGlueConfig stated = ConfigLoader.read(project.getBasedir().toPath());
        ProjectAnalysis.Result result =
                ProjectAnalysis.run(project, configuration(stated, basePackage, failOnUnclassified));
        report(result.diagnostics());
        Explanation.of(Outcome.of(result.model())).forEach(log::info);

        Validation validation = result.validation();
        if (validation.passed()) {
            Explanation.of(validation).forEach(log::info);
            return;
        }
        Explanation.of(validation).forEach(log::error);
        throw new MojoFailureException(
                "HexaGlue refused " + validation.refusals().size()
                        + " classification(s); the reasons and their remediation are logged above");
    }

    /**
     * Applies what the build states on top of what the document states.
     *
     * <p>A goal parameter wins over the document, and only when it is set: a build stating
     * {@code -Dhexaglue.failOnUnclassified=true} means it, whereas a parameter left alone must not
     * silently undo a gate the document armed. That is what distinguishes an unset parameter from
     * one set to its default value here.</p>
     *
     * @param stated what the configuration document states
     * @param basePackage the package the analysis is scoped to, null or blank when unset
     * @param failOnUnclassified whether an undecided type fails the build, null when unset
     * @return the configuration of this run
     */
    static HexaGlueConfig configuration(HexaGlueConfig stated, String basePackage, Boolean failOnUnclassified) {
        AnalysisScope scope = scopedTo(stated, basePackage).analysis();
        ValidationConfig gates = Optional.ofNullable(failOnUnclassified)
                .map(fail -> ValidationConfig.builder()
                        .failOnUnclassified(fail)
                        .minConfidence(stated.validation().minConfidence())
                        .failOnAmbiguous(stated.validation().failOnAmbiguous())
                        .allowInferred(stated.validation().allowInferred())
                        .findingThresholds(stated.validation().findingThresholds())
                        .build())
                .orElseGet(stated::validation);
        return new HexaGlueConfig(scope, stated.classification(), gates, stated.generation(), stated.modules());
    }

    /**
     * Applies the package a build scopes the analysis to, when it states one.
     *
     * @param stated what the configuration document states
     * @param basePackage the package the analysis is scoped to, null or blank when unset
     * @return the configuration of this run
     */
    static HexaGlueConfig scopedTo(HexaGlueConfig stated, String basePackage) {
        AnalysisScope scope = Optional.ofNullable(basePackage)
                .filter(pkg -> !pkg.isBlank())
                .map(pkg -> new AnalysisScope(
                        Optional.of(pkg),
                        stated.analysis().includePackages(),
                        stated.analysis().excludePackages()))
                .orElseGet(stated::analysis);
        return new HexaGlueConfig(
                scope, stated.classification(), stated.validation(), stated.generation(), stated.modules());
    }

    /**
     * Says what the reading left out.
     *
     * <p>A degraded reading is said once and plainly, because it is rare and it explains wrong
     * verdicts. What was deliberately left out is counted rather than listed: a perimeter doing its
     * job leaves out most of a code base, and a build log burying its own conclusions under
     * thousands of expected exclusions says less than one that offers them on request.</p>
     */
    private void report(List<Diagnostic> diagnostics) {
        Log log = getLog();
        long expected = diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.INFO)
                .count();
        diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() != DiagnosticSeverity.INFO)
                .forEach(diagnostic -> log.warn(diagnostic.code() + ": " + diagnostic.message()));
        if (expected > 0) {
            log.info(expected + " type(s) were not analysed; run with -X to see which and why");
        }
        diagnostics.forEach(diagnostic -> log.debug(diagnostic.code() + ": " + diagnostic.message()));
    }
}
