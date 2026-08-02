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

package io.hexaglue.engine.rule;

import io.hexaglue.engine.Derivation;
import io.hexaglue.engine.KindEvidence;
import io.hexaglue.engine.KnowledgeAssertion;
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Rule;
import io.hexaglue.knowledge.KnowledgeFinding;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Takes the kind the author declared in the sources, through an intent annotation or an intent
 * interface.
 *
 * <p>The kind is carried by the pack entry and read from it — the rule never holds a kind of its
 * own. That is what lets a user pack declare an in-house marker and get the same treatment as
 * jMolecules, and what keeps the declared kind from being lost between the entry and the
 * verdict.</p>
 *
 * <p>An intent interface can be inherited through several steps, and jMolecules is built that
 * way: {@code AggregateRoot} extends {@code Entity}, so a type implementing the first declares
 * both. Both signals are emitted, each with the distance at which it was found, and the decision
 * keeps the nearer one — which is the one the author wrote.</p>
 *
 * @since 7.0.0
 */
public final class DeclaredKind implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("S1-INTENT");

    DeclaredKind() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public Set<Predicate> reads() {
        return Set.of(Predicate.KNOWLEDGE);
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE);
    }

    @Override
    public void apply(Derivation derivation) {
        for (KnowledgeAssertion assertion : derivation.all(KnowledgeAssertion.class)) {
            if (!derivation.perimeter().contains(assertion.subject())) {
                continue;
            }
            Optional<ArchKind> declared = assertion.finding().declaredKind();
            if (declared.isPresent()) {
                derivation.derive(intent(derivation, assertion, declared.orElseThrow()));
            }
        }
    }

    private static KindEvidence intent(Derivation derivation, KnowledgeAssertion assertion, ArchKind kind) {
        TypeId subject = assertion.subject();
        KnowledgeFinding finding = assertion.finding();
        Evidence evidence = new Evidence(
                finding.fact().tier(),
                finding.fact().tier().maxConfidence(),
                finding.fact() + "(" + kind + ")",
                subject.qualifiedName() + " declares " + kind + " by bearing " + finding.symbol() + " (pack "
                        + finding.packId() + ")",
                derivation.code().type(subject).flatMap(TypeNode::sourceLocation),
                List.of());
        return KindEvidence.derived(
                subject, kind, evidence, distanceTo(derivation, subject, finding.symbol()), ID, assertion.proof());
    }

    /**
     * Measures how far the symbol sits from the declaration: zero when the type itself bears it,
     * one when its nearest supertype does, and one more per step up the closure.
     */
    private static int distanceTo(Derivation derivation, TypeId subject, String symbol) {
        int rank = derivation.code().supertypesOf(subject).indexOf(TypeId.of(symbol));
        return rank < 0 ? 0 : rank + 1;
    }
}
