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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.frontend.FrontendRequest;
import io.hexaglue.model.config.AnalysisScope;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the host hands the frontend is a decision, not a formality: a build tool knows every root
 * it compiles, generated ones included, and handing them all over feeds the analysis its own
 * output — after which the second run over unchanged sources no longer reads the same.
 */
class ProjectSourcesTest {

    @TempDir
    Path projectDir;

    private Path directory(String relativePath) {
        Path created = projectDir.resolve(relativePath);
        try {
            Files.createDirectories(created);
        } catch (IOException unwritable) {
            throw new UncheckedIOException("Failed to create " + created, unwritable);
        }
        return created;
    }

    private MavenProject project() {
        MavenProject project = new MavenProject();
        Build build = new Build();
        build.setSourceDirectory(projectDir.resolve("src/main/java").toString());
        build.setDirectory(projectDir.resolve("target").toString());
        build.setOutputDirectory(projectDir.resolve("target/classes").toString());
        project.getModel().setBuild(build);
        return project;
    }

    private FrontendRequest requestOf(MavenProject project) {
        return ProjectSources.request(project, AnalysisScope.everything());
    }

    @Test
    @DisplayName("reads the source directory the project declares")
    void readsTheDeclaredSourceDirectory() {
        Path sources = directory("src/main/java");

        assertThat(requestOf(project()).sourceRoots()).containsExactly(sources);
    }

    @Test
    @DisplayName("never reads a root under the build directory, whatever the build added there")
    void neverReadsARootUnderTheBuildDirectory() {
        Path sources = directory("src/main/java");
        Path generated = directory("target/generated-sources/annotations");
        MavenProject project = project();
        project.addCompileSourceRoot(generated.toString());

        assertThat(requestOf(project).sourceRoots()).containsExactly(sources);
    }

    @Test
    @DisplayName("reads the expanded sources instead when the build produced them")
    void readsTheExpandedSourcesWhenTheyExist() {
        directory("src/main/java");
        Path expanded = directory("target/hexaglue/delombok-sources");

        assertThat(requestOf(project()).sourceRoots()).containsExactly(expanded);
    }

    @Test
    @DisplayName("puts the compiled classes on the classpath, so a type without sources stays knowable")
    void putsCompiledClassesOnTheClasspath() {
        directory("src/main/java");
        Path classes = directory("target/classes");

        assertThat(requestOf(project()).classpath()).containsExactly(classes);
    }

    @Test
    @DisplayName("leaves the compiled classes out before anything was compiled")
    void leavesCompiledClassesOutBeforeTheyExist() {
        directory("src/main/java");

        assertThat(requestOf(project()).classpath()).isEmpty();
    }

    @Test
    @DisplayName("parses at the language level the project compiles at")
    void parsesAtTheProjectLanguageLevel() {
        directory("src/main/java");
        MavenProject project = project();
        project.getProperties().setProperty("maven.compiler.release", "21");

        assertThat(requestOf(project).javaVersion()).isEqualTo(21);
    }

    @Test
    @DisplayName("falls back to the baseline when the project states no language level")
    void fallsBackToTheBaselineLanguageLevel() {
        directory("src/main/java");

        assertThat(requestOf(project()).javaVersion()).isEqualTo(FrontendRequest.DEFAULT_JAVA_VERSION);
    }

    @Nested
    @DisplayName("a whole reactor")
    class AWholeReactor {

        private MavenProject module(String artifactId, String packaging) {
            MavenProject module = new MavenProject();
            module.setArtifactId(artifactId);
            module.setPackaging(packaging);
            Build build = new Build();
            build.setSourceDirectory(
                    projectDir.resolve(artifactId).resolve("src/main/java").toString());
            build.setDirectory(projectDir.resolve(artifactId).resolve("target").toString());
            build.setOutputDirectory(
                    projectDir.resolve(artifactId).resolve("target/classes").toString());
            module.getModel().setBuild(build);
            return module;
        }

        private FrontendRequest requestOf(MavenProject... modules) {
            return ProjectSources.reactorRequest(List.of(modules), AnalysisScope.everything());
        }

        @Test
        @DisplayName("is read in one pass, each root under the module that declared it")
        void isReadInOnePass() {
            Path domain = directory("shop-domain/src/main/java");
            Path infra = directory("shop-infra/src/main/java");

            FrontendRequest request = requestOf(module("shop-domain", "jar"), module("shop-infra", "jar"));

            assertThat(request.sourceRoots()).containsExactly(domain, infra);
            assertThat(request.modules()).containsEntry(domain, "shop-domain").containsEntry(infra, "shop-infra");
        }

        @Test
        @DisplayName("leaves out a module holding no sources of its own")
        void leavesOutAModuleWithoutSources() {
            Path domain = directory("shop-domain/src/main/java");

            FrontendRequest request = requestOf(module("shop-parent", "pom"), module("shop-domain", "jar"));

            assertThat(request.sourceRoots()).containsExactly(domain);
            assertThat(request.modules()).containsOnlyKeys(domain);
        }

        @Test
        @DisplayName("parses at the highest level any module compiles at, so no module is read below its own")
        void parsesAtTheHighestLevelOfTheReactor() {
            directory("shop-domain/src/main/java");
            directory("shop-infra/src/main/java");
            MavenProject domain = module("shop-domain", "jar");
            MavenProject infra = module("shop-infra", "jar");
            infra.getProperties().setProperty("maven.compiler.release", "21");

            assertThat(requestOf(domain, infra).javaVersion()).isEqualTo(21);
        }

        @Test
        @DisplayName("puts every module's compiled classes on the classpath")
        void putsEveryModulesClassesOnTheClasspath() {
            directory("shop-domain/src/main/java");
            directory("shop-infra/src/main/java");
            Path domainClasses = directory("shop-domain/target/classes");
            Path infraClasses = directory("shop-infra/target/classes");

            FrontendRequest request = requestOf(module("shop-domain", "jar"), module("shop-infra", "jar"));

            assertThat(request.classpath()).contains(domainClasses, infraClasses);
        }

        @Test
        @DisplayName("refuses a reactor where nothing holds a source to read")
        void refusesAReactorWithoutSources() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> requestOf(module("shop-parent", "pom")))
                    .withMessageContaining("source root");
        }
    }
}
