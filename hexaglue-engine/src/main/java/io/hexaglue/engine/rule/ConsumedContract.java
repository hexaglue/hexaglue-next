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
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Rule;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Set;

/**
 * Reads a contract the core calls and nothing inside fulfils as the driven port it is.
 *
 * <p>This is the hexagon seen from the inside: something in the core keeps an interface as a
 * collaborator, and no implementation of that interface lives in the core. Whatever answers those
 * calls therefore lies outside — which is the definition of a way out, and the reason the
 * declaration alone can never say it. A Spring Data repository has no implementation in the sources
 * at all, because the framework writes it; an interface with no implementation anywhere is a port
 * the deployment still has to fill. Both read the same, and neither is read from a name.</p>
 *
 * <p>What blocks the reading is an implementation on the near side: see {@link Contracts} for why
 * a contract the core both writes and calls is a seam rather than a boundary.</p>
 *
 * @since 7.0.0
 */
public final class ConsumedContract implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R4");

    ConsumedContract() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads a contract the core calls and nothing inside fulfils as the driven port it is";
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
        if (!Contracts.implementersInTheCore(derivation, contract).isEmpty()) {
            return;
        }
        List<TypeNode> callers = Contracts.holdersInTheCore(derivation, contract);
        if (callers.isEmpty()) {
            return;
        }
        TypeId caller = callers.get(0).id();
        Contracts.speak(
                derivation,
                contract,
                ArchKind.DRIVEN_PORT,
                "CONSUMED_BY_CORE(" + caller.qualifiedName() + ")",
                caller.simpleName() + " calls it and nothing inside the hexagon implements it",
                callers.stream().map(TypeNode::id).toList(),
                ID);
    }
}
