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
import io.hexaglue.model.declaration.Method;
import java.util.List;
import java.util.Objects;

/**
 * A domain service: stateless domain behaviour operating on several domain types.
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind DOMAIN_SERVICE
 * @param injectedPorts the driven ports this service depends on, in declaration order
 * @param operations the domain operations this service exposes, in declaration order
 * @since 7.0.0
 */
public record DomainService(
        TypeId id,
        TypeStructure structure,
        Classification classification,
        List<TypeRef> injectedPorts,
        List<Method> operations)
        implements DomainType {

    /**
     * Validates the kind coherence of the verdict and copies the lists.
     */
    public DomainService {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(injectedPorts, "injectedPorts must not be null");
        Objects.requireNonNull(operations, "operations must not be null");
        KindCoherence.require(ArchKind.DOMAIN_SERVICE, classification, id);
        injectedPorts = List.copyOf(injectedPorts);
        operations = List.copyOf(operations);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DOMAIN_SERVICE;
    }

    /**
     * Returns whether this service depends on driven ports.
     *
     * @return true when at least one port is injected
     */
    public boolean hasInjectedPorts() {
        return !injectedPorts.isEmpty();
    }
}
