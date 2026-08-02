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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Runs the rules until nothing new can be derived.
 *
 * <p>The first round runs every rule; each round after it runs only the rules reading a predicate
 * that grew in the round before. A seed rule, which reads nothing, therefore runs exactly once —
 * and the cost of a round is the cost of what changed, not of the whole base.</p>
 *
 * <p>Rules only add facts and the universe of derivable facts is finite, so the loop reaches a
 * fixed point. The round ceiling is not what makes it terminate: it is what turns a rule that
 * fabricates a new fact every round into a coded failure instead of a build that hangs.</p>
 *
 * @since 7.0.0
 */
public final class Saturation {

    /**
     * Far above what a converging rule set needs — the deepest chain of dependent conclusions is
     * a handful of rounds — and low enough that a rule that never settles fails in a moment.
     */
    private static final int MAX_ROUNDS = 32;

    private Saturation() {}

    /**
     * Derives every fact the rules can reach from the context.
     *
     * @param rules the rules to run, in identifier order
     * @param context what the rules may read
     * @return the saturated fact base
     * @throws EngineException when the rules have not converged within the round ceiling
     */
    public static FactBase saturate(RuleSet rules, EngineContext context) {
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(context, "context must not be null");
        FactBase facts = new FactBase();
        Set<Predicate> pending = EnumSet.allOf(Predicate.class);
        for (int round = 1; !pending.isEmpty(); round++) {
            if (round > MAX_ROUNDS) {
                throw EngineException.of(EngineException.NO_CONVERGENCE, notConverging(rules, pending));
            }
            pending = runRound(rules, context, facts, pending, round);
        }
        return facts;
    }

    private static Set<Predicate> runRound(
            RuleSet rules, EngineContext context, FactBase facts, Set<Predicate> pending, int round) {
        Set<Predicate> grown = EnumSet.noneOf(Predicate.class);
        for (Rule rule : rules.rules()) {
            if (round == 1 || !Collections.disjoint(rule.reads(), pending)) {
                rule.apply(new Derivation(context, facts, rule, grown));
            }
        }
        return grown;
    }

    /**
     * Names the rules that could still be deriving, so the failure points at the suspects rather
     * than at the loop.
     */
    private static String notConverging(RuleSet rules, Set<Predicate> pending) {
        String suspects = rules.rules().stream()
                .filter(rule -> !Collections.disjoint(rule.reads(), pending))
                .map(rule -> rule.id().value())
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
        return "the rules did not converge within " + MAX_ROUNDS + " rounds; still deriving " + pending + " through: "
                + suspects;
    }
}
