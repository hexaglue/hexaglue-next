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
import io.hexaglue.engine.KnowledgeAssertion;
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Rule;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.Set;

/**
 * Records what the knowledge packs recognize on every type of the model.
 *
 * <p>Every type, not only those of the perimeter: a classpath stub is never classified, but
 * knowing that it is an {@code EntityManager} is exactly how the type holding one gets read as
 * reaching outside the hexagon. The perimeter limits verdicts, not knowledge.</p>
 *
 * <p>This rule interprets nothing. It puts what the packs state into the fact base so that every
 * rule reads the same knowledge, once, and so that a verdict cites the pack entry behind it.</p>
 *
 * @since 7.0.0
 */
public final class AssertKnowledge implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("KNOWLEDGE");

    AssertKnowledge() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "records what the knowledge packs recognize on every type of the model";
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.KNOWLEDGE);
    }

    @Override
    public void apply(Derivation derivation) {
        for (TypeNode type : derivation.code().types()) {
            derivation.knowledge().factsFor(derivation.code(), type).stream()
                    .map(finding -> new KnowledgeAssertion(type.id(), finding))
                    .forEach(derivation::derive);
        }
    }
}
