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
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the type an entry point delegates to as the application service it is.
 *
 * <p>The same layer as {@link PortPivot}, seen from the outside rather than from the ports. In a
 * codebase under migration the entry point is the surest thing the analysis has — the framework
 * marks it — and what it hands the work to is the application layer whether or not the outward
 * calls have been recognized as ports yet.</p>
 *
 * <p>Being reached from the ring is required and is never enough on its own: something must say
 * the type does application work rather than sit there. Either it calls a way out of its own, or
 * it carries the framework's word for the layer — {@code @Service}, {@code @Component},
 * {@code @Transactional}. That stereotype completes a case; it never opens one, which is the
 * difference between corroborating and deciding, and the reason a {@code @Service} nobody reaches
 * for stays unclassified.</p>
 *
 * @since 7.0.0
 */
public final class AdapterCollaborator implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R6b");

    private static final Set<TypeNature> IMPLEMENTATIONS = Set.of(TypeNature.CLASS, TypeNature.RECORD);

    AdapterCollaborator() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
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
        for (TypeNode type : derivation.perimeter().types()) {
            if (IMPLEMENTATIONS.contains(type.nature()) && !Contracts.onTheRing(derivation, type.id())) {
                read(derivation, type);
            }
        }
    }

    private void read(Derivation derivation, TypeNode type) {
        Optional<TypeId> entryPoint = Contracts.holdersOf(derivation, type.id()).stream()
                .map(TypeNode::id)
                .filter(holder -> derivation
                        .kindOf(holder)
                        .filter(ArchKind.DRIVING_ADAPTER::equals)
                        .isPresent())
                .findFirst();
        if (entryPoint.isEmpty()) {
            return;
        }
        corroboration(derivation, type)
                .ifPresent(corroboration -> Contracts.speak(
                        derivation,
                        type.id(),
                        ArchKind.APPLICATION_SERVICE,
                        "REACHED_FROM_THE_RING(" + entryPoint.orElseThrow().qualifiedName() + ")",
                        entryPoint.orElseThrow().simpleName() + " hands the work to it, and " + corroboration,
                        List.of(entryPoint.orElseThrow()),
                        ID));
    }

    /**
     * Returns what else says this type does application work, when anything does.
     */
    private static Optional<String> corroboration(Derivation derivation, TypeNode type) {
        Optional<TypeId> wayOut = Contracts.collaboratorsOf(derivation, type).stream()
                .filter(neighbour -> derivation
                        .kindOf(neighbour)
                        .filter(ArchKind.DRIVEN_PORT::equals)
                        .isPresent())
                .findFirst();
        if (wayOut.isPresent()) {
            return Optional.of(
                    "it reaches outside through " + wayOut.orElseThrow().simpleName());
        }
        return stereotype(derivation, type.id())
                .map(symbol -> "the framework knows it as application work (" + symbol + ")");
    }

    private static Optional<String> stereotype(Derivation derivation, TypeId subject) {
        return derivation.about(subject, KnowledgeAssertion.class).stream()
                .filter(assertion -> assertion.finding().fact() == KnowledgeFact.APPLICATION_STEREOTYPE)
                .map(assertion -> assertion.finding().symbol())
                .findFirst();
    }
}
