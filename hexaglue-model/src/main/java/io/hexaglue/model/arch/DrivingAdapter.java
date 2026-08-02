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

/**
 * A driving adapter: an entry point — web controller, message listener, scheduler, command line —
 * through which the outside world reaches the hexagon.
 *
 * <p>The ports it calls into are a conclusion of the engine, not a syntactic fact: a driving adapter
 * invokes or receives them by injection rather than declaring them in its supertypes.</p>
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind DRIVING_ADAPTER
 * @param drivingPorts the driving ports this adapter calls into
 * @since 7.0.0
 */
public record DrivingAdapter(
        TypeId id, TypeStructure structure, Classification classification, List<TypeRef> drivingPorts)
        implements AdapterType {

    /**
     * Validates the kind coherence of the verdict and copies the wired ports.
     */
    public DrivingAdapter {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(drivingPorts, "drivingPorts must not be null");
        KindCoherence.require(ArchKind.DRIVING_ADAPTER, classification, id);
        drivingPorts = List.copyOf(drivingPorts);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DRIVING_ADAPTER;
    }

    @Override
    public PortDirection direction() {
        return PortDirection.DRIVING;
    }

    @Override
    public List<TypeRef> ports() {
        return drivingPorts;
    }
}
