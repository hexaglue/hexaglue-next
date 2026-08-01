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
 * A domain entity: identity and lifecycle, owned by an aggregate when the engine established the
 * composition.
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind ENTITY
 * @param identityField the field carrying the entity identity, when detected
 * @param owningAggregate the aggregate owning this entity, when established
 * @since 7.0.0
 */
public record Entity(
        TypeId id,
        TypeStructure structure,
        Classification classification,
        Optional<Field> identityField,
        Optional<TypeRef> owningAggregate)
        implements DomainType {

    /**
     * Validates the kind coherence of the verdict.
     */
    public Entity {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(identityField, "identityField must not be null");
        Objects.requireNonNull(owningAggregate, "owningAggregate must not be null");
        KindCoherence.require(ArchKind.ENTITY, classification, id);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.ENTITY;
    }

    /**
     * Returns whether an identity field was detected.
     *
     * @return true when the identity field is present
     */
    public boolean hasIdentity() {
        return identityField.isPresent();
    }

    /**
     * Returns whether an owning aggregate was established.
     *
     * @return true when the owning aggregate is present
     */
    public boolean hasOwningAggregate() {
        return owningAggregate.isPresent();
    }
}
