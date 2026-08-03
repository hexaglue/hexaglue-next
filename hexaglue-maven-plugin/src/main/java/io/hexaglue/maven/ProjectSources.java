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

import io.hexaglue.frontend.FrontendRequest;
import io.hexaglue.model.config.AnalysisScope;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * What the host hands the frontend: where the sources are, what resolves their references, and
 * which language level to read them at.
 *
 * <p>Only the source directory the project declares is read — never a root under the build
 * directory. A build tool knows every root it compiles, generated ones included, and handing them
 * all over feeds the analysis its own output: an emitted adapter comes back implementing the port
 * its author wrote, the rules read that port as a seam rather than a boundary, and the second run
 * over unchanged sources no longer says the same thing. Recognizing generated code by its marker
 * remains the net for what a project checks into its own sources; choosing the roots is the
 * decision.</p>
 *
 * <p>The compiled classes join the classpath when they exist, so a type whose sources are absent —
 * one an annotation processor emitted, one a dependency ships — is still knowable by its bytecode.
 * Dependency artifacts are passed as resolved: an entry the build cannot produce is the frontend's
 * to refuse loudly, not this class's to drop quietly.</p>
 */
final class ProjectSources {

    /** Where expanded sources are looked for, replacing the declared root when they exist. */
    static final String EXPANDED_SOURCES = "hexaglue/delombok-sources";

    private static final String COMPILER_RELEASE = "maven.compiler.release";

    private ProjectSources() {}

    /**
     * Builds what to read from what the project declares.
     *
     * @param project the project being built
     * @param scope the perimeter of the analysis
     * @return the request to hand the frontend
     */
    static FrontendRequest request(MavenProject project, AnalysisScope scope) {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        FrontendRequest.Builder request = FrontendRequest.builder()
                .sourceRoot(sourceRoot(project))
                .scope(scope)
                .javaVersion(languageLevel(project));
        compiledClasses(project).ifPresent(request::classpathEntry);
        for (Artifact artifact : project.getArtifacts()) {
            Optional.ofNullable(artifact.getFile()).map(File::toPath).ifPresent(request::classpathEntry);
        }
        return request.build();
    }

    /**
     * Builds what to read from a whole reactor: every module's sources at once, each root under the
     * module that declared it.
     *
     * <p>One reading rather than one per module, because the references that cross modules are the
     * reason a reactor is worth reading as one: read module by module, a port declared in one and
     * implemented in another resolves to a stub of itself, and the seam the analysis exists to find
     * is exactly what it loses. A module holding no source of its own — an aggregator POM, a module
     * whose sources are not there yet — contributes nothing and is passed over.</p>
     *
     * @param modules the projects of the reactor, in build order
     * @param scope the perimeter of the analysis
     * @return the request to hand the frontend
     * @throws IllegalArgumentException when no module of the reactor holds a source root to read
     */
    static FrontendRequest reactorRequest(List<MavenProject> modules, AnalysisScope scope) {
        Objects.requireNonNull(modules, "modules must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        FrontendRequest.Builder request = FrontendRequest.builder().scope(scope);
        int level = FrontendRequest.DEFAULT_JAVA_VERSION;
        for (MavenProject module : modules) {
            Path sourceRoot = sourceRoot(module);
            if (Files.isDirectory(sourceRoot)) {
                request.sourceRoot(sourceRoot, module.getArtifactId());
            }
            // Reading below the level a module compiles at would fail on its own syntax, so the
            // whole reactor is read at the highest level any of its modules claims.
            level = Math.max(level, languageLevel(module));
            compiledClasses(module).ifPresent(request::classpathEntry);
            for (Artifact artifact : module.getArtifacts()) {
                Optional.ofNullable(artifact.getFile()).map(File::toPath).ifPresent(request::classpathEntry);
            }
        }
        return request.javaVersion(level).build();
    }

    /**
     * Returns the root to read: the expanded sources when a build step produced them, the declared
     * source directory otherwise. Expanded sources carry the members an annotation stands for,
     * which the declaration alone does not show.
     */
    private static Path sourceRoot(MavenProject project) {
        Path expanded = Path.of(project.getBuild().getDirectory(), EXPANDED_SOURCES);
        return Files.isDirectory(expanded)
                ? expanded
                : Path.of(project.getBuild().getSourceDirectory());
    }

    private static Optional<Path> compiledClasses(MavenProject project) {
        Path classes = Path.of(project.getBuild().getOutputDirectory());
        return Files.isDirectory(classes) ? Optional.of(classes) : Optional.empty();
    }

    /**
     * Returns the level the sources are parsed at: the one the project compiles at, or the
     * frontend's baseline when the project states none in a form this can read.
     */
    private static int languageLevel(MavenProject project) {
        String release = project.getProperties().getProperty(COMPILER_RELEASE);
        if (release == null) {
            return FrontendRequest.DEFAULT_JAVA_VERSION;
        }
        try {
            return Integer.parseInt(release.trim());
        } catch (NumberFormatException notANumber) {
            // A release the build states in a form this cannot read is the compiler's problem to
            // report; reading at the baseline keeps the analysis running rather than failing the
            // build twice for the same reason.
            return FrontendRequest.DEFAULT_JAVA_VERSION;
        }
    }
}
