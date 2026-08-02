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
import io.hexaglue.engine.PortRole;
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Relation;
import io.hexaglue.engine.RelationKind;
import io.hexaglue.engine.Rule;
import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads what trade a driven port plies from the shape of the signatures it declares.
 *
 * <p>Three shapes, and not one method name among them. Signatures converging on a single type of
 * the perimeter — it comes back as a result, bare or wrapped, and goes in as an argument — are how
 * storage is written, whatever the methods are called: {@code find}, {@code load}, {@code get} and
 * {@code byId} all read the same because none of them is read. Methods that only ever go out, void
 * and carrying values that cannot change, are how something is announced. Everything else is a call
 * to a service, and reads as a gateway.</p>
 *
 * <p>Convergence must be on exactly one type. A port whose signatures revolve around two subjects
 * is not converging on either, and calling it a repository would pick one of them for no reason; it
 * falls back to the general reading, which claims less and is not wrong.</p>
 *
 * <p>A Spring Data repository declares no signature at all — everything it offers is inherited from
 * vendor code — so the pack answers for it, and the subject it captured is the one the rule already
 * knows.</p>
 *
 * @since 7.0.0
 */
public final class PortSignatures implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("W2-ROLE");

    PortSignatures() {
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
        return Set.of(Predicate.PORT_ROLE, Predicate.RELATION);
    }

    @Override
    public void apply(Derivation derivation) {
        for (TypeNode port : derivation.perimeter().types()) {
            if (derivation
                    .kindOf(port.id())
                    .filter(ArchKind.DRIVEN_PORT::equals)
                    .isPresent()) {
                read(derivation, port);
            }
        }
    }

    private void read(Derivation derivation, TypeNode port) {
        if (recognizedAsStorage(derivation, port.id())) {
            derivation.derive(PortRole.derived(port.id(), DrivenPortType.REPOSITORY, ID));
            return;
        }
        Optional<TypeId> subject = subjectOf(derivation, port);
        if (subject.isPresent()) {
            derivation.derive(PortRole.derived(port.id(), DrivenPortType.REPOSITORY, ID));
            derivation.derive(Relation.derived(port.id(), RelationKind.MANAGES, subject.orElseThrow(), ID));
            return;
        }
        derivation.derive(PortRole.derived(port.id(), roleOf(derivation, port), ID));
    }

    /**
     * Answers whether a pack already recognized the port as storage, which is the one case where
     * the signatures are not the author's to read.
     */
    private static boolean recognizedAsStorage(Derivation derivation, TypeId port) {
        return derivation.about(port, KnowledgeAssertion.class).stream()
                .anyMatch(assertion -> assertion.finding().fact() == KnowledgeFact.SPRING_DATA_REPOSITORY);
    }

    /**
     * Returns the single type of the perimeter the signatures converge on, when there is one.
     */
    private static Optional<TypeId> subjectOf(Derivation derivation, TypeNode port) {
        List<TypeId> taken = Signatures.taken(derivation, port);
        List<TypeId> converging = Signatures.returned(derivation, port).stream()
                .distinct()
                .filter(taken::contains)
                .toList();
        return converging.size() == 1 ? Optional.of(converging.get(0)) : Optional.empty();
    }

    /**
     * Reads the remaining two shapes: one-way methods carrying values as a publication, anything
     * else as a call to a service.
     */
    private static DrivenPortType roleOf(Derivation derivation, TypeNode port) {
        return announces(derivation, port) ? DrivenPortType.EVENT_PUBLISHER : DrivenPortType.GATEWAY;
    }

    private static boolean announces(Derivation derivation, TypeNode port) {
        List<Method> methods = port.methods();
        return !methods.isEmpty()
                && methods.stream()
                        .allMatch(method -> "void".equals(method.returnType().qualifiedName()))
                && methods.stream()
                        .flatMap(method -> method.parameters().stream())
                        .map(Parameter::type)
                        .flatMap(reference -> carried(reference, derivation))
                        .anyMatch(Shapes::isImmutable);
    }

    private static Stream<TypeNode> carried(TypeRef reference, Derivation derivation) {
        return Signatures.namedInPerimeter(derivation, Stream.of(reference)).stream()
                .flatMap(id -> derivation.code().type(id).stream());
    }
}
