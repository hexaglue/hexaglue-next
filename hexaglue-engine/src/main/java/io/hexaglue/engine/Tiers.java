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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Says what kind of signal carried a set of evidences, how many of each, and how the kinds rank
 * against one another.
 *
 * <p>The aggregator compares candidates on a single number, one decimal place per tier, because a
 * number is what sorting needs. That number is a comparison key and nothing else: a reader told a
 * verdict was {@code decided on 11000} learns nothing they could argue with. What the number
 * encodes — one signal of framework knowledge and one of graph relation — is what a reader can
 * check against the sources, so that is what gets written down wherever a decision is rendered.</p>
 *
 * <p>The tier codes S1 to S6 stay where they belong, in the rules and in the reference that
 * specifies them. They are an index, not an explanation: nothing in {@code S2} says what it is or
 * that it outranks {@code S3}, and a reader who has to be taught a private numbering before
 * reading a log has been handed the engine's filing system instead of its reasoning.</p>
 *
 * <p>One reading, used by the proof a decision states, the candidates an explanation lists and the
 * reasons it gives, so none of them can describe the same weighing differently.</p>
 */
final class Tiers {

    private Tiers() {}

    /**
     * Names a tier in the words that say what it is.
     *
     * @param tier the tier
     * @return its name, such as {@code framework knowledge}
     */
    static String named(EvidenceTier tier) {
        return tier.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    /**
     * Renders the pecking order of the tiers, strongest first.
     *
     * <p>Derived from the declaration order of {@link EvidenceTier}, which <em>is</em> the order
     * the aggregator weighs them in, so the line cannot drift from the behaviour it describes.</p>
     *
     * @return the ranking, as one line
     */
    static String ranking() {
        return Arrays.stream(EvidenceTier.values()).map(Tiers::named).collect(Collectors.joining(" > "));
    }

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
     * Names the unit once, on the leading tier: {@code 1 signal of framework knowledge, 1 of graph
     * relation} reads as a sentence, where repeating the noun would read as a table.
     */
    private static String phrase(EvidenceTier tier, int count, boolean leading) {
        String unit = leading ? (count == 1 ? " signal of " : " signals of ") : " of ";
        return count + unit + named(tier);
    }
}
