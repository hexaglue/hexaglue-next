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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Typed access to the domain side of an {@link ArchModel}: aggregate roots, entities, value
 * objects, identifiers, events, domain services, and the resolution of an aggregate's composition
 * to the classified types it owns. Streams follow the model's identity order.
 *
 * @since 7.0.0
 */
public final class DomainIndex {

    private final Map<TypeId, ArchType> typesById;

    DomainIndex(Map<TypeId, ArchType> typesById) {
        this.typesById = typesById;
    }

    /**
     * Returns every aggregate root, in identity order.
     *
     * @return the stream of aggregate roots
     */
    public Stream<AggregateRoot> aggregateRoots() {
        return all(AggregateRoot.class);
    }

    /**
     * Returns every entity, in identity order.
     *
     * @return the stream of entities
     */
    public Stream<Entity> entities() {
        return all(Entity.class);
    }

    /**
     * Returns every value object, in identity order.
     *
     * @return the stream of value objects
     */
    public Stream<ValueObject> valueObjects() {
        return all(ValueObject.class);
    }

    /**
     * Returns every identifier, in identity order.
     *
     * @return the stream of identifiers
     */
    public Stream<Identifier> identifiers() {
        return all(Identifier.class);
    }

    /**
     * Returns every domain event, in identity order.
     *
     * @return the stream of domain events
     */
    public Stream<DomainEvent> domainEvents() {
        return all(DomainEvent.class);
    }

    /**
     * Returns every domain service, in identity order.
     *
     * @return the stream of domain services
     */
    public Stream<DomainService> domainServices() {
        return all(DomainService.class);
    }

    /**
     * Returns the aggregate root with the given id.
     *
     * @param id the type id
     * @return the aggregate root, or empty when the id is unknown or classified otherwise
     */
    public Optional<AggregateRoot> aggregateRoot(TypeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(typesById.get(id))
                .filter(AggregateRoot.class::isInstance)
                .map(AggregateRoot.class::cast);
    }

    /**
     * Resolves the entities owned by the given aggregate to their classified types. A composition
     * reference absent from the model is skipped.
     *
     * @param aggregate the aggregate root
     * @return the immutable list of owned entities, in composition order
     */
    public List<Entity> entitiesOf(AggregateRoot aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        return resolve(aggregate.entities(), Entity.class);
    }

    /**
     * Resolves the value objects owned by the given aggregate to their classified types. A
     * composition reference absent from the model is skipped.
     *
     * @param aggregate the aggregate root
     * @return the immutable list of owned value objects, in composition order
     */
    public List<ValueObject> valueObjectsOf(AggregateRoot aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        return resolve(aggregate.valueObjects(), ValueObject.class);
    }

    private <T extends ArchType> Stream<T> all(Class<T> type) {
        return typesById.values().stream().filter(type::isInstance).map(type::cast);
    }

    private <T extends ArchType> List<T> resolve(List<TypeRef> refs, Class<T> type) {
        return refs.stream()
                .map(ref -> Optional.ofNullable(typesById.get(TypeId.of(ref.qualifiedName()))))
                .flatMap(Optional::stream)
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }
}
