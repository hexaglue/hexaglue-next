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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.declaration.Field;
import java.util.Objects;
import java.util.Optional;

/**
 * A domain event: something that happened in the domain, linked to its source aggregate when the
 * engine established it.
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind DOMAIN_EVENT
 * @param aggregateIdField the field referencing the source aggregate identity, when detected
 * @param timestampField the field carrying the occurrence time, when detected
 * @param sourceAggregate the aggregate publishing this event, when established
 * @since 7.0.0
 */
public record DomainEvent(
        TypeId id,
        TypeStructure structure,
        Classification classification,
        Optional<Field> aggregateIdField,
        Optional<Field> timestampField,
        Optional<TypeRef> sourceAggregate)
        implements DomainType {

    /**
     * Validates the kind coherence of the verdict.
     */
    public DomainEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(aggregateIdField, "aggregateIdField must not be null");
        Objects.requireNonNull(timestampField, "timestampField must not be null");
        Objects.requireNonNull(sourceAggregate, "sourceAggregate must not be null");
        KindCoherence.require(ArchKind.DOMAIN_EVENT, classification, id);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DOMAIN_EVENT;
    }

    /**
     * Creates a domain event without derived links.
     *
     * @param id the stable type identity
     * @param structure the structural description
     * @param classification the complete verdict, kind DOMAIN_EVENT
     * @return a new DomainEvent
     */
    public static DomainEvent of(TypeId id, TypeStructure structure, Classification classification) {
        return new DomainEvent(id, structure, classification, Optional.empty(), Optional.empty(), Optional.empty());
    }
}
