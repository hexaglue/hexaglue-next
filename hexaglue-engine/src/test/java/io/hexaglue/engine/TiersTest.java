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

package io.hexaglue.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TiersTest {

    private static Evidence at(EvidenceTier tier) {
        return Evidence.of(tier, tier.maxConfidence(), "FACT(" + tier.code() + ")", "because " + tier.code());
    }

    @Test
    @DisplayName("names the tier and calls one signal a signal")
    void namesTheTierAndCallsOneSignalASignal() {
        assertThat(Tiers.carrying(List.of(at(EvidenceTier.GRAPH_RELATION)))).isEqualTo("1 signal of graph relation");
    }

    @Test
    @DisplayName("counts the signals a tier carried")
    void countsTheSignalsATierCarried() {
        assertThat(Tiers.carrying(List.of(at(EvidenceTier.GRAPH_RELATION), at(EvidenceTier.GRAPH_RELATION))))
                .isEqualTo("2 signals of graph relation");
    }

    @Test
    @DisplayName("listing the tiers strongest first, whatever order the evidences arrive in")
    void listingTheTiersStrongestFirstWhateverOrderTheEvidencesArriveIn() {
        List<Evidence> weakestFirst =
                List.of(at(EvidenceTier.NAMING), at(EvidenceTier.GRAPH_RELATION), at(EvidenceTier.FRAMEWORK_KNOWLEDGE));

        assertThat(Tiers.carrying(weakestFirst))
                .isEqualTo("1 signal of framework knowledge, 1 of graph relation, 1 of naming");
    }

    @Test
    @DisplayName("ranking the kinds of signal strongest first, as the aggregator weighs them")
    void rankingTheKindsOfSignalStrongestFirstAsTheAggregatorWeighsThem() {
        assertThat(Tiers.ranking())
                .isEqualTo("declared intent > framework knowledge > graph relation > local structure"
                        + " > topology > naming");
    }

    @Test
    @DisplayName("refusing to summarise nothing, which would read as a decision made on no ground")
    void refusingToSummariseNothingWhichWouldReadAsADecisionMadeOnNoGround() {
        assertThatThrownBy(() -> Tiers.carrying(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
