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
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads a contract the core fulfils and the outer ring calls as the driving port it is.
 *
 * <p>The mirror image of a way out: here the core is on the answering side and the caller sits
 * outside, so the contract is how the world reaches in. Both halves are required, and each one
 * alone means something else — a contract nobody outside calls is an internal seam, and a contract
 * the core does not fulfil is an outward call the ring happens to make.</p>
 *
 * <p>Holding the contract is enough to read the relation: dependency injection puts the port in a
 * field or a constructor parameter, and that placement is visible in every codebase whatever the
 * frontend was asked for. When method bodies were read, the invocation itself is stated as a second
 * reading of the same port — the capability adds to the case, it never carries it, so the verdict
 * does not depend on how the analysis was configured.</p>
 *
 * <p>An entry point holding the implementation instead of the contract reaches past the port
 * entirely. Nothing is read there: no port was used, so none is stated, and naming the shortcut is
 * the conformity question rather than this one.</p>
 *
 * @since 7.0.0
 */
public final class ExposedContract implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R5");

    ExposedContract() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads a contract the core fulfils and the outer ring calls as the driving port it is";
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
        if (implementers.isEmpty()) {
            return;
        }
        List<TypeNode> callers = Contracts.holdersOf(derivation, contract).stream()
                .filter(holder -> derivation
                        .kindOf(holder.id())
                        .filter(ArchKind.DRIVING_ADAPTER::equals)
                        .isPresent())
                .toList();
        if (callers.isEmpty()) {
            return;
        }
        TypeId caller = callers.get(0).id();
        TypeId implementer = implementers.get(0).id();
        Contracts.speak(
                derivation,
                contract,
                ArchKind.DRIVING_PORT,
                "HELD_BY_DRIVING_ADAPTER(" + caller.qualifiedName() + ")",
                caller.simpleName() + " reaches the application through it and " + implementer.simpleName()
                        + " answers it",
                List.of(caller, implementer),
                ID);
        invocation(derivation, contract, callers)
                .ifPresent(invoker -> Contracts.speak(
                        derivation,
                        contract,
                        ArchKind.DRIVING_PORT,
                        "CALLED_BY_DRIVING_ADAPTER(" + invoker.qualifiedName() + ")",
                        "a method of " + invoker.simpleName() + " calls it, which the bodies show directly",
                        List.of(invoker, implementer),
                        ID));
    }

    /**
     * Returns the first entry point observed calling the contract, when the bodies were read: an
     * invocation edge exists only under that capability.
     */
    private static Optional<TypeId> invocation(Derivation derivation, TypeId contract, List<TypeNode> callers) {
        return callers.stream()
                .map(TypeNode::id)
                .filter(caller -> derivation.code().edgesFrom(caller).stream().anyMatch(edge -> calls(edge, contract)))
                .findFirst();
    }

    private static boolean calls(Edge edge, TypeId contract) {
        return edge.kind() == EdgeKind.INVOKES && edge.target().equals(contract);
    }
}
