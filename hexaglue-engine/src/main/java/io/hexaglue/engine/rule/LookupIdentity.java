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
import io.hexaglue.engine.Relation;
import io.hexaglue.engine.RelationKind;
import io.hexaglue.engine.Rule;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads the key a way out searches an aggregate by as the identity of that aggregate.
 *
 * <p>This is what settles the duel the shape of a declaration cannot. {@code FleetTag} and
 * {@code Email} are written identically — a record around one value — so reading either of them
 * alone yields both an identity and a value, and picking one would be a guess. Asking who searches
 * by it turns the question into a relational one: the aggregate keeps the value, and the way out
 * that hands the aggregate back takes that same value to find it. Nothing else in a codebase does
 * that with a plain attribute.</p>
 *
 * <p>Exactly one key must answer. Two of them means the aggregate is searched by two things, and
 * electing one would be a coin flip; the report is a better place for that question than the model.
 * The reading is also stated as a tie between the two types, because a generator writing a
 * repository needs to know which type carries the identity, not merely that one does.</p>
 *
 * @since 7.0.0
 */
public final class LookupIdentity implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R2");

    LookupIdentity() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads the key a way out searches an aggregate by as the identity of that aggregate";
    }

    @Override
    public Set<Predicate> reads() {
        return Lifecycle.SOURCES;
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE, Predicate.RELATION);
    }

    @Override
    public void apply(Derivation derivation) {
        for (Relation tie : Lifecycle.storageTies(derivation)) {
            read(derivation, tie.subject(), tie.object());
        }
    }

    private void read(Derivation derivation, TypeId port, TypeId aggregate) {
        if (!Lifecycle.owns(derivation, aggregate)) {
            return;
        }
        List<TypeId> kept = identitiesKeptBy(derivation, aggregate);
        List<TypeId> keys = derivation.code().type(port).stream()
                .flatMap(node -> searchKeysOf(derivation, node, aggregate))
                .distinct()
                .filter(kept::contains)
                .toList();
        if (keys.size() != 1) {
            return;
        }
        TypeId identity = keys.get(0);
        Contracts.speak(
                derivation,
                identity,
                ArchKind.IDENTIFIER,
                "SEARCHED_BY(" + port.qualifiedName() + ")",
                port.simpleName() + " takes it to answer with " + aggregate.simpleName() + ", which keeps it",
                List.of(aggregate, port),
                ID);
        derivation.derive(Relation.derived(aggregate, RelationKind.IDENTIFIED_BY, identity, ID));
    }

    /**
     * Returns the values the aggregate keeps that are written the way an identity is written. A
     * way out may well search by something else it holds — a name, a date — and being searched by
     * does not turn an attribute into an identity.
     */
    private static List<TypeId> identitiesKeptBy(Derivation derivation, TypeId aggregate) {
        return derivation.code().type(aggregate).stream()
                .flatMap(node -> Shapes.state(node).stream())
                .map(Field::type)
                .map(reference -> TypeId.of(reference.qualifiedName()))
                .distinct()
                .filter(kept -> derivation
                        .code()
                        .type(kept)
                        .filter(Shapes::readsAsIdentity)
                        .isPresent())
                .toList();
    }

    /**
     * Returns the types of the perimeter the way out is handed by the methods that answer with the
     * aggregate. A method that returns something else is not a search for it.
     */
    private static Stream<TypeId> searchKeysOf(Derivation derivation, TypeNode port, TypeId aggregate) {
        return port.methods().stream()
                .filter(method -> answersWith(method, aggregate))
                .flatMap(method ->
                        Signatures.namedInPerimeter(
                                derivation, method.parameters().stream().map(Parameter::type))
                                .stream());
    }

    private static boolean answersWith(Method method, TypeId aggregate) {
        TypeRef answered = method.returnType().unwrapElement();
        return aggregate.qualifiedName().equals(answered.qualifiedName());
    }
}
