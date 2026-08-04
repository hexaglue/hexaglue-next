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

import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.PluginDiscovery;
import io.hexaglue.spi.PluginRun;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Runs the backends over a whole reactor at once, and writes one set of documents for it.
 *
 * <p>An architecture split across modules is still one architecture. Reported module by module, a
 * port declared in one and implemented in another reads as a port nothing implements on one side
 * and an adapter answering a third party on the other — two false statements where there is one
 * true one. This goal reads every module in a single pass, so the references that cross a module
 * boundary are the ones the report is actually about.</p>
 *
 * <p>Like {@code report}, it writes and does not judge. Whether the architecture is acceptable was
 * decided by {@code validate}, from the same findings.</p>
 *
 * @since 7.0.0
 */
@Mojo(
        name = "reactor-report",
        defaultPhase = LifecyclePhase.VERIFY,
        aggregator = true,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class ReactorReportMojo extends AbstractMojo {

    /** Where the documents go when the build states nothing, under the root of the reactor. */
    private static final String DEFAULT_OUTPUT = "hexaglue";

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

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
     * Where the documents the backends produce are written. Left unstated, they go under the build
     * directory of the reactor's root.
     *
     * <p>Deliberately not defaulted to {@code ${project.build.directory}}: a goal that aggregates
     * runs once, at the end of the build, and the project it runs <em>on</em> is whichever module
     * came last. Writing the report of a whole reactor into that module's target directory would
     * put it somewhere nobody would look, and somewhere that changes when the build order does.</p>
     */
    @Parameter(property = "hexaglue.reportDirectory")
    private String reportDirectory;

    /**
     * Creates the goal. The build instantiates it and fills its parameters before running it.
     */
    // Written out rather than left implicit: the documentation gate requires every public
    // constructor to say what it is for, including the one the compiler would generate.
    @SuppressWarnings({"PMD.UnnecessaryConstructor", "PMD.CallSuperInConstructor"})
    public ReactorReportMojo() {
        // Nothing to set up: the build fills the parameter fields after instantiating the goal.
    }

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        if (skip) {
            log.info("HexaGlue is skipped");
            return;
        }
        MavenProject reactor = session.getTopLevelProject();
        // A lifecycle binding declared on the root is inherited by every module, so without this
        // the whole reactor would be read once per module — the same answer, N times, and N log
        // lines each claiming to be about the reactor.
        if (!reactor.equals(session.getCurrentProject())) {
            log.debug("HexaGlue reports on the reactor from its root, not from "
                    + session.getCurrentProject().getArtifactId());
            return;
        }
        List<Path> configuration = ConfigLoader.searchPath(reactor);
        HexaGlueConfig config = ValidateMojo.scopedTo(ConfigLoader.read(configuration), basePackage);
        Map<String, Map<String, String>> options = ConfigLoader.readPluginOptions(configuration);

        // The build sets the context loader to the realm holding this plugin and everything a
        // project declared alongside it, which is exactly where the backends are.
        List<HexaGluePlugin> plugins = PluginDiscovery.on(Thread.currentThread().getContextClassLoader());
        if (plugins.isEmpty()) {
            log.info("HexaGlue found no backend on the classpath; nothing to report");
            return;
        }
        log.info("HexaGlue is reading the whole reactor as one analysis");
        ProjectAnalysis.Result analysed = ProjectAnalysis.runReactor(session.getProjects(), config);
        Diagnostics.report(analysed.diagnostics(), log);
        log.info("HexaGlue laid out " + analysed.model().moduleTopology().size()
                + " module(s) of it, being the ones whose role the project declares");
        PluginRun run = ProjectAnalysis.contribute(
                analysed, plugins, config.generation().minConfidence(), options);

        Path output = output(reactor);
        Documents.report(run, log);
        Documents.write(run.documents(), output);
        log.info("HexaGlue wrote " + run.documents().size() + " document(s) to " + output);
    }

    /**
     * Returns where to write: what the build states, or the reactor root's build directory.
     */
    private Path output(MavenProject reactor) {
        if (reportDirectory == null || reportDirectory.isBlank()) {
            return Path.of(reactor.getBuild().getDirectory(), DEFAULT_OUTPUT);
        }
        return Path.of(reportDirectory);
    }
}
