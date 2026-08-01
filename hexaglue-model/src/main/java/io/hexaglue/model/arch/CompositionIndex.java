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

package io.hexaglue.model.arch;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.declaration.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Navigation over the compositional facts of an {@link ArchModel}: what an aggregate embeds,
 * which identifier belongs to which aggregate, and the cross-aggregate references a type holds
 * through identifier-typed fields. Everything here reads decided facts — the composition recorded
 * on the aggregates and the declared fields — no inference happens at this level.
 *
 * <p>A field counts as a cross-aggregate reference when its type (or the element type of a
 * collection) is the identifier of another aggregate; an aggregate holding its own identity and an
 * entity holding its owning aggregate's identifier are not references. Streams follow the model's
 * identity order, fields their declaration order.</p>
 *
 * @since 7.0.0
 */
public final class CompositionIndex {

    private final Map<TypeId, ArchType> typesById;
    private final SortedMap<TypeId, TypeId> aggregateByIdentifier;

    CompositionIndex(Map<TypeId, ArchType> typesById) {
        this.typesById = typesById;
        SortedMap<TypeId, TypeId> byIdentifier = new TreeMap<>();
        typesById.values().stream()
                .filter(AggregateRoot.class::isInstance)
                .map(AggregateRoot.class::cast)
                .forEach(aggregate -> byIdentifier.putIfAbsent(identifierTypeOf(aggregate), aggregate.id()));
        this.aggregateByIdentifier = Collections.unmodifiableSortedMap(byIdentifier);
    }

    private static TypeId identifierTypeOf(AggregateRoot aggregate) {
        return TypeId.of(aggregate.identityField().type().qualifiedName());
    }

    /**
     * Returns the types embedded by the given aggregate: its entities, then its value objects, in
     * composition order.
     *
     * @param ownerId the owner type id
     * @return the stream of embedded type ids, empty when the id is not an aggregate root
     */
    public Stream<TypeId> embeddedBy(TypeId ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        return aggregate(ownerId)
                .map(owner -> Stream.concat(owner.entities().stream(), owner.valueObjects().stream())
                        .map(ref -> TypeId.of(ref.qualifiedName())))
                .orElseGet(Stream::empty);
    }

    /**
     * Returns the aggregates embedding the given type, in identity order.
     *
     * @param embeddedId the embedded type id
     * @return the stream of owner type ids, possibly empty
     */
    public Stream<TypeId> embeddedIn(TypeId embeddedId) {
        Objects.requireNonNull(embeddedId, "embeddedId must not be null");
        return typesById.keySet().stream().filter(ownerId -> embeddedBy(ownerId).anyMatch(embeddedId::equals));
    }

    /**
     * Returns whether the given type embeds anything.
     *
     * @param typeId the type id
     * @return true when the type is an aggregate root with a non-empty composition
     */
    public boolean hasCompositions(TypeId typeId) {
        Objects.requireNonNull(typeId, "typeId must not be null");
        return embeddedBy(typeId).findAny().isPresent();
    }

    /**
     * Returns the identifier type of the given aggregate, read from its identity field.
     *
     * @param aggregateId the aggregate type id
     * @return the identifier type id, or empty when the id is not an aggregate root
     */
    public Optional<TypeId> identifierOf(TypeId aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return aggregate(aggregateId).map(CompositionIndex::identifierTypeOf);
    }

    /**
     * Returns the aggregate whose identity is the given identifier type.
     *
     * @param identifierId the identifier type id
     * @return the owning aggregate type id, or empty when no aggregate identifies with it
     */
    public Optional<TypeId> aggregateOf(TypeId identifierId) {
        Objects.requireNonNull(identifierId, "identifierId must not be null");
        return Optional.ofNullable(aggregateByIdentifier.get(identifierId));
    }

    /**
     * Returns the cross-aggregate references held by the given type, in field declaration order.
     *
     * @param sourceId the source type id
     * @return the stream of references, empty when the type is unknown or holds none
     */
    public Stream<AggregateReference> referencesFrom(TypeId sourceId) {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        ArchType source = typesById.get(sourceId);
        if (source == null) {
            return Stream.empty();
        }
        List<AggregateReference> references = new ArrayList<>();
        for (Field field : source.structure().fields()) {
            for (TypeId candidate : identifierCandidates(field)) {
                aggregateOf(candidate)
                        .filter(aggregateId -> isCrossReference(source, aggregateId))
                        .ifPresent(aggregateId ->
                                references.add(new AggregateReference(sourceId, candidate, aggregateId)));
            }
        }
        return references.stream();
    }

    /**
     * Returns the types referencing the given aggregate through its identifier, in identity order.
     *
     * @param aggregateId the referenced aggregate type id
     * @return the stream of referencing type ids, possibly empty
     */
    public Stream<TypeId> referencedBy(TypeId aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return typesById.keySet().stream()
                .flatMap(this::referencesFrom)
                .filter(reference -> reference.aggregateType().equals(aggregateId))
                .map(AggregateReference::sourceType);
    }

    private Optional<AggregateRoot> aggregate(TypeId id) {
        return Optional.ofNullable(typesById.get(id))
                .filter(AggregateRoot.class::isInstance)
                .map(AggregateRoot.class::cast);
    }

    private static List<TypeId> identifierCandidates(Field field) {
        return Stream.concat(Stream.of(field.type()), field.elementType().stream())
                .filter(TypeRef.Named.class::isInstance)
                .map(ref -> TypeId.of(ref.qualifiedName()))
                .toList();
    }

    private static boolean isCrossReference(ArchType source, TypeId aggregateId) {
        return !aggregateId.equals(source.id()) && !referencesOwnAggregate(source, aggregateId);
    }

    private static boolean referencesOwnAggregate(ArchType source, TypeId aggregateId) {
        return source instanceof Entity entity
                && entity.owningAggregate()
                        .map(owner -> TypeId.of(owner.qualifiedName()).equals(aggregateId))
                        .orElse(false);
    }

    /**
     * A cross-aggregate reference: a type pointing at another aggregate through that aggregate's
     * identifier.
     *
     * @param sourceType the type holding the reference
     * @param identifierType the identifier type carrying the reference
     * @param aggregateType the referenced aggregate
     * @since 7.0.0
     */
    public record AggregateReference(TypeId sourceType, TypeId identifierType, TypeId aggregateType) {

        /**
         * Validates the reference.
         */
        public AggregateReference {
            Objects.requireNonNull(sourceType, "sourceType must not be null");
            Objects.requireNonNull(identifierType, "identifierType must not be null");
            Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        }
    }
}
