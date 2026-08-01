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

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.hexaglue.testkit.GoldenFiles;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs every profile 1 scenario through the analysis engine bound on the classpath and compares
 * the snapshot to its golden file. Skips when no engine is bound: the corpus stays executable
 * from the first commit of the reactor, and turns red or green as soon as an engine registers an
 * {@link AnalysisRunner}.
 */
class Profile1CorpusTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden/profile1");

    @TempDir
    Path tempDir;

    static Stream<Arguments> scenarios() {
        List<CorpusScenario> scenarios = Corpus.profile1();
        return scenarios.stream().map(scenario -> Arguments.of(scenario.id(), scenario));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void scenarioMatchesGolden(String id, CorpusScenario scenario) {
        AnalysisRunner runner = AnalysisRunner.discover().orElse(null);
        assumeTrue(runner != null, "No analysis engine bound to the testkit corpus yet");

        Path root = scenario.materialize(tempDir);
        String snapshot = runner.analyze(root, scenario.basePackage());

        GoldenFiles.assertMatches(GOLDEN_DIR, id + ".txt", snapshot);
    }
}
