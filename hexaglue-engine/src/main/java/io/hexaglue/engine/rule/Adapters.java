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
import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;

/**
 * What the two readings of the outer ring say in common: a framework symbol places a type at the
 * edge, and the verdict cites the pack entry that placed it there.
 *
 * @since 7.0.0
 */
final class Adapters {

    private Adapters() {}

    /**
     * States that a type sits on the outer ring, on the strength of what a pack recognized.
     *
     * @param derivation the derivation the rule is running under
     * @param subject the type placed at the edge
     * @param kind the side of the ring it sits on
     * @param premise what the pack recognized, cited as the proof of the verdict
     * @param why the reason, phrased for a reader of the report
     * @param rule the rule concluding it
     */
    static void speak(
            Derivation derivation, TypeId subject, ArchKind kind, KnowledgeAssertion premise, String why, RuleId rule) {
        KnowledgeFact fact = premise.finding().fact();
        Evidence evidence = new Evidence(
                fact.tier(),
                fact.tier().maxConfidence(),
                fact + "(" + premise.subject().qualifiedName() + ")",
                subject.qualifiedName() + " is a " + kind + " because " + why + " ("
                        + premise.finding().symbol() + ")",
                derivation.code().type(subject).flatMap(TypeNode::sourceLocation),
                List.of(premise.subject()));
        derivation.derive(KindEvidence.derived(subject, kind, evidence, 0, rule, premise.proof()));
    }
}
