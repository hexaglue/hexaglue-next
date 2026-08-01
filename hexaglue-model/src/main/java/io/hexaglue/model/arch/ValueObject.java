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
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.declaration.Field;
import java.util.Objects;
import java.util.Optional;

/**
 * A value object: immutable, identified by its attributes.
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind VALUE_OBJECT
 * @since 7.0.0
 */
public record ValueObject(TypeId id, TypeStructure structure, Classification classification) implements DomainType {

    /**
     * Validates the kind coherence of the verdict.
     */
    public ValueObject {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        KindCoherence.require(ArchKind.VALUE_OBJECT, classification, id);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.VALUE_OBJECT;
    }

    /**
     * Returns whether this value object wraps a single value.
     *
     * @return true when the structure declares exactly one field
     */
    public boolean isSingleValue() {
        return structure.fields().size() == 1;
    }

    /**
     * Returns the single wrapped field, when this value object wraps exactly one value.
     *
     * @return the wrapped field, or empty for multi-field value objects
     */
    public Optional<Field> wrappedField() {
        return isSingleValue() ? Optional.of(structure.fields().get(0)) : Optional.empty();
    }
}
