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

import io.hexaglue.model.classification.RuleId;
import java.util.Set;

/**
 * One inference step: read the context and the facts already held, derive what follows.
 *
 * <p>A rule is a pure function of what it is given. It holds no state between runs — the
 * saturation loop calls it as many times as its inputs grow, and a rule that remembered anything
 * would make the outcome depend on the number of rounds. What it concluded last round is in the
 * fact base, which is the only memory there is.</p>
 *
 * <p>The declared reads and writes are a contract the scheduler acts on: a rule is re-run only
 * when a predicate it reads has grown, and deriving an undeclared predicate is refused rather
 * than silently unscheduling the rules that were waiting for it.</p>
 *
 * @since 7.0.0
 */
public interface Rule {

    /**
     * Returns the published identifier of this rule, which orders it among the others and names
     * it in every proof it produces.
     *
     * @return the rule id
     */
    RuleId id();

    /**
     * Returns the predicates this rule consumes. A rule reading nothing is a seed: it depends
     * only on the context, so it runs once.
     *
     * @return the read predicates, empty for a seed rule
     */
    default Set<Predicate> reads() {
        return Set.of();
    }

    /**
     * Returns the predicates this rule may derive.
     *
     * @return the written predicates
     */
    Set<Predicate> writes();

    /**
     * Derives everything that follows from the current state.
     *
     * @param derivation the context, the facts held so far, and the sink to derive into
     */
    void apply(Derivation derivation);
}
