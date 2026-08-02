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
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RuleId;
import java.util.Objects;

/**
 * A tie between two types that a rule established.
 *
 * <p>A verdict says what a type is; a relation says what it is to another. Both are needed:
 * knowing that an interface is a driven port leaves out which aggregate it manages, and that
 * second half is what a report and a generator read. Stating it here, once, is what keeps a
 * consumer from re-deriving it from names downstream.</p>
 *
 * @param subject the type the relation is stated about
 * @param kind what the relation says
 * @param object the type at the other end
 * @param proof how the relation was established
 * @since 7.0.0
 */
public record Relation(TypeId subject, RelationKind kind, TypeId object, ProofNode proof) implements Fact {

    /**
     * Validates that every component is present.
     */
    public Relation {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(object, "object must not be null");
        Objects.requireNonNull(proof, "proof must not be null");
    }

    /**
     * Creates a tie a rule concluded, with the proof of how it did.
     *
     * @param subject the type the relation is stated about
     * @param kind what the relation says
     * @param object the type at the other end
     * @param rule the rule concluding it
     * @param premises the facts the rule read to conclude
     * @return a new Relation
     */
    public static Relation derived(
            TypeId subject, RelationKind kind, TypeId object, RuleId rule, ProofNode... premises) {
        return new Relation(subject, kind, object, ProofNode.derived(rule, render(subject, kind, object), premises));
    }

    @Override
    public Predicate predicate() {
        return Predicate.RELATION;
    }

    @Override
    public String render() {
        return render(subject, kind, object);
    }

    private static String render(TypeId subject, RelationKind kind, TypeId object) {
        return kind + "(" + subject.qualifiedName() + ", " + object.qualifiedName() + ")";
    }
}
