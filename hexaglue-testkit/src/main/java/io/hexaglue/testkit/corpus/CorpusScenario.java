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

package io.hexaglue.testkit.corpus;

import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * One acceptance-corpus scenario: a self-contained set of Java sources with the base package to
 * analyze them under.
 *
 * <p>Construction rejects blank components and an empty source list with
 * {@link IllegalArgumentException}, and defensively copies {@code sources}.
 *
 * @param profile the population this scenario belongs to
 * @param id unique scenario identifier, e.g. {@code ClassificationGoldenFilesTest-minimalExample}
 * @param basePackage the base package the analysis is scoped to
 * @param origin provenance of the fixture in the harvested code base, e.g. {@code
 *     ClassificationGoldenFilesTest#createMinimalExample}
 * @param sources the fixture source files, in stable order
 * @since 7.0.0
 */
public record CorpusScenario(
        CorpusProfile profile, String id, String basePackage, String origin, List<SourceFile> sources) {

    /**
     * Validates the components and defensively copies the source list.
     */
    public CorpusScenario {
        Objects.requireNonNull(profile, "profile");
        requireNonBlank(id, "id");
        requireNonBlank(basePackage, "basePackage");
        requireNonBlank(origin, "origin");
        Objects.requireNonNull(sources, "sources");
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("Scenario " + id + " has no sources");
        }
        sources = List.copyOf(sources);
    }

    /**
     * Writes every source of this scenario under {@code root} and returns {@code root}.
     *
     * @param root the directory receiving the fixture tree, typically a JUnit {@code @TempDir}
     * @return {@code root}, for chaining into the analysis call
     */
    public Path materialize(Path root) {
        Objects.requireNonNull(root, "root");
        for (SourceFile source : sources) {
            SourceFixtures.write(root, source.relativePath(), source.content());
        }
        return root;
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    /**
     * A single fixture source file.
     *
     * <p>Construction rejects a blank path or content with {@link IllegalArgumentException}.
     *
     * @param relativePath path relative to the scenario source root, e.g. {@code com/acme/Order.java}
     * @param content the Java source text
     * @since 7.0.0
     */
    public record SourceFile(String relativePath, String content) {

        /**
         * Validates that the path and the content are non-blank.
         */
        public SourceFile {
            if (relativePath == null || relativePath.isBlank()) {
                throw new IllegalArgumentException("relativePath must not be blank");
            }
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("content of " + relativePath + " must not be blank");
            }
        }
    }
}
