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
import io.hexaglue.engine.PortRole;
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Rule;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Set;

/**
 * Reads what the domain announces as the domain event it is.
 *
 * <p>An event is something that happened, so it cannot change afterwards and it travels outward.
 * Two places in a codebase say that without a word: a way out whose whole trade is announcing —
 * every method one-way, carrying values — and an aggregate answering with something it does not
 * keep. Both readings need immutability, which is why a marker interface says nothing here: an
 * interface holds no state, so nothing about it can have happened.</p>
 *
 * <p>The second reading turns on what the aggregate holds, because that is what separates the two
 * things an aggregate method can hand back. A method answering with one of its own fields is a
 * reader of its state; a method answering with something it never keeps has built it, and a domain
 * type built to be handed out is the event. Without that distinction every getter on an aggregate
 * would announce something.</p>
 *
 * @since 7.0.0
 */
public final class PublishedEvent implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R7");

    PublishedEvent() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public Set<Predicate> reads() {
        return Set.of(Predicate.PORT_ROLE);
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE);
    }

    @Override
    public void apply(Derivation derivation) {
        for (PortRole role : derivation.all(PortRole.class)) {
            if (role.role() == DrivenPortType.EVENT_PUBLISHER) {
                derivation.code().type(role.subject()).ifPresent(port -> readAnnouncements(derivation, port));
            }
        }
        for (TypeNode type : derivation.perimeter().types()) {
            if (derivation
                    .kindOf(type.id())
                    .filter(ArchKind.AGGREGATE_ROOT::equals)
                    .isPresent()) {
                readAnswers(derivation, type);
            }
        }
    }

    /** What a way out carries outward and never asks about. */
    private void readAnnouncements(Derivation derivation, TypeNode port) {
        for (TypeId carried :
                Signatures.taken(derivation, port).stream().distinct().toList()) {
            speak(
                    derivation,
                    carried,
                    "ANNOUNCED_BY(" + port.id().qualifiedName() + ")",
                    port.id().simpleName() + " carries it outward and never asks anything back",
                    port.id());
        }
    }

    /** What an aggregate hands back without keeping it. */
    private void readAnswers(Derivation derivation, TypeNode aggregate) {
        List<TypeId> kept = Lifecycle.keptBy(aggregate);
        for (TypeId answered : Signatures.returned(derivation, aggregate).stream()
                .distinct()
                .filter(answer -> !kept.contains(answer))
                .toList()) {
            speak(
                    derivation,
                    answered,
                    "ANSWERED_BY(" + aggregate.id().qualifiedName() + ")",
                    aggregate.id().simpleName() + " hands it back without keeping any of it",
                    aggregate.id());
        }
    }

    /**
     * States the reading, on the one condition both readings share: what happened cannot be
     * edited afterwards.
     */
    private void speak(Derivation derivation, TypeId subject, String fact, String why, TypeId related) {
        derivation
                .code()
                .type(subject)
                .filter(Shapes::isImmutable)
                .ifPresent(node ->
                        Contracts.speak(derivation, subject, ArchKind.DOMAIN_EVENT, fact, why, List.of(related), ID));
    }
}
