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
import io.hexaglue.model.arch.Backends;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.arch.PortFamily;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * The same sources are right or wrong depending on what the build does with them: a hole
     * nothing in the sources fills is a fault, unless this very build writes what fills it.
     */
    @Nested
    @DisplayName("a hole this build fills itself")
    class Generated {

        private ShopJudgements aPortAndAWayIn() {
            return ShopJudgements.shop()
                    .repository("com.shop.Orders", "com.shop.Order")
                    .applicationService("com.shop.OrderService")
                    .uses("com.shop.OrderService", "com.shop.Orders")
                    .drivingPort("com.shop.PlaceOrder")
                    .applicationService("com.shop.PlaceOrderService", "com.shop.PlaceOrder");
        }

        private Backends writing(PortFamily... families) {
            return new Backends(Map.of("io.hexaglue.jpa", Set.of(families)));
        }

        @Test
        @DisplayName("is not reported as a hole")
        void isNotReported() {
            Judged judged =
                    aPortAndAWayIn().judgeOnABuildThatGenerates(writing(PortFamily.driven(DrivenPortType.REPOSITORY)));

            assertThat(coded(judged.findings(), HexagonalFindings.DRIVEN_PORT_UNPLUGGED))
                    .isEmpty();
        }

        @Test
        @DisplayName("and is said to have been left out, by whose word")
        void isSaidToHaveBeenLeftOut() {
            Judged judged =
                    aPortAndAWayIn().judgeOnABuildThatGenerates(writing(PortFamily.driven(DrivenPortType.REPOSITORY)));

            assertThat(judged.diagnostics()).singleElement().satisfies(diagnostic -> {
                assertThat(diagnostic.code()).isEqualTo(HexagonalFindings.FILLED_BY_GENERATION);
                assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.INFO);
                assertThat(diagnostic.message())
                        .contains("1 port(s)")
                        .contains("com.shop.Orders")
                        .contains("io.hexaglue.jpa");
            });
        }

        @Test
        @DisplayName("while a hole of a family nothing declared goes on being reported")
        void aFamilyNobodyDeclaredIsStillReported() {
            Judged judged =
                    aPortAndAWayIn().judgeOnABuildThatGenerates(writing(PortFamily.driven(DrivenPortType.GATEWAY)));

            assertThat(coded(judged.findings(), HexagonalFindings.DRIVEN_PORT_UNPLUGGED))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.Orders")));
            assertThat(judged.diagnostics()).isEmpty();
        }

        @Test
        @DisplayName("and a way in nothing drives falls silent the same way, on the same word")
        void aWayInFallsSilentTheSameWay() {
            Judged judged = aPortAndAWayIn().judgeOnABuildThatGenerates(writing(PortFamily.driving()));

            assertThat(coded(judged.findings(), HexagonalFindings.DRIVING_PORT_UNDRIVEN))
                    .isEmpty();
            assertThat(coded(judged.findings(), HexagonalFindings.DRIVEN_PORT_UNPLUGGED))
                    .hasSize(1);
            assertThat(judged.diagnostics())
                    .singleElement()
                    .satisfies(diagnostic -> assertThat(diagnostic.message()).contains("com.shop.PlaceOrder"));
        }

        @Test
        @DisplayName("and a build generating nothing says nothing about it")
        void aBuildGeneratingNothingSaysNothing() {
            Judged judged = aPortAndAWayIn().judgeOnABuildThatGenerates(Backends.none());

            assertThat(coded(judged.findings(), HexagonalFindings.DRIVEN_PORT_UNPLUGGED))
                    .hasSize(1);
            assertThat(judged.diagnostics()).isEmpty();
        }

        /**
         * The other checks read a port from a different side, and none of them is about a hole the
         * build fills: silencing them too would hide what generation does not answer for.
         */
        @Test
        @DisplayName("but the core still has to call the port it declared")
        void theCoreStillHasToCallIt() {
            Judged judged = ShopJudgements.shop()
                    .repository("com.shop.Orders", "com.shop.Order")
                    .judgeOnABuildThatGenerates(writing(PortFamily.driven(DrivenPortType.REPOSITORY)));

            assertThat(coded(judged.findings(), HexagonalFindings.DRIVEN_PORT_UNUSED))
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
