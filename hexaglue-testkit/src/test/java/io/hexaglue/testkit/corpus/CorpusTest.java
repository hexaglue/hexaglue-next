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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Every profile answers the same structural questions, which is what makes a new one cheap to add:
 * declaring it in {@link CorpusProfile} enrols it here, and an index naming a scenario the
 * resources do not carry fails loudly rather than shrinking the corpus in silence.
 */
class CorpusTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @EnumSource(CorpusProfile.class)
    @DisplayName("loads with at least one scenario")
    void loadsWithAtLeastOneScenario(CorpusProfile profile) {
        assertThat(Corpus.of(profile)).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(CorpusProfile.class)
    @DisplayName("names each of its scenarios once")
    void namesEachScenarioOnce(CorpusProfile profile) {
        assertThat(Corpus.of(profile)).extracting(CorpusScenario::id).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @EnumSource(CorpusProfile.class)
    @DisplayName("gives every scenario its profile, a base package and at least one source")
    void givesEveryScenarioWhatItNeeds(CorpusProfile profile) {
        for (CorpusScenario scenario : Corpus.of(profile)) {
            assertThat(scenario.profile()).isEqualTo(profile);
            assertThat(scenario.basePackage())
                    .as("basePackage of %s", scenario.id())
                    .isNotBlank();
            assertThat(scenario.sources()).as("sources of %s", scenario.id()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("a scenario materializes its sources on disk")
    void scenarioMaterializes() throws Exception {
        CorpusScenario scenario = Corpus.of(CorpusProfile.PROFILE_1).get(0);

        Path root = scenario.materialize(tempDir);

        for (CorpusScenario.SourceFile source : scenario.sources()) {
            Path file = root.resolve(source.relativePath());
            assertThat(Files.readString(file)).isEqualTo(source.content());
        }
    }

    @Test
    @DisplayName("no two profiles name the same scenario, so one golden file names one scenario")
    void noTwoProfilesNameTheSameScenario() {
        List<String> ids = Arrays.stream(CorpusProfile.values())
                .flatMap(profile -> Corpus.of(profile).stream())
                .map(CorpusScenario::id)
                .toList();

        assertThat(ids).doesNotHaveDuplicates();
    }
}
