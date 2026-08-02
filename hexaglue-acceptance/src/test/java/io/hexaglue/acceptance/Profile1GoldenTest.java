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

package io.hexaglue.acceptance;

import io.hexaglue.testkit.GoldenFiles;
import io.hexaglue.testkit.corpus.AnalysisRunner;
import io.hexaglue.testkit.corpus.Corpus;
import io.hexaglue.testkit.corpus.CorpusScenario;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every profile 1 scenario, snapshot for snapshot.
 *
 * <p>The scoreboard next door scores the claims a person reviewed; this pins everything else the
 * model says — which record each type landed in, what it was filled with, why the ones left over
 * were left over. The claims say whether the engine is right; the snapshots say when it changed.
 * Neither replaces the other, and a change to either is meant to be read in a diff.</p>
 *
 * <p>The engine is not discovered here so much as required: the testkit ships the corpus without an
 * analysis, and this module is where one is bound to it. A missing runner is a wiring failure, not
 * a reason to skip.</p>
 */
class Profile1GoldenTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/golden/profile1");

    @TempDir
    Path workspace;

    static Stream<Arguments> scenarios() {
        return Corpus.profile1().stream().map(scenario -> Arguments.of(scenario.id(), scenario));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void scenarioMatchesGolden(String id, CorpusScenario scenario) {
        AnalysisRunner runner = AnalysisRunner.discover()
                .orElseThrow(() ->
                        new AssertionError("No analysis runner is registered: the corpus has nothing to run against"));

        String snapshot = runner.analyze(scenario.materialize(workspace.resolve(id)), scenario.basePackage());

        GoldenFiles.assertMatchesExisting(GOLDEN_DIR, id + ".json", snapshot);
    }
}
