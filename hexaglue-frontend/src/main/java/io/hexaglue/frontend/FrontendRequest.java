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

package io.hexaglue.frontend;

import io.hexaglue.model.EnumSets;
import io.hexaglue.model.code.CodeModelCapability;
import io.hexaglue.model.config.AnalysisScope;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What the frontend is asked to read: where the sources are, what the classpath is, which Java
 * level to parse at, which perimeter to analyze and which optional facts to extract.
 *
 * <p>The classpath is what makes classpath types knowable: with it, {@code extends
 * JpaRepository<Order, OrderId>} resolves to a real hierarchy instead of a bare name. Parsing
 * stays tolerant when it is incomplete — an unresolved type keeps its source-level name.</p>
 *
 * <p>A whole reactor is read in one call rather than one call per module: a reference from one
 * module to another then resolves to the type itself instead of to a stub of it, and the modules of
 * a reactor are exactly what makes such references worth reading. Which module a root belongs to is
 * something only the host knows — a directory says nothing about the build that declared it.</p>
 *
 * @param sourceRoots the directories holding the Java sources to read, in reading order
 * @param classpath the classpath entries (jars or class directories) used to resolve references
 * @param javaVersion the Java language level the sources are parsed at
 * @param scope the perimeter of the analysis
 * @param capabilities the optional extractions to run
 * @param modules the reactor module each source root belongs to, empty on a single-module project
 * @since 7.0.0
 */
public record FrontendRequest(
        List<Path> sourceRoots,
        List<Path> classpath,
        int javaVersion,
        AnalysisScope scope,
        Set<CodeModelCapability> capabilities,
        Map<Path, String> modules) {

    /** The Java level sources are parsed at unless the caller asks for another one. */
    public static final int DEFAULT_JAVA_VERSION = 17;

    /**
     * Validates the request — at least one source root, a supported Java level — and defensively
     * copies every collection.
     */
    public FrontendRequest {
        Objects.requireNonNull(sourceRoots, "sourceRoots must not be null");
        Objects.requireNonNull(classpath, "classpath must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(capabilities, "capabilities must not be null");
        Objects.requireNonNull(modules, "modules must not be null");
        if (sourceRoots.isEmpty()) {
            throw new IllegalArgumentException("at least one source root is required");
        }
        if (javaVersion < 8) {
            throw new IllegalArgumentException("javaVersion must be >= 8, got " + javaVersion);
        }
        List<Path> declaredRoots = sourceRoots;
        modules.forEach((root, module) -> {
            if (!declaredRoots.contains(root)) {
                throw new IllegalArgumentException(root + " is attributed to module " + module + " but is not read");
            }
        });
        sourceRoots = List.copyOf(sourceRoots);
        classpath = List.copyOf(classpath);
        capabilities = EnumSets.ordered(capabilities);
        modules = Collections.unmodifiableMap(new LinkedHashMap<>(modules));
    }

    /**
     * Creates a request reading one source root with no classpath, over the whole perimeter.
     *
     * @param sourceRoot the directory holding the Java sources
     * @return a new request
     */
    public static FrontendRequest of(Path sourceRoot) {
        return builder().sourceRoot(sourceRoot).build();
    }

    /**
     * Creates a builder for a frontend request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether the given capability was requested.
     *
     * @param capability the capability to test
     * @return true when the frontend must run it
     */
    public boolean has(CodeModelCapability capability) {
        return capabilities.contains(capability);
    }

    /**
     * Builder for {@link FrontendRequest} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final List<Path> sourceRoots = new ArrayList<>();
        private final List<Path> classpath = new ArrayList<>();
        private final Set<CodeModelCapability> capabilities = EnumSet.noneOf(CodeModelCapability.class);
        private final Map<Path, String> modules = new LinkedHashMap<>();
        private int javaVersion = DEFAULT_JAVA_VERSION;
        private AnalysisScope scope = AnalysisScope.everything();

        private Builder() {}

        /**
         * Adds a source root to read, belonging to no module in particular.
         *
         * @param sourceRoot the directory holding Java sources
         * @return this builder
         */
        public Builder sourceRoot(Path sourceRoot) {
            sourceRoots.add(Objects.requireNonNull(sourceRoot, "sourceRoot must not be null"));
            return this;
        }

        /**
         * Adds a source root to read, stating which module of a reactor declared it.
         *
         * <p>Only the host knows this: a directory says nothing about the module it was declared in.
         * A reading where no root names a module is the reading of a single-module project, and no
         * topology follows from it.</p>
         *
         * @param sourceRoot the directory holding Java sources
         * @param moduleName the module that declared the root
         * @return this builder
         */
        public Builder sourceRoot(Path sourceRoot, String moduleName) {
            Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");
            Objects.requireNonNull(moduleName, "moduleName must not be null");
            sourceRoots.add(sourceRoot);
            modules.put(sourceRoot, moduleName);
            return this;
        }

        /**
         * Adds a classpath entry used to resolve references.
         *
         * @param entry a jar file or a directory of compiled classes
         * @return this builder
         */
        public Builder classpathEntry(Path entry) {
            classpath.add(Objects.requireNonNull(entry, "entry must not be null"));
            return this;
        }

        /**
         * Sets the Java language level the sources are parsed at.
         *
         * @param javaVersion the language level
         * @return this builder
         */
        public Builder javaVersion(int javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        /**
         * Sets the analysis scope.
         *
         * @param scope the analysis scope
         * @return this builder
         */
        public Builder scope(AnalysisScope scope) {
            this.scope = Objects.requireNonNull(scope, "scope must not be null");
            return this;
        }

        /**
         * Requests an optional extraction.
         *
         * @param capability the capability to run
         * @return this builder
         */
        public Builder capability(CodeModelCapability capability) {
            capabilities.add(Objects.requireNonNull(capability, "capability must not be null"));
            return this;
        }

        /**
         * Builds the request.
         *
         * @return a new FrontendRequest
         */
        public FrontendRequest build() {
            return new FrontendRequest(sourceRoots, classpath, javaVersion, scope, capabilities, modules);
        }
    }
}
