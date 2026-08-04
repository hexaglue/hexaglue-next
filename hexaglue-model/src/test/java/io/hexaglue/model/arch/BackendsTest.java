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

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.ProofNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a build states its backends will write, and which holes that leaves reportable.
 */
class BackendsTest {

    private static DrivenPort drivenPort(String name, DrivenPortType role) {
        return new DrivenPort(
                TypeId.of("com.shop." + name),
                TypeStructure.builder(TypeNature.INTERFACE).build(),
                verdict(ArchKind.DRIVEN_PORT, PortDirection.DRIVEN),
                role,
                Optional.empty());
    }

    private static DrivingPort drivingPort(String name) {
        return new DrivingPort(
                TypeId.of("com.shop." + name),
                TypeStructure.builder(TypeNature.INTERFACE).build(),
                verdict(ArchKind.DRIVING_PORT, PortDirection.DRIVING),
                List.of(),
                List.of(),
                List.of());
    }

    private static Classification verdict(ArchKind kind, PortDirection direction) {
        return Classification.builder(kind, Confidence.HIGH, Basis.INFERRED, ProofNode.fact(kind + " by fixture"))
                .direction(direction)
                .build();
    }

    @Test
    @DisplayName("a build with no backend fills nothing")
    void aBuildWithNoBackendFillsNothing() {
        assertThat(Backends.none().covering(drivenPort("Orders", DrivenPortType.REPOSITORY)))
                .isEmpty();
    }

    @Test
    @DisplayName("a backend fills the family it declared, and only that one")
    void fillsTheFamilyItDeclared() {
        Backends backends =
                new Backends(Map.of("io.hexaglue.jpa", Set.of(PortFamily.driven(DrivenPortType.REPOSITORY))));

        assertThat(backends.covering(drivenPort("Orders", DrivenPortType.REPOSITORY)))
                .containsExactly("io.hexaglue.jpa");
        assertThat(backends.covering(drivenPort("Mailer", DrivenPortType.GATEWAY)))
                .isEmpty();
        assertThat(backends.covering(drivingPort("PlaceOrder"))).isEmpty();
    }

    @Test
    @DisplayName("and a backend filling the way in fills every driving port, which has no families")
    void fillsEveryDrivingPort() {
        Backends backends = new Backends(Map.of("io.hexaglue.rest", Set.of(PortFamily.driving())));

        assertThat(backends.covering(drivingPort("PlaceOrder"))).containsExactly("io.hexaglue.rest");
        assertThat(backends.covering(drivenPort("Orders", DrivenPortType.REPOSITORY)))
                .isEmpty();
    }

    @Test
    @DisplayName("naming every backend that fills the same hole, in an order a message can be diffed on")
    void namingEveryBackendThatFillsIt() {
        Backends backends = new Backends(Map.of(
                "io.hexaglue.zeta", Set.of(PortFamily.driven(DrivenPortType.REPOSITORY)),
                "io.hexaglue.alpha", Set.of(PortFamily.driven(DrivenPortType.REPOSITORY), PortFamily.driving())));

        assertThat(backends.covering(drivenPort("Orders", DrivenPortType.REPOSITORY)))
                .containsExactly("io.hexaglue.alpha", "io.hexaglue.zeta");
    }

    @Test
    @DisplayName("and saying how each family reads, so a diagnostic can name it")
    void sayingHowEachFamilyReads() {
        assertThat(PortFamily.driven(DrivenPortType.REPOSITORY).toDisplayString())
                .isEqualTo("driven ports of role REPOSITORY");
        assertThat(PortFamily.driving().toDisplayString()).isEqualTo("driving ports");
    }
}
