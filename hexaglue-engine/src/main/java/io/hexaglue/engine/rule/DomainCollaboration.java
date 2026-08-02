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
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Set;

/**
 * Reads behaviour the domain owns but no single type of it can hold as the domain service it is.
 *
 * <p>Four conditions, and each one removes a different neighbour. Nothing it keeps can change, or
 * it remembers something and is an entity. It calls no way out, or it is the application layer
 * deciding when to reach outside. It spans at least two types of the domain, or the behaviour
 * belongs to the one type it works on and should be a method there. And something inside calls it,
 * or its position is not observable at all — a type nobody uses could be anything.</p>
 *
 * <p>The strictness is the point. This kind was unreachable in the previous engine, which is worse
 * than rare: a whole family of domain code had nowhere to land and was reported as unclassified or,
 * worse, as an application service that then looked like a layering violation.</p>
 *
 * @since 7.0.0
 */
public final class DomainCollaboration implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R8");

    /** Below two domain types there is no collaboration, only a method on the wrong class. */
    private static final int COLLABORATION = 2;

    private static final Set<TypeNature> IMPLEMENTATIONS = Set.of(TypeNature.CLASS, TypeNature.RECORD);

    DomainCollaboration() {
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
            if (IMPLEMENTATIONS.contains(type.nature()) && !Contracts.onTheRing(derivation, type.id())) {
                read(derivation, type);
            }
        }
    }

    private void read(Derivation derivation, TypeNode type) {
        if (!remembersNothing(type) || callsAWayOut(derivation, type)) {
            return;
        }
        List<TypeId> spanned = domainTypesOf(derivation, type);
        if (spanned.size() < COLLABORATION
                || Contracts.holdersInTheCore(derivation, type.id()).isEmpty()) {
            return;
        }
        Contracts.speak(
                derivation,
                type.id(),
                ArchKind.DOMAIN_SERVICE,
                "SPANS_DOMAIN_TYPES(" + spanned.size() + ")",
                "it keeps nothing that can change, reaches nothing outside, and works across "
                        + spanned.stream()
                                .map(TypeId::simpleName)
                                .reduce((left, right) -> left + " and " + right)
                                .orElseThrow(),
                spanned,
                ID);
    }

    /**
     * Answers whether nothing the declaration keeps can change between two calls. A type with no
     * state at all qualifies — which is why this is not {@code Shapes.isImmutable}: there, holding
     * nothing is the absence of a signal, and here it is the nominal case.
     */
    private static boolean remembersNothing(TypeNode type) {
        return Shapes.state(type).stream().allMatch(field -> field.modifiers().contains(Modifier.FINAL));
    }

    private static boolean callsAWayOut(Derivation derivation, TypeNode type) {
        return Contracts.collaboratorsOf(derivation, type).stream()
                .anyMatch(neighbour ->
                        derivation.kindOf(neighbour).filter(ArchKind::isPort).isPresent());
    }

    /**
     * Returns the distinct types of the domain the declaration's methods work across.
     */
    private static List<TypeId> domainTypesOf(Derivation derivation, TypeNode type) {
        return Signatures.mentioned(derivation, type).stream()
                .distinct()
                .filter(mentioned ->
                        derivation.kindOf(mentioned).filter(ArchKind::isDomain).isPresent())
                .toList();
    }
}
