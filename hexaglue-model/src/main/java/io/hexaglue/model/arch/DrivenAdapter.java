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
 * A driven adapter: the technical implementation — persistence, messaging, remote client — through
 * which the hexagon reaches the outside world.
 *
 * <p>The implemented ports are the ones the engine recognized as driven ports among the supertypes;
 * the remaining supertypes stay in the structure without being read as an architectural link.</p>
 *
 * @param id the stable type identity
 * @param structure the structural description
 * @param classification the complete verdict, kind DRIVEN_ADAPTER
 * @param implementedPorts the driven ports this adapter implements
 * @since 7.0.0
 */
public record DrivenAdapter(
        TypeId id, TypeStructure structure, Classification classification, List<TypeRef> implementedPorts)
        implements AdapterType {

    /**
     * Validates the kind coherence of the verdict and copies the implemented ports.
     */
    public DrivenAdapter {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(structure, "structure must not be null");
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(implementedPorts, "implementedPorts must not be null");
        KindCoherence.require(ArchKind.DRIVEN_ADAPTER, classification, id);
        implementedPorts = List.copyOf(implementedPorts);
    }

    @Override
    public ArchKind kind() {
        return ArchKind.DRIVEN_ADAPTER;
    }

    @Override
    public PortDirection direction() {
        return PortDirection.DRIVEN;
    }

    @Override
    public List<TypeRef> ports() {
        return implementedPorts;
    }
}
