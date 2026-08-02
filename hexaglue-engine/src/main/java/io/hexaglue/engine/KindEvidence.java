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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RuleId;
import java.util.Objects;

/**
 * One signal for one kind on one type. Evidences accumulate and compete; none of them decides.
 *
 * <p>The kind is carried by the evidence itself and never by the rule that emitted it: a rule
 * says what it saw, and the same rule can speak for different kinds on different types. That is
 * what keeps a rule from owning a kind, and a kind from being lost when a rule is reused.</p>
 *
 * <p>The distance says how far from the type the signal was found: zero when the declaration
 * itself carries it, one when its nearest supertype does, and so on. It is the tie-breaker at
 * equal tier — a type implementing {@code AggregateRoot} inherits {@code Entity} too, and the
 * nearer of the two is the one the author meant.</p>
 *
 * @param subject the type the signal speaks about
 * @param kind the kind the signal supports
 * @param evidence the signal itself: tier, force, fact, justification and location
 * @param distance how many inheritance steps away the signal was found, zero on the declaration
 * @param proof how the signal was reached
 * @since 7.0.0
 */
public record KindEvidence(TypeId subject, ArchKind kind, Evidence evidence, int distance, ProofNode proof)
        implements Fact {

    /**
     * Validates the components and the distance.
     */
    public KindEvidence {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(proof, "proof must not be null");
        if (distance < 0) {
            throw new IllegalArgumentException("distance must be >= 0, got " + distance);
        }
        if (kind == ArchKind.UNCLASSIFIED) {
            throw new IllegalArgumentException(
                    subject + " cannot be evidenced as UNCLASSIFIED: that is the absence of a verdict, not a signal");
        }
    }

    /**
     * Creates a signal a rule concluded, with the proof of how it did.
     *
     * <p>The conclusion of the proof is the rendering of the fact itself, so the tree reads as
     * the derivation it is rather than as a paraphrase of it.</p>
     *
     * @param subject the type the signal speaks about
     * @param kind the kind the signal supports
     * @param evidence the signal itself
     * @param distance how many inheritance steps away the signal was found
     * @param rule the rule concluding it
     * @param premises the facts the rule read to conclude
     * @return a new KindEvidence
     */
    public static KindEvidence derived(
            TypeId subject, ArchKind kind, Evidence evidence, int distance, RuleId rule, ProofNode... premises) {
        return new KindEvidence(
                subject,
                kind,
                evidence,
                distance,
                ProofNode.derived(rule, render(subject, kind, evidence, distance), premises));
    }

    @Override
    public Predicate predicate() {
        return Predicate.EVIDENCE;
    }

    @Override
    public String render() {
        return render(subject, kind, evidence, distance);
    }

    private static String render(TypeId subject, ArchKind kind, Evidence evidence, int distance) {
        return kind + "(" + subject.qualifiedName() + ") [" + evidence.tier().code() + " " + evidence.fact() + " d"
                + distance + "]";
    }
}
