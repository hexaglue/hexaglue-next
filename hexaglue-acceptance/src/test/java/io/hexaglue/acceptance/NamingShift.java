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

import io.hexaglue.testkit.corpus.CorpusExpectations.Claim;
import java.util.List;
import java.util.Optional;

/**
 * One type whose verdict moved when the naming vocabulary was switched on, weighed against what a
 * reviewer had said about that type.
 *
 * @param qualifiedName the type whose verdict moved
 * @param withoutNames the kind read from position alone
 * @param withNames the kind read once names are part of the evidence
 * @param outcome what the move is worth against the reviewed answer
 */
record NamingShift(String qualifiedName, String withoutNames, String withNames, Outcome outcome) {

    /**
     * Weighs one move against what a reviewer vouched for.
     *
     * @param qualifiedName the type whose verdict moved
     * @param withoutNames the kind read from position alone
     * @param withNames the kind read once names are part of the evidence
     * @param claims what the scenario is held to mean, in file order
     * @return the move, weighed
     */
    static NamingShift of(String qualifiedName, String withoutNames, String withNames, List<Claim> claims) {
        Optional<Claim> reviewed = claims.stream()
                .filter(claim -> claim.qualifiedName().equals(qualifiedName))
                .findFirst();
        Outcome outcome =
                reviewed.map(claim -> outcomeOf(claim, withoutNames, withNames)).orElse(Outcome.UNARBITRATED);
        return new NamingShift(qualifiedName, withoutNames, withNames, outcome);
    }

    private static Outcome outcomeOf(Claim claim, String withoutNames, String withNames) {
        boolean held = claim.isSatisfiedBy(withoutNames);
        boolean holds = claim.isSatisfiedBy(withNames);
        if (held == holds) {
            return held ? Outcome.INDIFFERENT : Outcome.NEITHER;
        }
        return holds ? Outcome.GAIN : Outcome.DAMAGE;
    }

    /**
     * Renders the move as one line of the report, without its indentation.
     *
     * @return the line
     */
    String render() {
        return outcome.label() + "  " + qualifiedName + ": " + withoutNames + " -> " + withNames;
    }

    /** What switching the vocabulary on did to one verdict, judged against the reviewed answer. */
    enum Outcome {

        /** The name contradicted the reviewed answer, which position alone had reached. */
        DAMAGE("damage"),

        /** The name reached the reviewed answer where position alone had not. */
        GAIN("gain"),

        /** The verdict moved on a type no reviewer spoke about, so nobody can say which is right. */
        UNARBITRATED("unarbitrated"),

        /** Both readings miss the reviewed answer, differently. */
        NEITHER("neither"),

        /** Both readings satisfy the claim, which was about a kind neither of them names. */
        INDIFFERENT("indifferent");

        private final String label;

        Outcome(String label) {
            this.label = label;
        }

        /**
         * Returns the word the report uses.
         *
         * @return the label
         */
        String label() {
            return label;
        }
    }
}
