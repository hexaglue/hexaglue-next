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
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import java.util.Set;

/**
 * How the domain is read from the lifecycle around it: what a way out keeps alive, and what the
 * thing it keeps is made of.
 *
 * <p>Nothing in a domain declaration says what it is. A class with fields looks like every other
 * class with fields, so the rules of this wave read the domain from the outside in — a way out
 * whose whole trade is keeping one type and handing it back gives that type a life of its own, and
 * everything that type holds is then part of it. Several rules ask the same two questions along the
 * way, and asking them twice would let the answers drift, so both readings live here.</p>
 *
 * <p>Two things an owner keeps are deliberately not parts. A contract is nobody's part: an
 * aggregate holding a way out is a layering question for the report, and calling that contract a
 * value would erase the boundary instead of stating it. And a value written the way an identity is
 * written is left alone entirely — composition cannot tell the identity of the owner from a plain
 * value beside it, since both are fields, and settling that duel by position would be a guess. That
 * duel belongs to {@link LookupIdentity}, which reads the key a way out searches by.</p>
 *
 * @since 7.0.0
 */
final class Lifecycle {

    /** What both readings of this wave watch, declared once so the rules stay in step. */
    static final Set<Predicate> SOURCES = Set.of(Predicate.PORT_ROLE, Predicate.RELATION);

    /** A part is something instantiated and kept; an interface is a contract, never a part. */
    private static final Set<TypeNature> PARTS = Set.of(TypeNature.CLASS, TypeNature.RECORD, TypeNature.ENUM);

    private Lifecycle() {}

    /**
     * Returns the ties running from a way out that plies storage to the type it keeps.
     *
     * <p>The tie is enough on its own: it is only ever stated of a way out that stores, so asking
     * again what trade that way out plies would restate its own premise. A gateway naming a type in
     * its signatures never produces one, which is the whole point — asking a service about a thing
     * says nothing about how that thing lives.</p>
     *
     * @param derivation the derivation the rule is running under
     * @return the storage ties held so far, in subject then rendering order
     */
    static List<Relation> storageTies(Derivation derivation) {
        return derivation.all(Relation.class).stream()
                .filter(relation -> relation.kind() == RelationKind.MANAGES)
                .toList();
    }

    /**
     * Returns whether the previous round read the given type as something that owns parts.
     *
     * @param derivation the derivation the rule is running under
     * @param id the type to place
     * @return true when the type was read as an aggregate or an entity
     */
    static boolean owns(Derivation derivation, TypeId id) {
        return derivation
                .kindOf(id)
                .filter(kind -> kind == ArchKind.AGGREGATE_ROOT || kind == ArchKind.ENTITY)
                .isPresent();
    }

    /**
     * Returns the types the given owner is made of, in declaration order and without repetition.
     * A field holding many of a part composes just as much as a field holding one, so containers
     * are unwrapped.
     *
     * @param derivation the derivation the rule is running under
     * @param owner the declaration to read
     * @return the parts, in declaration order
     */
    static List<TypeId> partsOf(Derivation derivation, TypeNode owner) {
        return keptBy(owner).stream()
                .filter(part -> !part.equals(owner.id()))
                .filter(part -> isPart(derivation, part))
                .toList();
    }

    /**
     * States the composition the two readings of this wave share as a tie from owner to part.
     *
     * <p>Which kind a part reads as is one question, whose part it is another. A generator writing
     * a mapping needs the owner and not merely the kind, and recovering the owner from the shape a
     * second time would be a second definition of what counts as a part — the one thing this class
     * exists to prevent.</p>
     *
     * @param derivation the derivation the rule is running under
     * @param owner the declaration the part belongs to
     * @param part the type it is made of
     * @param rule the rule stating it, which the proof names
     */
    static void tie(Derivation derivation, TypeNode owner, TypeId part, RuleId rule) {
        derivation.derive(Relation.derived(owner.id(), RelationKind.OWNS, part, rule));
    }

    /**
     * Returns whether some aggregate or entity of the perimeter is made of the given type.
     *
     * <p>Composition and collaboration are the same field seen from two ends: an aggregate keeping
     * a type is made of it, an application service keeping one is calling it. What tells them apart
     * is which of the two ends is the aggregate, so any rule reading a holder as a caller has to
     * ask this before it speaks.</p>
     *
     * @param derivation the derivation the rule is running under
     * @param id the type to place
     * @return true when the type is part of something
     */
    static boolean isPartOfSomething(Derivation derivation, TypeId id) {
        return derivation.perimeter().types().stream()
                .filter(owner -> owns(derivation, owner.id()))
                .anyMatch(owner -> partsOf(derivation, owner).contains(id));
    }

    /**
     * Returns the types of the perimeter the given declaration keeps in its state, containers
     * unwrapped — parts and everything else it holds alike.
     *
     * @param owner the declaration to read
     * @return the kept types, in declaration order and without repetition
     */
    static List<TypeId> keptBy(TypeNode owner) {
        return Shapes.state(owner).stream()
                .map(field -> field.type().unwrapElement())
                .map(reference -> TypeId.of(reference.qualifiedName()))
                .distinct()
                .toList();
    }

    /**
     * Returns whether the given type keeps something the analysis has read as an identity, which
     * is what tells a part with a life of its own from a part that is only a value.
     *
     * @param derivation the derivation the rule is running under
     * @param part the type to read
     * @return true when one of its fields carries an identity
     */
    static boolean carriesIdentity(Derivation derivation, TypeId part) {
        return derivation.code().type(part).stream()
                .flatMap(node -> Shapes.state(node).stream())
                .map(Field::type)
                .map(reference -> TypeId.of(reference.qualifiedName()))
                .anyMatch(kept -> derivation
                        .kindOf(kept)
                        .filter(ArchKind.IDENTIFIER::equals)
                        .isPresent());
    }

    private static boolean isPart(Derivation derivation, TypeId part) {
        return derivation
                .code()
                .type(part)
                .filter(type -> PARTS.contains(type.nature()))
                .filter(type -> !Shapes.readsAsIdentity(type))
                .isPresent();
    }
}
