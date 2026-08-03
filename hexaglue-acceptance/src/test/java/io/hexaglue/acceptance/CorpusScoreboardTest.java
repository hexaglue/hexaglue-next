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
import io.hexaglue.testkit.corpus.CorpusProfile;
import io.hexaglue.testkit.corpus.CorpusScenario;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * How much of the reference corpus the engine gets right, held to a committed floor, one profile at
 * a time.
 *
 * <p>The corpus is the acceptance criterion of the rewrite, and it cannot be a pass-or-fail gate
 * while the engine is half built: some scenarios need sensors that do not exist yet. Nor can it be
 * a golden-file harness, which would record today's wrong verdicts as tomorrow's reference. So it
 * is a scoreboard: every reviewed scenario is run, the score is compared to a number committed
 * next to this test, and the build fails both when the score drops — a regression — and when it
 * rises without the floor being raised, so progress is recorded rather than drifting.</p>
 *
 * <p>The score is kept per profile rather than as one total, because the three profiles measure
 * three different things: a total would let a gain on sources written in our own vocabulary pay
 * for a loss on sources that give the engine no vocabulary at all.</p>
 *
 * <p>A scenario counts only once a human has reviewed its expectations. The rest are drafts
 * harvested from a legacy engine that carries confirmed bugs, and importing them wholesale would
 * measure conformance to those bugs.</p>
 */
class CorpusScoreboardTest {

    private static final String FLOOR = "/corpus-floor.properties";

    @TempDir
    Path workspace;

    @ParameterizedTest(name = "{0} holds every reviewed scenario it held before, and no fewer")
    @EnumSource(CorpusProfile.class)
    void theEngineHoldsItsGround(CorpusProfile profile) {
        Properties floor = floor();
        List<CorpusScenario> reviewed = Corpus.of(profile).stream()
                .filter(scenario -> CorpusExpectations.of(scenario).isScorable())
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
        String reviewedKey = profile.directory() + ".reviewed";
        String passingKey = profile.directory() + ".passing";

        assertThat(reviewed.size())
                .as("Reviewed scenarios: set %s in corpus-floor.properties to %d", reviewedKey, reviewed.size())
                .isEqualTo(number(floor, reviewedKey));
        assertThat(passing)
                .as(
                        "%d of %d reviewed %s scenarios pass. Still failing:%n  %s%n"
                                + "Below the floor means a regression; above it means %s owes an update to %d.",
                        passing,
                        reviewed.size(),
                        profile.directory(),
                        String.join(System.lineSeparator() + "  ", failing),
                        passingKey,
                        passing)
                .isEqualTo(number(floor, passingKey));
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
