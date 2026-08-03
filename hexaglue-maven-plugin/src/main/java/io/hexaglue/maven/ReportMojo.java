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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Runs the backends a project installed, and writes what they produced.
 *
 * <p>This goal writes and does not judge. Whether the architecture is acceptable was decided by
 * {@code validate}, from the same findings these reports display — the two cannot disagree because
 * there is one analysis and one judgement behind both.</p>
 *
 * <p>A plugin never touches the disk itself: it hands over a relative path and a body, and the
 * writing happens here, under one directory. That is what makes the confinement a property of the
 * shape rather than of every backend's good behaviour.</p>
 *
 * @since 7.0.0
 */
@Mojo(
        name = "report",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class ReportMojo extends AbstractMojo {

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
     * Where the documents the backends produce are written, under the build directory.
     */
    @Parameter(property = "hexaglue.reportDirectory", defaultValue = "${project.build.directory}/hexaglue")
    private String reportDirectory;

    /**
     * Creates the goal. The build instantiates it and fills its parameters before running it.
     */
    // Written out rather than left implicit: the documentation gate requires every public
    // constructor to say what it is for, including the one the compiler would generate.
    @SuppressWarnings({"PMD.UnnecessaryConstructor", "PMD.CallSuperInConstructor"})
    public ReportMojo() {
        // Nothing to set up: the build fills the parameter fields after instantiating the goal.
    }

    @Override
    public void execute() throws MojoExecutionException {
        Log log = getLog();
        if (skip) {
            log.info("HexaGlue is skipped");
            return;
        }
        List<Path> configuration = ConfigLoader.searchPath(project);
        HexaGlueConfig config = ValidateMojo.scopedTo(ConfigLoader.read(configuration), basePackage);
        Map<String, Map<String, String>> options = ConfigLoader.readPluginOptions(configuration);

        // The build sets the context loader to the realm holding this plugin and everything a
        // project declared alongside it, which is exactly where the backends are.
        List<HexaGluePlugin> plugins = PluginDiscovery.on(Thread.currentThread().getContextClassLoader());
        if (plugins.isEmpty()) {
            log.info("HexaGlue found no backend on the classpath; nothing to report");
            return;
        }
        ProjectAnalysis.Result analysed = ProjectAnalysis.run(project, config);
        PluginRun run = ProjectAnalysis.contribute(analysed, plugins, options);

        Documents.report(run, log);
        Documents.write(run.documents(), Path.of(reportDirectory));
        log.info("HexaGlue wrote " + run.documents().size() + " document(s) to " + reportDirectory);
    }
}
