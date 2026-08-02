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

class CorpusExpectationsTest {

    @Nested
    @DisplayName("reads what a scenario is held to mean")
    class ReadsWhatAScenarioIsHeldToMean {

        @Test
        @DisplayName("for every scenario of the corpus, drafted or reviewed")
        void forEveryScenarioOfTheCorpus() {
            assertThat(Corpus.profile1())
                    .allSatisfy(scenario -> assertThat(
                                    CorpusExpectations.profile1(scenario.id()).scenarioId())
                            .isEqualTo(scenario.id()));
        }

        @Test
        @DisplayName("answering empty and unreviewed for a scenario that has no file")
        void emptyForAScenarioWithoutAFile() {
            CorpusExpectations expectations = CorpusExpectations.profile1("no-such-scenario");

            assertThat(expectations.reviewed()).isFalse();
            assertThat(expectations.claims()).isEmpty();
            assertThat(expectations.isScorable()).isFalse();
        }
    }

    @Nested
    @DisplayName("scores nothing")
    class ScoresNothing {

        @Test
        @DisplayName("while the file is still a draft, however many claims it carries")
        void whileTheFileIsStillADraft() {
            // Every scenario starts as a draft: the legacy engine carries bugs, so an imported
            // expectation means nothing until someone has read it.
            assertThat(Corpus.profile1())
                    .filteredOn(scenario ->
                            !CorpusExpectations.profile1(scenario.id()).reviewed())
                    .allSatisfy(scenario -> assertThat(
                                    CorpusExpectations.profile1(scenario.id()).isScorable())
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
