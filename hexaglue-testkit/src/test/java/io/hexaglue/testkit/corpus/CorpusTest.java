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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CorpusTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("profile 1 loads with at least one scenario")
    void profile1Loads() {
        List<CorpusScenario> scenarios = Corpus.profile1();

        assertThat(scenarios).isNotEmpty();
    }

    @Test
    @DisplayName("profile 1 scenario ids are unique")
    void profile1IdsAreUnique() {
        List<CorpusScenario> scenarios = Corpus.profile1();

        assertThat(scenarios).extracting(CorpusScenario::id).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("every profile 1 scenario has a base package and at least one source")
    void profile1ScenariosAreComplete() {
        for (CorpusScenario scenario : Corpus.profile1()) {
            assertThat(scenario.basePackage())
                    .as("basePackage of %s", scenario.id())
                    .isNotBlank();
            assertThat(scenario.sources()).as("sources of %s", scenario.id()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("a profile 1 scenario materializes its sources on disk")
    void scenarioMaterializes() throws Exception {
        CorpusScenario scenario = Corpus.profile1().get(0);

        Path root = scenario.materialize(tempDir);

        for (CorpusScenario.SourceFile source : scenario.sources()) {
            Path file = root.resolve(source.relativePath());
            assertThat(Files.readString(file)).isEqualTo(source.content());
        }
    }
}
