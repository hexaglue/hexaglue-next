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

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Attachment point between the corpus and an analysis engine.
 *
 * <p>The testkit ships the corpus and the harness but no engine. An engine module provides an
 * implementation through {@link ServiceLoader} registration; corpus tests discover it at runtime
 * and skip when none is present. The returned snapshot is the engine's canonical, deterministic
 * text rendering of its analysis result, suitable for golden-file comparison.
 *
 * @since 7.0.0
 */
@FunctionalInterface
public interface AnalysisRunner {

    /**
     * Analyzes the sources under {@code sourceRoot} scoped to {@code basePackage} and returns a
     * canonical snapshot of the resulting architectural model.
     *
     * @param sourceRoot the root directory of the Java sources to analyze
     * @param basePackage the base package delimiting the analysis scope
     * @return a deterministic text snapshot of the analysis result
     */
    String analyze(Path sourceRoot, String basePackage);

    /**
     * Discovers the analysis runner registered on the classpath, if any.
     *
     * @return the first registered runner, or empty when no engine is bound
     */
    static Optional<AnalysisRunner> discover() {
        return ServiceLoader.load(AnalysisRunner.class).findFirst();
    }
}
