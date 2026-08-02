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

import io.hexaglue.engine.rule.Catalogue;
import io.hexaglue.model.classification.RuleId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The rules of one analysis, ordered by identifier.
 *
 * <p>Ordering them by name rather than by registration is what makes a run repeatable: the
 * derivation order stops depending on how the set was assembled. Two rules cannot share an
 * identifier, because a proof naming a rule has to name exactly one.</p>
 *
 * @since 7.0.0
 */
public final class RuleSet {

    private final List<Rule> rules;

    private RuleSet(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Assembles a rule set, ordering it by identifier.
     *
     * @param rules the rules, in any order
     * @return the ordered rule set
     * @throws IllegalArgumentException when two rules claim the same identifier
     */
    public static RuleSet of(List<Rule> rules) {
        Objects.requireNonNull(rules, "rules must not be null");
        List<Rule> ordered = rules.stream()
                .sorted(Comparator.comparing(rule -> rule.id().value()))
                .toList();
        Set<RuleId> identities = new LinkedHashSet<>();
        for (Rule rule : ordered) {
            if (!identities.add(rule.id())) {
                throw new IllegalArgumentException("two rules claim the identifier: " + rule.id());
            }
        }
        return new RuleSet(ordered);
    }

    /**
     * Returns the rules the engine runs by default.
     *
     * @return the standard rule set
     */
    public static RuleSet standard() {
        return of(Catalogue.all());
    }

    /**
     * Returns the rules, in identifier order.
     *
     * @return the immutable rule list
     */
    public List<Rule> rules() {
        return rules;
    }
}
