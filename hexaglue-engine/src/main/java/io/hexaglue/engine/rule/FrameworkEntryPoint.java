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
import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.classification.RuleId;
import java.util.Set;

/**
 * Reads a type the framework calls from outside as the driving adapter it is.
 *
 * <p>This is the outermost ring, and the easiest to read: a controller, a message listener or a
 * JAX-RS resource exists to receive a call nobody in the application makes. Whoever wrote it chose
 * a framework symbol that says so, which is why an entry point is a fact about position rather
 * than a guess about intent.</p>
 *
 * <p>The ring matters beyond its own verdict: an adapter is what the next wave reads to tell a
 * driving port from an internal contract, and what keeps a type of the edge from being mistaken
 * for the core.</p>
 *
 * @since 7.0.0
 */
public final class FrameworkEntryPoint implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("W1-DA");

    FrameworkEntryPoint() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads a type the framework calls from outside as the driving adapter it is";
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
            if (assertion.finding().fact() == KnowledgeFact.DRIVING_ENTRYPOINT
                    && derivation.perimeter().contains(assertion.subject())) {
                Adapters.speak(
                        derivation,
                        assertion.subject(),
                        ArchKind.DRIVING_ADAPTER,
                        assertion,
                        "the framework calls it from outside the application",
                        ID);
            }
        }
    }
}
