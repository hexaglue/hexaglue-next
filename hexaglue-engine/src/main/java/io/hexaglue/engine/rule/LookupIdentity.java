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
 * <p>Only the methods that retrieve <em>at most one</em> aggregate — the aggregate itself, or an
 * optional of it — say how one is found again. A method answering with several is a search among
 * them, and being searched by a value does not make that value an identity: a way out that lists
 * orders by customer says something about customers, not about what an order is.</p>
 *
 * <p>Exactly one key must remain. When several do, a key the round has already concluded to be the
 * identity of <em>another</em> aggregate stands aside — one value does not identify two things when
 * anything else can carry the identity of the second. That reading only ever narrows a tie, never
 * vetoes a lone key: under saturation a conclusion may only grow, and a veto arriving late would
 * have to take back an election already made. Past both readings, what is left is silence; the
 * report is a better place for an aggregate searched two ways than the model. The reading is also
 * stated as a tie between the two types, because a generator writing a repository needs to know
 * which type carries the identity, not merely that one does.</p>
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
                .flatMap(node -> lookupKeysOf(derivation, node, aggregate))
                .distinct()
                .filter(kept::contains)
                .toList();
        List<TypeId> elected = withoutKeysSpokenFor(derivation, aggregate, keys);
        if (elected.size() != 1) {
            return;
        }
        TypeId identity = elected.get(0);
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
     * Returns the types of the perimeter the way out is handed by the methods that retrieve at
     * most one aggregate. A method answering with several is a search among them, and one that
     * returns something else is not a search for the aggregate at all.
     */
    private static Stream<TypeId> lookupKeysOf(Derivation derivation, TypeNode port, TypeId aggregate) {
        return port.methods().stream()
                .filter(method -> retrievesAtMostOne(method, aggregate))
                .flatMap(method ->
                        Signatures.namedInPerimeter(
                                derivation, method.parameters().stream().map(Parameter::type))
                                .stream());
    }

    private static boolean retrievesAtMostOne(Method method, TypeId aggregate) {
        TypeRef answer = method.returnType();
        if (aggregate.qualifiedName().equals(answer.qualifiedName())) {
            return true;
        }
        return answer.isOptionalLike()
                && aggregate.qualifiedName().equals(answer.unwrapElement().qualifiedName());
    }

    /**
     * Drops, among several candidates, the keys the round has already concluded to identify
     * another aggregate. Never applied to a lone key — a way out that only ever retrieves this
     * aggregate by that value has said that this is how it is found, and a tiebreak that could
     * empty an election would be a conclusion able to shrink under saturation.
     */
    private static List<TypeId> withoutKeysSpokenFor(Derivation derivation, TypeId aggregate, List<TypeId> keys) {
        if (keys.size() < 2) {
            return keys;
        }
        return keys.stream()
                .filter(key -> !identifiesAnother(derivation, aggregate, key))
                .toList();
    }

    private static boolean identifiesAnother(Derivation derivation, TypeId aggregate, TypeId key) {
        return derivation.all(Relation.class).stream()
                .anyMatch(relation -> relation.kind() == RelationKind.IDENTIFIED_BY
                        && relation.object().equals(key)
                        && !relation.subject().equals(aggregate));
    }
}
