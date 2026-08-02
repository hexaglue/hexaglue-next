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

/**
 * The solver: facts in, a classified model out, with the proof of every verdict.
 *
 * <p>Classifying a type means reading signals about it — what the author declared, what a
 * framework implies, how the neighbours use it, what its shape is — and weighing them. The
 * signals depend on each other: whether a type is an identifier depends on whether its owner is
 * an aggregate, which depends on whether a repository manages it. One pass over the types cannot
 * answer that; the engine derives facts until nothing new can be derived, then decides.</p>
 *
 * <p>The shape is a fact base saturated by rules. The code model is the base stratum: the rules
 * read it and never change it. Each rule declares the predicates it reads and the predicates it
 * writes, and a round only re-runs the rules whose inputs grew — so the loop costs what the new
 * facts cost, not what the whole base costs. Rules only ever add facts, and the universe of facts
 * is finite, so the loop reaches a fixed point; a rule that keeps inventing facts hits a round
 * ceiling and fails loudly rather than spinning.</p>
 *
 * <p>Every fact carries the proof of how it was reached, so explaining a verdict costs nothing
 * extra: the tree is already there. And nothing here is ordered by chance — the fact base is
 * sorted, the rules run in identifier order, so two runs on the same sources derive the same
 * facts in the same order.</p>
 *
 * @since 7.0.0
 */
package io.hexaglue.engine;
