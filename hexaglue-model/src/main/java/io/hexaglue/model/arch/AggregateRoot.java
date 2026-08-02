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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An aggregate root, enriched with the composition the engine derived: its identity, the entities
 * and value objects it owns, the events it publishes, the driven port that persists it and the
 * invariants it protects. Plugins read this — they never re-derive it.
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind AGGREGATE_ROOT
 * @param identityField the field carrying the aggregate identity, when the engine could name it
 * @param effectiveIdentityType the unwrapped identity type (e.g. {@code UUID} behind
 *     {@code OrderId})
 * @param entities the owned entities, in discovery order
 * @param valueObjects the owned value objects, in discovery order
 * @param domainEvents the events this aggregate publishes, in discovery order
 * @param drivenPort the driven port managing this aggregate, when one exists
 * @param invariants the invariants detected on this aggregate, in discovery order
 * @since 7.0.0
 */
public record AggregateRoot(
        TypeId id,
        TypeStructure structure,
        Classification classification,
        Optional<Field> identityField,
        Optional<TypeRef> effectiveIdentityType,
        List<TypeRef> entities,
        List<TypeRef> valueObjects,
        List<TypeRef> domainEvents,
        Optional<TypeRef> drivenPort,
        List<Invariant> invariants)
        implements DomainType {

    /**
     * Validates the kind coherence of the verdict and copies the composition lists.
     */
    public AggregateRoot {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(identityField, "identityField must not be null");
        Objects.requireNonNull(effectiveIdentityType, "effectiveIdentityType must not be null");
        Objects.requireNonNull(entities, "entities must not be null");
        Objects.requireNonNull(valueObjects, "valueObjects must not be null");
        Objects.requireNonNull(domainEvents, "domainEvents must not be null");
        Objects.requireNonNull(drivenPort, "drivenPort must not be null");
        Objects.requireNonNull(invariants, "invariants must not be null");
        KindCoherence.require(ArchKind.AGGREGATE_ROOT, classification, id);
        entities = List.copyOf(entities);
        valueObjects = List.copyOf(valueObjects);
        domainEvents = List.copyOf(domainEvents);
        invariants = List.copyOf(invariants);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.AGGREGATE_ROOT;
    }

    /**
     * Returns whether this aggregate owns entities or value objects.
     *
     * @return true when the composition is not empty
     */
    public boolean hasComposition() {
        return !entities.isEmpty() || !valueObjects.isEmpty();
    }

    /**
     * Returns whether the field carrying the identity was named.
     *
     * @return true when the identity field is present
     */
    public boolean hasIdentity() {
        return identityField.isPresent();
    }
}
