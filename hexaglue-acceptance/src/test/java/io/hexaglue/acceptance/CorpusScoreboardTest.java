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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.testkit.corpus.Corpus;
import io.hexaglue.testkit.corpus.CorpusExpectations;
import io.hexaglue.testkit.corpus.CorpusScenario;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * How much of the reference corpus the engine gets right, held to a committed floor.
 *
 * <p>The corpus is the acceptance criterion of the rewrite, and it cannot be a pass-or-fail gate
 * while the engine is half built: most scenarios need sensors that do not exist yet. Nor can it be
 * a golden-file harness, which would record today's wrong verdicts as tomorrow's reference. So it
 * is a scoreboard: every reviewed scenario is run, the score is compared to a number committed
 * next to this test, and the build fails both when the score drops — a regression — and when it
 * rises without the floor being raised, so progress is recorded rather than drifting.</p>
 *
 * <p>A scenario counts only once a human has reviewed its expectations. The rest are drafts
 * harvested from a legacy engine that carries confirmed bugs, and importing them wholesale would
 * measure conformance to those bugs.</p>
 */
class CorpusScoreboardTest {

    private static final String FLOOR = "/corpus-floor.properties";

    @TempDir
    Path workspace;

    @Test
    @DisplayName("the engine holds every reviewed scenario it held before, and no fewer")
    void theEngineHoldsItsGround() {
        Properties floor = floor();
        List<CorpusScenario> reviewed = Corpus.profile1().stream()
                .filter(scenario -> CorpusExpectations.profile1(scenario.id()).isScorable())
                .toList();

        List<String> failing = new ArrayList<>();
        for (CorpusScenario scenario : reviewed) {
            List<String> unmet =
                    CorpusRun.of(scenario, workspace.resolve(scenario.id())).unmetClaims();
            if (!unmet.isEmpty()) {
                failing.add(scenario.id() + ": " + String.join(", ", unmet));
            }
        }
        int passing = reviewed.size() - failing.size();

        assertThat(reviewed)
                .as("Reviewed scenarios: raise profile1.reviewed in corpus-floor.properties to %d", reviewed.size())
                .hasSize(number(floor, "profile1.reviewed"));
        assertThat(passing)
                .as(
                        "%d of %d reviewed scenarios pass. Still failing:%n  %s%n"
                                + "Below the floor means a regression; above it means the floor owes an update to %d.",
                        passing, reviewed.size(), String.join("%n  ", failing), passing)
                .isEqualTo(number(floor, "profile1.passing"));
    }

    private static int number(Properties floor, String key) {
        String value = floor.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing key " + key + " in " + FLOOR);
        }
        return Integer.parseInt(value.strip());
    }

    private static Properties floor() {
        Properties properties = new Properties();
        try (InputStream stream = CorpusScoreboardTest.class.getResourceAsStream(FLOOR)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + FLOOR + " on the test classpath");
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + FLOOR, e);
        }
        return properties;
    }
}
