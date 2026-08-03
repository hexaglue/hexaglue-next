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

package io.hexaglue.engine.finding;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A port is checked from both sides on purpose: nothing plugged into it and nothing in the core
 * using it are different failures, and a report that merged them would say half of what it knows.
 */
class HexagonalFindingsTest {

    private static List<Finding> coded(List<Finding> findings, IssueCode code) {
        return findings.stream().filter(finding -> finding.code().equals(code)).toList();
    }

    /**
     * A hexagon with nothing wrong with it: a port, the adapter that fills it, the service that
     * uses it, and a way in that something drives.
     */
    private static ShopJudgements soundHexagon() {
        return ShopJudgements.shop()
                .repository("com.shop.Orders", "com.shop.Order")
                .drivenAdapterFor("com.shop.SqlOrders", "com.shop.Orders")
                .drivingPort("com.shop.PlaceOrder")
                .applicationService("com.shop.OrderService", "com.shop.PlaceOrder")
                .drivingAdapterFor("com.shop.OrderController", "com.shop.PlaceOrder")
                .aggregateStoredBy("com.shop.Order", "com.shop.Orders")
                .uses("com.shop.OrderService", "com.shop.Orders");
    }

    @Nested
    @DisplayName("a hexagon that holds")
    class Sound {

        @Test
        @DisplayName("says nothing about it")
        void staysSilent() {
            List<Finding> findings = soundHexagon().judge();

            assertThat(findings.stream()
                            .map(Finding::code)
                            .map(IssueCode::value)
                            .filter(code -> code.startsWith("HG-HEX")))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("the application reaching past its ports")
    class ApplicationPurity {

        @Test
        @DisplayName("says which adapter it named, and which port to name instead")
        void namesThePortToUseInstead() {
            List<Finding> findings = soundHexagon()
                    .uses("com.shop.OrderService", "com.shop.SqlOrders")
                    .judge();

            assertThat(coded(findings, HexagonalFindings.APPLICATION_NAMES_ADAPTER))
                    .singleElement()
                    .satisfies(finding -> {
                        assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.OrderService"));
                        assertThat(finding.severity()).isEqualTo(Severity.CRITICAL);
                        assertThat(finding.message()).contains("SqlOrders").contains("Name Orders instead");
                    });
        }

        @Test
        @DisplayName("says it once, not once per rule that could have said it")
        void saysItOnce() {
            List<Finding> findings = soundHexagon()
                    .uses("com.shop.OrderService", "com.shop.SqlOrders")
                    .judge();

            assertThat(findings.stream()
                            .filter(finding -> finding.subject().equals(TypeId.of("com.shop.OrderService"))))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("a port with a side missing")
    class Ports {

        @Test
        @DisplayName("says when nothing plugs into a driven port")
        void reportsAnUnpluggedDrivenPort() {
            List<Finding> findings = ShopJudgements.shop()
                    .repository("com.shop.Orders", "com.shop.Order")
                    .applicationService("com.shop.OrderService")
                    .uses("com.shop.OrderService", "com.shop.Orders")
                    .judge();

            assertThat(coded(findings, HexagonalFindings.DRIVEN_PORT_UNPLUGGED))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.Orders")));
        }

        @Test
        @DisplayName("says when nothing in the core answers a driving port")
        void reportsAnUnansweredDrivingPort() {
            List<Finding> findings = ShopJudgements.shop()
                    .drivingPort("com.shop.PlaceOrder")
                    .drivingAdapterFor("com.shop.OrderController", "com.shop.PlaceOrder")
                    .judge();

            assertThat(coded(findings, HexagonalFindings.DRIVING_PORT_UNANSWERED))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.PlaceOrder")));
        }

        @Test
        @DisplayName("says when the core never calls a driven port it declared")
        void reportsADrivenPortNobodyCalls() {
            List<Finding> findings = ShopJudgements.shop()
                    .repository("com.shop.Orders", "com.shop.Order")
                    .drivenAdapterFor("com.shop.SqlOrders", "com.shop.Orders")
                    .judge();

            assertThat(coded(findings, HexagonalFindings.DRIVEN_PORT_UNUSED))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.severity()).isEqualTo(Severity.MINOR));
        }

        @Test
        @DisplayName("says when nothing outside drives a driving port")
        void reportsAnUndrivenDrivingPort() {
            List<Finding> findings = ShopJudgements.shop()
                    .drivingPort("com.shop.PlaceOrder")
                    .applicationService("com.shop.OrderService", "com.shop.PlaceOrder")
                    .judge();

            assertThat(coded(findings, HexagonalFindings.DRIVING_PORT_UNDRIVEN))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.PlaceOrder")));
        }

        @Test
        @DisplayName("says when a port carries an implementation")
        void reportsAPortThatIsNotAnInterface() {
            List<Finding> findings = ShopJudgements.shop()
                    .drivingPortAsClass("com.shop.PlaceOrder")
                    .judge();

            assertThat(coded(findings, HexagonalFindings.PORT_NOT_AN_INTERFACE))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.message()).contains("class"));
        }
    }

    @Nested
    @DisplayName("the world talking to itself")
    class Adapters {

        @Test
        @DisplayName("says when an adapter reaches another without going through the core")
        void reportsAnAdapterReachingAnAdapter() {
            List<Finding> findings = soundHexagon()
                    .uses("com.shop.OrderController", "com.shop.SqlOrders")
                    .judge();

            assertThat(coded(findings, HexagonalFindings.ADAPTER_TO_ADAPTER))
                    .singleElement()
                    .satisfies(finding -> {
                        assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.OrderController"));
                        assertThat(finding.relatedTypes()).containsExactly(TypeId.of("com.shop.SqlOrders"));
                    });
        }
    }
}
