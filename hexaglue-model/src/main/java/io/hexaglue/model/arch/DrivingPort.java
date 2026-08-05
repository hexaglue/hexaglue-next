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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A driving port: the interface through which the outside world drives the hexagon, with the use
 * cases it exposes.
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind DRIVING_PORT
 * @param useCases the use cases exposed by this port, in declaration order
 * @param inputTypes the domain types accepted by the port, in discovery order
 * @param outputTypes the domain types produced by the port, in discovery order
 * @param subject the aggregate this port's use cases are about, when the engine established one
 * @since 7.0.0
 */
public record DrivingPort(
        TypeId id,
        TypeStructure structure,
        Classification classification,
        List<UseCase> useCases,
        List<TypeRef> inputTypes,
        List<TypeRef> outputTypes,
        Optional<TypeRef> subject)
        implements PortType {

    /**
     * Validates the kind coherence of the verdict and copies the lists.
     */
    public DrivingPort {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(useCases, "useCases must not be null");
        Objects.requireNonNull(inputTypes, "inputTypes must not be null");
        Objects.requireNonNull(outputTypes, "outputTypes must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        KindCoherence.require(ArchKind.DRIVING_PORT, classification, id);
        useCases = List.copyOf(useCases);
        inputTypes = List.copyOf(inputTypes);
        outputTypes = List.copyOf(outputTypes);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DRIVING_PORT;
    }

    @Override
    public PortDirection direction() {
        return PortDirection.DRIVING;
    }
}
