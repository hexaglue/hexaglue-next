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

import io.hexaglue.acceptance.NamingShift.Outcome;
import io.hexaglue.testkit.corpus.CorpusExpectations.Claim;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A measurement is only worth what its instrument is worth.
 *
 * <p>The report next door concludes that reading names buys nothing, and a reader is entitled to
 * ask whether the harness could have seen a gain at all. So each outcome is exercised on its own,
 * against a claim written here rather than harvested: a gain, a damage, both misses, the claim that
 * does not discriminate, and the type nobody reviewed.</p>
 */
class NamingShiftTest {

    private static final String SUBJECT = "com.acme.OrderId";

    private static final List<Claim> EXPECTS_IDENTIFIER = List.of(new Claim(SUBJECT, "IDENTIFIER", false));

    @Test
    @DisplayName("counts a gain when the name reaches the reviewed answer position had missed")
    void countsAGain() {
        NamingShift shift = NamingShift.of(SUBJECT, "UNCLASSIFIED", "IDENTIFIER", EXPECTS_IDENTIFIER);

        assertThat(shift.outcome()).isEqualTo(Outcome.GAIN);
    }

    @Test
    @DisplayName("counts damage when the name contradicts an answer position had reached")
    void countsDamage() {
        NamingShift shift = NamingShift.of(SUBJECT, "IDENTIFIER", "VALUE_OBJECT", EXPECTS_IDENTIFIER);

        assertThat(shift.outcome()).isEqualTo(Outcome.DAMAGE);
    }

    @Test
    @DisplayName("scores nothing when the type is one no reviewer spoke about")
    void scoresNothingWithoutAReviewer() {
        NamingShift shift = NamingShift.of("com.acme.Elsewhere", "UNCLASSIFIED", "IDENTIFIER", EXPECTS_IDENTIFIER);

        assertThat(shift.outcome()).isEqualTo(Outcome.UNARBITRATED);
    }

    @Test
    @DisplayName("counts neither when both readings miss the reviewed answer")
    void countsNeitherWhenBothMiss() {
        NamingShift shift = NamingShift.of(SUBJECT, "UNCLASSIFIED", "VALUE_OBJECT", EXPECTS_IDENTIFIER);

        assertThat(shift.outcome()).isEqualTo(Outcome.NEITHER);
    }

    @Test
    @DisplayName("counts the move indifferent when the claim rejects a kind neither reading names")
    void countsIndifferentWhenTheClaimRejectsSomethingElse() {
        List<Claim> rejectsEntity = List.of(new Claim(SUBJECT, "ENTITY", true));

        NamingShift shift = NamingShift.of(SUBJECT, "UNCLASSIFIED", "IDENTIFIER", rejectsEntity);

        assertThat(shift.outcome()).isEqualTo(Outcome.INDIFFERENT);
    }

    @Test
    @DisplayName("renders the move so a reader sees the direction before the verdict")
    void rendersTheMove() {
        NamingShift shift = NamingShift.of(SUBJECT, "UNCLASSIFIED", "IDENTIFIER", EXPECTS_IDENTIFIER);

        assertThat(shift.render()).isEqualTo("gain  com.acme.OrderId: UNCLASSIFIED -> IDENTIFIER");
    }
}
