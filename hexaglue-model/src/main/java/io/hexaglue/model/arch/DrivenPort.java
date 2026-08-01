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
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Classification;
import java.util.Objects;
import java.util.Optional;

/**
 * A driven port: the interface through which the hexagon drives the outside world, typed by its
 * functional family and linked to the aggregate it manages when the engine established it.
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind DRIVEN_PORT
 * @param portType the functional family of the port
 * @param managedAggregate the aggregate this port manages, when established
 * @since 7.0.0
 */
public record DrivenPort(
        TypeId id,
        TypeStructure structure,
        Classification classification,
        DrivenPortType portType,
        Optional<TypeRef> managedAggregate)
        implements PortType {

    /**
     * Validates the kind coherence of the verdict.
     */
    public DrivenPort {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(portType, "portType must not be null");
        Objects.requireNonNull(managedAggregate, "managedAggregate must not be null");
        KindCoherence.require(ArchKind.DRIVEN_PORT, classification, id);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DRIVEN_PORT;
    }

    @Override
    public PortDirection direction() {
        return PortDirection.DRIVEN;
    }

    /**
     * Returns whether this port is a repository.
     *
     * @return true for the REPOSITORY family
     */
    public boolean isRepository() {
        return portType == DrivenPortType.REPOSITORY;
    }

    /**
     * Returns whether this port is a gateway.
     *
     * @return true for the GATEWAY family
     */
    public boolean isGateway() {
        return portType == DrivenPortType.GATEWAY;
    }
}
