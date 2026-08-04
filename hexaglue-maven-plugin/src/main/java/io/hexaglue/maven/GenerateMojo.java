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
import io.hexaglue.spi.SourceFile;
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
 * Writes the types the backends generate from the classified model, and hands them to the compiler.
 *
 * <p>The goal <strong>writes and does not judge</strong>. What a build makes of its architecture is
 * the business of {@code validate}, which can stop it, and of {@code report}, which explains it; a
 * generation that also failed builds would make the same run mean two things and give a project no
 * way to generate while it is still cleaning up. What a backend declined to write is said here —
 * loudly enough to notice, never as an exit condition.</p>
 *
 * @since 7.0.0
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class GenerateMojo extends AbstractMojo {

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
     * Where the generated types are written, and what is added to the compiler's source roots.
     */
    @Parameter(
            property = "hexaglue.generatedSourcesDirectory",
            defaultValue = "${project.build.directory}/generated-sources/hexaglue")
    private String generatedSourcesDirectory;

    /**
     * Creates the goal. The build instantiates it and fills its parameters before running it.
     */
    // Written out rather than left implicit: the documentation gate requires every public
    // constructor to say what it is for, including the one the compiler would generate.
    @SuppressWarnings({"PMD.UnnecessaryConstructor", "PMD.CallSuperInConstructor"})
    public GenerateMojo() {
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
            log.info("HexaGlue found no backend on the classpath; nothing to generate");
            return;
        }
        ProjectAnalysis.Result analysed = ProjectAnalysis.run(project, config, PluginDiscovery.declaredBy(plugins));
        Diagnostics.report(analysed.diagnostics(), log);
        PluginRun run = ProjectAnalysis.contribute(
                analysed, plugins, config.generation().minConfidence(), options);

        Diagnostics.report(run.diagnostics(), log);
        if (!run.skipped().isEmpty()) {
            log.warn("HexaGlue skipped " + String.join(", ", run.skipped()));
        }
        write(run, log);
    }

    /**
     * Writes what belongs to this module and tells the compiler where it is.
     *
     * <p>The source root is added even when nothing was written: a build that generated nothing
     * this time still has whatever an earlier run left there, and dropping the root would take a
     * project's own sources out from under it.</p>
     */
    private void write(PluginRun run, Log log) throws MojoExecutionException {
        String module = project.getArtifactId();
        List<SourceFile> mine = Sources.addressedTo(run.sources(), module);
        List<SourceFile> elsewhere = Sources.addressedElsewhere(run.sources(), module);
        Path root = Path.of(generatedSourcesDirectory);
        Sources.write(mine, root);
        project.addCompileSourceRoot(root.toString());
        log.info("HexaGlue wrote " + mine.size() + " type(s) to " + root);
        if (!elsewhere.isEmpty()) {
            log.info("HexaGlue left " + elsewhere.size() + " type(s) to the module they are addressed to: "
                    + named(elsewhere));
        }
    }

    private static String named(List<SourceFile> sources) {
        return sources.stream()
                .map(source -> source.qualifiedName() + " (" + source.module().orElseThrow() + ")")
                .reduce("", (all, one) -> all.isEmpty() ? one : all + ", " + one);
    }
}
