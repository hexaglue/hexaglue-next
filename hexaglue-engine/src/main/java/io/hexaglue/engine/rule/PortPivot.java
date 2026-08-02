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
import java.util.Optional;
import java.util.Set;

/**
 * Reads the type standing between the ports as the application service it is.
 *
 * <p>Once the boundary is known, the application layer is what touches it from the inside: a class
 * answering a way in, calling a way out, or doing both. That is the whole of the reading, and it
 * needs neither a stereotype nor a suffix — a use case is a position between two holes in the wall,
 * and the wall was placed by the wave before this one.</p>
 *
 * <p>Either half suffices. A class answering a way in that reaches nothing outward still
 * orchestrates a use case; a class calling a way out that no port exposes is still the layer that
 * decides when to call it. Requiring both would silence the halves that most codebases actually
 * write.</p>
 *
 * <p>A type the ring already claims is left where it is. An entry point holding a way out directly
 * has skipped the application layer, and that shortcut is a finding rather than a promotion:
 * reading it as the layer it bypassed would erase the very thing an audit is looking for.</p>
 *
 * @since 7.0.0
 */
public final class PortPivot implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R6");

    private static final Set<TypeNature> IMPLEMENTATIONS = Set.of(TypeNature.CLASS, TypeNature.RECORD);

    PortPivot() {
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
        Optional<TypeId> answered = portOf(derivation, Contracts.contractsOf(derivation, type), ArchKind.DRIVING_PORT);
        if (answered.isPresent()) {
            speak(
                    derivation,
                    type.id(),
                    "ANSWERS_DRIVING_PORT",
                    answered.orElseThrow(),
                    ", through which the outside reaches the application");
            return;
        }
        portOf(derivation, Contracts.collaboratorsOf(derivation, type), ArchKind.DRIVEN_PORT)
                .ifPresent(called -> speak(
                        derivation,
                        type.id(),
                        "CALLS_DRIVEN_PORT",
                        called,
                        ", through which the application reaches outside"));
    }

    private static void speak(Derivation derivation, TypeId pivot, String relation, TypeId port, String about) {
        Contracts.speak(
                derivation,
                pivot,
                ArchKind.APPLICATION_SERVICE,
                relation + "(" + port.qualifiedName() + ")",
                "it stands on " + port.simpleName() + about,
                List.of(port),
                ID);
    }

    /**
     * Returns the first of the given neighbours the previous round read as a port of that
     * direction.
     */
    private static Optional<TypeId> portOf(Derivation derivation, List<TypeId> neighbours, ArchKind direction) {
        return neighbours.stream()
                .filter(neighbour ->
                        derivation.kindOf(neighbour).filter(direction::equals).isPresent())
                .findFirst();
    }
}
