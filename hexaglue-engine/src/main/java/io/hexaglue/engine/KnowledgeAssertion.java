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

import io.hexaglue.knowledge.KnowledgeFinding;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.ProofNode;
import java.util.Objects;

/**
 * What a knowledge pack recognizes on a type: the technical fact, the symbol that carries it and
 * the pack that states it.
 *
 * <p>This is an observation, not a conclusion — the packs do not classify. Turning it into a
 * signal for a kind is the job of a rule, and every such rule cites the assertion as its premise,
 * so a verdict can always be traced back to the pack entry behind it.</p>
 *
 * @param subject the type the pack recognized something on
 * @param finding what the pack recognized, captures included
 * @since 7.0.0
 */
public record KnowledgeAssertion(TypeId subject, KnowledgeFinding finding) implements Fact {

    /**
     * Validates that the assertion names both its subject and what was recognized.
     */
    public KnowledgeAssertion {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(finding, "finding must not be null");
    }

    @Override
    public Predicate predicate() {
        return Predicate.KNOWLEDGE;
    }

    @Override
    public String render() {
        return finding.fact() + "(" + subject.qualifiedName() + ") [" + finding.packId() + ":" + finding.symbol() + "]";
    }

    @Override
    public ProofNode proof() {
        return ProofNode.fact(render());
    }
}
