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

package io.hexaglue.model.classification;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One node of a proof tree: a conclusion, the rule that derived it and the premises the rule
 * consumed. A base fact carries no rule and no premises. Every derived fact remembers how it came
 * to be — the tree feeds {@code explain}, the audit and the golden files for free.
 *
 * @param conclusion the derived or observed fact, rendered canonically
 * @param rule the rule that derived the conclusion, absent for a base fact
 * @param premises the facts the rule consumed, in rule order
 * @since 7.0.0
 */
public record ProofNode(String conclusion, Optional<RuleId> rule, List<ProofNode> premises) {

    /**
     * Validates that a rule-less node is a leaf and copies the premises.
     */
    public ProofNode {
        Objects.requireNonNull(conclusion, "conclusion must not be null");
        Objects.requireNonNull(rule, "rule must not be null");
        Objects.requireNonNull(premises, "premises must not be null");
        if (conclusion.isBlank()) {
            throw new IllegalArgumentException("conclusion must not be blank");
        }
        if (rule.isEmpty() && !premises.isEmpty()) {
            throw new IllegalArgumentException("a base fact cannot have premises without a rule");
        }
        premises = List.copyOf(premises);
    }

    /**
     * Creates a base fact: observed, not derived.
     *
     * @param conclusion the observed fact
     * @return a new leaf ProofNode
     */
    public static ProofNode fact(String conclusion) {
        return new ProofNode(conclusion, Optional.empty(), List.of());
    }

    /**
     * Creates a derived fact.
     *
     * @param rule the rule that fired
     * @param conclusion the derived fact
     * @param premises the facts the rule consumed
     * @return a new ProofNode
     */
    public static ProofNode derived(RuleId rule, String conclusion, ProofNode... premises) {
        return new ProofNode(conclusion, Optional.of(rule), List.of(premises));
    }
}
