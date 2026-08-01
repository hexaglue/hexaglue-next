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

import io.hexaglue.model.TypeId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Typed access to the ports of an {@link ArchModel}: driving and driven ports, the usual driven
 * specializations (repositories, gateways), and the link from an aggregate to the repository that
 * manages it. Streams follow the model's identity order.
 *
 * @since 7.0.0
 */
public final class PortIndex {

    private final Map<TypeId, ArchType> typesById;

    PortIndex(Map<TypeId, ArchType> typesById) {
        this.typesById = typesById;
    }

    /**
     * Returns every driving port, in identity order.
     *
     * @return the stream of driving ports
     */
    public Stream<DrivingPort> drivingPorts() {
        return all(DrivingPort.class);
    }

    /**
     * Returns every driven port, in identity order.
     *
     * @return the stream of driven ports
     */
    public Stream<DrivenPort> drivenPorts() {
        return all(DrivenPort.class);
    }

    /**
     * Returns every driven port of type repository, in identity order.
     *
     * @return the stream of repositories
     */
    public Stream<DrivenPort> repositories() {
        return drivenPorts().filter(DrivenPort::isRepository);
    }

    /**
     * Returns every driven port of type gateway, in identity order.
     *
     * @return the stream of gateways
     */
    public Stream<DrivenPort> gateways() {
        return drivenPorts().filter(DrivenPort::isGateway);
    }

    /**
     * Returns the repository whose managed aggregate is the given one.
     *
     * @param aggregateId the aggregate type id
     * @return the managing repository, or empty when no repository manages the aggregate
     */
    public Optional<DrivenPort> repositoryFor(TypeId aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return repositories()
                .filter(repository -> repository
                        .managedAggregate()
                        .map(ref -> ref.qualifiedName().equals(aggregateId.qualifiedName()))
                        .orElse(false))
                .findFirst();
    }

    private <T extends ArchType> Stream<T> all(Class<T> type) {
        return typesById.values().stream().filter(type::isInstance).map(type::cast);
    }
}
