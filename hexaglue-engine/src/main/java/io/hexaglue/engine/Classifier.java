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

import io.hexaglue.model.TypeId;
import java.util.List;
import java.util.Objects;

/**
 * Runs the analysis until the verdicts stop moving.
 *
 * <p>Classification is circular by nature: whether a type is an identifier depends on whether its
 * owner is an aggregate, which depends on whether a repository manages it. One pass cannot answer
 * that, so the engine reads the verdicts of the previous round, derives what follows, and decides
 * again — until a round changes nothing.</p>
 *
 * <p>Each round starts from an empty fact base rather than adding to the previous one. That is
 * the point: a signal derived from a reading that has since been revised must disappear with it.
 * Facts accumulate <em>within</em> a round, where nothing they depend on can change; verdicts are
 * recomputed <em>between</em> rounds, where things do.</p>
 *
 * @since 7.0.0
 */
public final class Classifier {

    /**
     * A chain of conclusions settles in two or three rounds. Beyond this, two rules are undoing
     * each other's work, and saying so beats looping.
     */
    private static final int MAX_ROUNDS = 8;

    private Classifier() {}

    /**
     * Classifies every type of the perimeter with the standard rules.
     *
     * @param context what the rules may read
     * @return the settled verdicts
     * @throws EngineException when the verdicts have not settled within the round ceiling
     */
    public static Verdicts classify(EngineContext context) {
        return classify(RuleSet.standard(), context);
    }

    /**
     * Classifies every type of the perimeter with the given rules.
     *
     * @param rules the rules to run
     * @param context what the rules may read
     * @return the settled verdicts
     * @throws EngineException when the verdicts have not settled within the round ceiling
     */
    public static Verdicts classify(RuleSet rules, EngineContext context) {
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Verdicts verdicts = Verdicts.none();
        for (int round = 1; round <= MAX_ROUNDS; round++) {
            Verdicts next =
                    Aggregator.decide(Saturation.saturate(rules, context.withVerdicts(verdicts)), context.perimeter());
            if (next.equals(verdicts)) {
                return next;
            }
            verdicts = next;
        }
        throw EngineException.of(EngineException.NO_CONVERGENCE, unsettled(rules, context, verdicts));
    }

    /**
     * Names the types still moving, which is where a rule contradicting another shows up.
     */
    private static String unsettled(RuleSet rules, EngineContext context, Verdicts verdicts) {
        Verdicts next =
                Aggregator.decide(Saturation.saturate(rules, context.withVerdicts(verdicts)), context.perimeter());
        List<TypeId> moving = verdicts.differencesWith(next);
        return "the verdicts did not settle within " + MAX_ROUNDS + " rounds; still moving: "
                + moving.stream()
                        .map(TypeId::qualifiedName)
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("none");
    }
}
