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

package io.hexaglue.engine;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.DomainEvent;
import io.hexaglue.model.arch.DomainService;
import io.hexaglue.model.arch.DomainType;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The domain records, filled from the lifecycle the rules read around each type.
 *
 * <p>Everything here comes from a tie somebody stated: a way out keeps the aggregate, the aggregate
 * is searched by an identity, an owner is made of its parts, an aggregate answers with an event.
 * Nothing invents a link the analysis did not reach — an aggregate no way out keeps has no identity
 * named, and says so with an empty value rather than a guess.</p>
 */
final class DomainAssembly {

    private final Links links;

    DomainAssembly(Links links) {
        this.links = Objects.requireNonNull(links);
    }

    /**
     * Builds the record the domain kind calls for.
     *
     * @param type the analyzed declaration
     * @param structure its structure as the model holds it
     * @param verdict the verdict reached on it, always a domain kind here
     * @return the domain record
     */
    DomainType of(TypeNode type, TypeStructure structure, Classification verdict) {
        return switch (verdict.kind()) {
            case AGGREGATE_ROOT -> aggregate(type, structure, verdict);
            case ENTITY -> entity(type, structure, verdict);
            case VALUE_OBJECT -> new ValueObject(type.id(), structure, verdict);
            case IDENTIFIER ->
                new Identifier(type.id(), structure, verdict, Structures.wrappedValueOf(Optional.of(type)));
            case DOMAIN_EVENT -> event(type, structure, verdict);
            default -> new DomainService(type.id(), structure, verdict, waysOutHandedTo(type), type.methods());
        };
    }

    /**
     * The effective identity is the value the aggregate is really stored under: an identity written
     * around one thing is that thing, and one written around anything else names no single value,
     * which is what the empty answer says.
     */
    private AggregateRoot aggregate(TypeNode type, TypeStructure structure, Classification verdict) {
        Optional<TypeId> identity = Links.single(links.objects(RelationKind.IDENTIFIED_BY, type.id()));
        return new AggregateRoot(
                type.id(),
                structure,
                verdict,
                identity.flatMap(carrier -> fieldHolding(type, carrier)),
                identity.flatMap(
                        carrier -> Structures.wrappedValueOf(links.code().type(carrier))),
                partsOf(type.id(), ArchKind.ENTITY),
                partsOf(type.id(), ArchKind.VALUE_OBJECT),
                Links.references(links.objects(RelationKind.ANNOUNCES, type.id())),
                Links.single(links.subjects(RelationKind.MANAGES, type.id())).map(Links::reference),
                List.of());
    }

    private Entity entity(TypeNode type, TypeStructure structure, Classification verdict) {
        return new Entity(
                type.id(),
                structure,
                verdict,
                fieldCarryingAnIdentity(type),
                Links.single(links.subjects(RelationKind.OWNS, type.id())
                                .filter(owner -> links.is(owner, ArchKind.AGGREGATE_ROOT)))
                        .map(Links::reference));
    }

    /**
     * An event states what happened and to what; the moment it happened is written into a field
     * nothing in the analysis can name yet, so it is left unnamed rather than guessed from a type.
     */
    private DomainEvent event(TypeNode type, TypeStructure structure, Classification verdict) {
        return new DomainEvent(
                type.id(),
                structure,
                verdict,
                fieldCarryingAnIdentity(type),
                Optional.empty(),
                Links.single(links.subjects(RelationKind.ANNOUNCES, type.id())).map(Links::reference));
    }

    /**
     * Returns the parts of an owner the verdicts read as the given kind. Which types are parts was
     * settled by the composition rules and is read back from the ties they stated; which kind each
     * part is was settled by the same round of verdicts.
     */
    private List<TypeRef> partsOf(TypeId owner, ArchKind kind) {
        return Links.references(links.objects(RelationKind.OWNS, owner).filter(part -> links.is(part, kind)));
    }

    private List<TypeRef> waysOutHandedTo(TypeNode type) {
        return Links.references(links.heldBy(type).filter(links::isAPort));
    }

    private Optional<Field> fieldCarryingAnIdentity(TypeNode type) {
        return Structures.state(type).stream()
                .filter(field -> links.is(TypeId.of(field.type().qualifiedName()), ArchKind.IDENTIFIER))
                .findFirst();
    }

    private static Optional<Field> fieldHolding(TypeNode type, TypeId carrier) {
        return Structures.state(type).stream()
                .filter(field -> carrier.qualifiedName()
                        .equals(field.type().unwrapElement().qualifiedName()))
                .findFirst();
    }
}
