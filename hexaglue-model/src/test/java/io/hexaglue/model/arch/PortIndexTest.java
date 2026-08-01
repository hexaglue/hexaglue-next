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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PortIndexTest {

    private final PortIndex ports = ShopModelFixtures.shopModel().portIndex();

    @Nested
    @DisplayName("Streams by direction")
    class StreamsByDirection {

        @Test
        @DisplayName("driving ports are streamed")
        void drivingPortsAreStreamed() {
            assertThat(ports.drivingPorts())
                    .extracting(DrivingPort::qualifiedName)
                    .containsExactly("com.shop.PlaceOrder");
        }

        @Test
        @DisplayName("driven ports are streamed in identity order")
        void drivenPortsAreStreamed() {
            assertThat(ports.drivenPorts())
                    .extracting(DrivenPort::qualifiedName)
                    .containsExactly("com.shop.OrderRepository", "com.shop.PaymentGateway");
        }
    }

    @Nested
    @DisplayName("Streams by port type")
    class StreamsByPortType {

        @Test
        @DisplayName("repositories are filtered")
        void repositoriesAreFiltered() {
            assertThat(ports.repositories())
                    .extracting(DrivenPort::qualifiedName)
                    .containsExactly("com.shop.OrderRepository");
        }

        @Test
        @DisplayName("gateways are filtered")
        void gatewaysAreFiltered() {
            assertThat(ports.gateways())
                    .extracting(DrivenPort::qualifiedName)
                    .containsExactly("com.shop.PaymentGateway");
        }
    }

    @Nested
    @DisplayName("Aggregate navigation")
    class AggregateNavigation {

        @Test
        @DisplayName("the repository managing an aggregate is found")
        void repositoryManagingAggregateIsFound() {
            assertThat(ports.repositoryFor(ShopModelFixtures.ORDER))
                    .map(DrivenPort::qualifiedName)
                    .contains("com.shop.OrderRepository");
        }

        @Test
        @DisplayName("an aggregate without repository answers empty")
        void aggregateWithoutRepositoryAnswersEmpty() {
            assertThat(ports.repositoryFor(ShopModelFixtures.CUSTOMER)).isEmpty();
        }
    }
}
