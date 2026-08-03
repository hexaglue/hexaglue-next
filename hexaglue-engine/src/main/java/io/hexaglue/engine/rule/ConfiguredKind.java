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
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Rule;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Set;

/**
 * Takes the kind the user wrote in the configuration.
 *
 * <p>This is the last word available to someone whose code the engine reads wrongly: naming the
 * type and its kind ends the argument, without touching the sources. It is stated at the declared
 * intent tier, at zero distance — nothing outranks it, and nothing needs to.</p>
 *
 * @since 7.0.0
 */
public final class ConfiguredKind implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("CONFIG");

    ConfiguredKind() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE);
    }

    @Override
    public void apply(Derivation derivation) {
        for (TypeNode type : derivation.perimeter().types()) {
            derivation
                    .config()
                    .classification()
                    .declaredKind(type.id())
                    .ifPresent(kind -> derivation.derive(declaration(type, kind)));
        }
    }

    private static KindEvidence declaration(TypeNode type, ArchKind kind) {
        TypeId id = type.id();
        Evidence evidence = new Evidence(
                EvidenceTier.DECLARED_INTENT,
                EvidenceTier.DECLARED_INTENT.maxConfidence(),
                "classification.explicit(" + kind + ")",
                "the configuration declares " + id.qualifiedName() + " as " + kind,
                type.sourceLocation(),
                List.of());
        return KindEvidence.derived(
                id,
                kind,
                evidence,
                0,
                ID,
                ProofNode.fact("configuration declares " + id.qualifiedName() + " as " + kind));
    }
}
