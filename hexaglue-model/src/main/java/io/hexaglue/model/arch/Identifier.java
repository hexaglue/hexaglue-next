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
import java.util.Objects;
import java.util.Optional;

/**
 * An identifier: a value object wrapping the identity of an entity or aggregate
 * (e.g. {@code OrderId} wrapping {@code UUID}).
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind IDENTIFIER
 * @param wrappedType the underlying identity type, when the identifier wraps a single value
 * @since 7.0.0
 */
public record Identifier(
        TypeId id, TypeStructure structure, Classification classification, Optional<TypeRef> wrappedType)
        implements DomainType {

    /**
     * Validates the kind coherence of the verdict.
     */
    public Identifier {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(wrappedType, "wrappedType must not be null");
        KindCoherence.require(ArchKind.IDENTIFIER, classification, id);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.IDENTIFIER;
    }

    /**
     * Returns whether this identifier wraps a single underlying value. A composite key, or one
     * whose underlying type the engine could not see, wraps none.
     *
     * @return true when the wrapped type is present
     */
    public boolean wrapsSingleValue() {
        return wrappedType.isPresent();
    }
}
