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
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Set;

/**
 * Reads a contract the core fulfils and nothing inside takes as the way in it can only be.
 *
 * <p>{@link ExposedContract} reads a way in from the ring that calls it, which is the strongest
 * statement available and the one to prefer wherever it exists. It is also unavailable exactly when
 * a generator has the most to offer: a hexagon whose web layer has not been written yet has no
 * entry point to call anything, so every contract it exposes reads as nothing at all — and the
 * backend that exists to write that missing layer is handed no port to write it for.</p>
 *
 * <p>The reading here is the same relation approached from the other side. A contract the core
 * writes has a caller by construction, or it would not have been written; if no declaration of the
 * perimeter holds it, that caller is outside. A seam between two types of the core is excluded by
 * the same clause that makes the reading work — the type on the calling side holds it, and holding
 * it is what this rule requires nobody to do.</p>
 *
 * <p>Stated below the tier {@link ExposedContract} speaks at, because a ring that speaks is worth
 * more than a ring that is missing: where both readings apply they agree, and where they would not,
 * the one that observed a caller wins. What is read from an absence stays subordinate to what is
 * read from a presence.</p>
 *
 * @since 7.0.0
 */
public final class OfferedContract implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R5b");

    OfferedContract() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads a contract the core fulfils and nothing inside takes as the way in it can only be";
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE);
    }

    @Override
    public void apply(Derivation derivation) {
        for (TypeNode type : derivation.perimeter().types()) {
            if (type.nature() == TypeNature.INTERFACE) {
                read(derivation, type.id());
            }
        }
    }

    private void read(Derivation derivation, TypeId contract) {
        List<TypeNode> implementers = Contracts.implementersInTheCore(derivation, contract);
        if (implementers.isEmpty() || !Contracts.holdersOf(derivation, contract).isEmpty()) {
            return;
        }
        TypeId implementer = implementers.get(0).id();
        Evidence evidence = new Evidence(
                EvidenceTier.LOCAL_STRUCTURE,
                EvidenceTier.LOCAL_STRUCTURE.maxConfidence(),
                "OFFERED_BY_THE_CORE(" + implementer.qualifiedName() + ")",
                contract.qualifiedName() + " is a " + ArchKind.DRIVING_PORT + " because " + implementer.simpleName()
                        + " answers it and nothing inside the hexagon takes it, so whoever calls it is outside",
                derivation.code().type(contract).flatMap(TypeNode::sourceLocation),
                List.of(implementer));
        derivation.derive(KindEvidence.derived(contract, ArchKind.DRIVING_PORT, evidence, 0, ID));
    }
}
