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

import io.hexaglue.knowledge.FrameworkKnowledge;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.config.HexaGlueConfig;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What one rule is handed for one run: everything it may read, and the one thing it may do.
 *
 * <p>Handing rules a narrow view rather than the loop's own state is what keeps a rule from
 * reaching sideways — it cannot see the round number, the other rules, or what they derived this
 * round as opposed to earlier. All it sees is the fact base as it stands.</p>
 *
 * @since 7.0.0
 */
public final class Derivation {

    private final EngineContext context;
    private final FactBase facts;
    private final RuleId rule;
    private final Set<Predicate> writes;
    private final Set<Predicate> grown;

    Derivation(EngineContext context, FactBase facts, Rule rule, Set<Predicate> grown) {
        this.context = Objects.requireNonNull(context);
        this.facts = Objects.requireNonNull(facts);
        this.rule = rule.id();
        this.writes = Set.copyOf(rule.writes());
        this.grown = Objects.requireNonNull(grown);
    }

    /**
     * Returns the analyzed sources, classpath stubs included.
     *
     * @return the code model
     */
    public CodeModel code() {
        return context.code();
    }

    /**
     * Returns what the knowledge packs recognize.
     *
     * @return the framework knowledge
     */
    public FrameworkKnowledge knowledge() {
        return context.knowledge();
    }

    /**
     * Returns the user's configuration.
     *
     * @return the configuration
     */
    public HexaGlueConfig config() {
        return context.config();
    }

    /**
     * Returns the types owed a verdict.
     *
     * @return the perimeter
     */
    public Perimeter perimeter() {
        return context.perimeter();
    }

    /**
     * Returns what the previous round concluded about a type — empty on the first round, and on
     * any type outside the perimeter.
     *
     * @param id the type id
     * @return the verdict of the previous round
     */
    public Optional<Classification> verdict(TypeId id) {
        return context.verdicts().verdict(id);
    }

    /**
     * Returns the kind the previous round decided for a type, which is what a propagation rule
     * conditions on.
     *
     * @param id the type id
     * @return the kind of the previous round
     */
    public Optional<ArchKind> kindOf(TypeId id) {
        return context.verdicts().kindOf(id);
    }

    /**
     * Returns every fact of the given shape held so far, this rule's own derivations of this run
     * included.
     *
     * @param <F> the fact shape
     * @param factType the fact shape to select
     * @return the immutable list of facts, in subject then rendering order
     */
    public <F extends Fact> List<F> all(Class<F> factType) {
        return facts.all(factType);
    }

    /**
     * Returns what is known about one subject.
     *
     * @param <F> the fact shape
     * @param subject the type to look up
     * @param factType the fact shape to select
     * @return the immutable list of facts, in rendering order
     */
    public <F extends Fact> List<F> about(TypeId subject, Class<F> factType) {
        return facts.about(subject, factType);
    }

    /**
     * Records what follows from what the rule read.
     *
     * @param fact the derived fact
     * @throws EngineException when the fact's predicate is not one the rule declared writing
     */
    public void derive(Fact fact) {
        Objects.requireNonNull(fact, "fact must not be null");
        if (!writes.contains(fact.predicate())) {
            throw EngineException.of(
                    EngineException.UNDECLARED_PREDICATE,
                    "rule " + rule + " derived " + fact.predicate() + " without declaring it: " + fact.render());
        }
        if (facts.add(fact)) {
            grown.add(fact.predicate());
        }
    }
}
