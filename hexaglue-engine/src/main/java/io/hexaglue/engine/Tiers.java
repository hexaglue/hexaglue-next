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

import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Says which tiers carried a set of evidences, and how many signals each brought.
 *
 * <p>The aggregator compares candidates on a single number, one decimal place per tier, because a
 * number is what sorting needs. That number is a comparison key and nothing else: a reader told a
 * verdict was {@code decided on 11000} learns nothing they could argue with. What the number
 * encodes — one signal at S2 and one at S3 — is what a reader can check against the sources, so
 * that is what gets written down wherever a decision is rendered.</p>
 *
 * <p>One reading, used by both the proof a decision states and the candidates an explanation
 * lists, so the two can never describe the same weighing differently.</p>
 */
final class Tiers {

    private Tiers() {}

    /**
     * Renders the tiers behind a set of evidences, strongest tier first.
     *
     * @param evidences the evidences supporting one candidate, in any order
     * @return a phrase such as {@code 1 signal at S2, 1 at S3}
     * @throws IllegalArgumentException when there is no evidence to summarise
     */
    static String carrying(List<Evidence> evidences) {
        if (evidences.isEmpty()) {
            throw new IllegalArgumentException("a decision is never carried by nothing");
        }
        Map<EvidenceTier, Integer> counts = new EnumMap<>(EvidenceTier.class);
        for (Evidence evidence : evidences) {
            counts.merge(evidence.tier(), 1, Integer::sum);
        }
        List<String> phrases = new ArrayList<>();
        for (Map.Entry<EvidenceTier, Integer> counted : counts.entrySet()) {
            phrases.add(phrase(counted.getKey(), counted.getValue(), phrases.isEmpty()));
        }
        return String.join(", ", phrases);
    }

    /**
     * Names the unit once, on the leading tier: {@code 1 signal at S2, 1 at S3} reads as a
     * sentence, where repeating the noun would read as a table.
     */
    private static String phrase(EvidenceTier tier, int count, boolean leading) {
        String unit = leading ? (count == 1 ? " signal" : " signals") : "";
        return count + unit + " at " + tier.code();
    }
}
