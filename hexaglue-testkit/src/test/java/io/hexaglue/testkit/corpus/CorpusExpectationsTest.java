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

import io.hexaglue.testkit.corpus.CorpusExpectations.Claim;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CorpusExpectationsTest {

    @Nested
    @DisplayName("reads what a scenario is held to mean")
    class ReadsWhatAScenarioIsHeldToMean {

        @ParameterizedTest
        @EnumSource(CorpusProfile.class)
        @DisplayName("for every scenario of every profile, drafted or reviewed")
        void forEveryScenarioOfEveryProfile(CorpusProfile profile) {
            assertThat(Corpus.of(profile))
                    .allSatisfy(scenario -> assertThat(
                                    CorpusExpectations.of(scenario).scenarioId())
                            .isEqualTo(scenario.id()));
        }

        @Test
        @DisplayName("answering empty and unreviewed for a scenario that has no file")
        void emptyForAScenarioWithoutAFile() {
            CorpusExpectations expectations = CorpusExpectations.of(CorpusProfile.PROFILE_1, "no-such-scenario");

            assertThat(expectations.reviewed()).isFalse();
            assertThat(expectations.claims()).isEmpty();
            assertThat(expectations.isScorable()).isFalse();
        }
    }

    @Nested
    @DisplayName("scores nothing")
    class ScoresNothing {

        @ParameterizedTest
        @EnumSource(CorpusProfile.class)
        @DisplayName("while the file is still a draft, however many claims it carries")
        void whileTheFileIsStillADraft(CorpusProfile profile) {
            // Every scenario starts as a draft: the legacy engine carries bugs, so an imported
            // expectation means nothing until someone has read it.
            assertThat(Corpus.of(profile))
                    .filteredOn(scenario -> !CorpusExpectations.of(scenario).reviewed())
                    .allSatisfy(scenario -> assertThat(
                                    CorpusExpectations.of(scenario).isScorable())
                            .isFalse());
        }
    }

    @Nested
    @DisplayName("a claim")
    class AClaim {

        @Test
        @DisplayName("holds when the engine decided the kind it expects")
        void holdsWhenTheKindMatches() {
            assertThat(new Claim("com.acme.Order", "AGGREGATE_ROOT", false).isSatisfiedBy("AGGREGATE_ROOT"))
                    .isTrue();
            assertThat(new Claim("com.acme.Order", "AGGREGATE_ROOT", false).isSatisfiedBy("ENTITY"))
                    .isFalse();
        }

        @Test
        @DisplayName("holds when the engine avoided the kind it rejects")
        void holdsWhenTheRejectedKindIsAvoided() {
            assertThat(new Claim("com.acme.Order", "VALUE_OBJECT", true).isSatisfiedBy("AGGREGATE_ROOT"))
                    .isTrue();
            assertThat(new Claim("com.acme.Order", "VALUE_OBJECT", true).isSatisfiedBy("VALUE_OBJECT"))
                    .isFalse();
        }
    }
}
