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

import io.hexaglue.model.arch.ArchModel;
import java.util.Objects;

/**
 * The whole engine in one call: a code model in, the classified model out.
 *
 * <p>Three steps, in this order and no other. The verdicts settle first, because a rule reading a
 * neighbour's kind has to read the final one. The facts are then derived once more against those
 * settled verdicts, because the fact base of each round is thrown away and only the last one
 * describes the codebase as the verdicts finally read it. The records are built from both.</p>
 *
 * @since 7.0.0
 */
public final class Analysis {

    private Analysis() {}

    /**
     * Analyzes the given context with the standard rules.
     *
     * @param context what the rules may read
     * @return the classified model, one entry per type of the perimeter
     * @throws EngineException when the verdicts have not settled within the round ceiling
     */
    public static ArchModel analyze(EngineContext context) {
        return analyze(RuleSet.standard(), context);
    }

    /**
     * Analyzes the given context with the given rules.
     *
     * @param rules the rules to run
     * @param context what the rules may read
     * @return the classified model, one entry per type of the perimeter
     * @throws EngineException when the verdicts have not settled within the round ceiling
     */
    public static ArchModel analyze(RuleSet rules, EngineContext context) {
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Verdicts verdicts = Classifier.classify(rules, context);
        FactBase facts = Saturation.saturate(rules, context.withVerdicts(verdicts));
        return Assembly.assemble(context, facts, verdicts);
    }
}
