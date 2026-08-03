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
 * What identification tolerates, conformity is allowed to condemn — so every check here is stated
 * against a model the engine was right to read the way it did.
 */
class DomainFindingsTest {

    private static List<Finding> coded(List<Finding> findings, IssueCode code) {
        return findings.stream().filter(finding -> finding.code().equals(code)).toList();
    }

    @Nested
    @DisplayName("the boundary of an aggregate")
    class Boundary {

        @Test
        @DisplayName("says when something outside reaches a part directly")
        void reportsAPartReachedFromOutside() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregate("com.shop.Order", "com.shop.OrderLine")
                    .entity("com.shop.OrderLine", "com.shop.Order")
                    .aggregate("com.shop.Invoice")
                    .uses("com.shop.Invoice", "com.shop.OrderLine")
                    .judge();

            assertThat(coded(findings, DomainFindings.BOUNDARY)).singleElement().satisfies(finding -> {
                assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.OrderLine"));
                assertThat(finding.severity()).isEqualTo(Severity.MAJOR);
                assertThat(finding.message()).contains("Invoice").contains("Order");
                assertThat(finding.relatedTypes()).containsExactly(TypeId.of("com.shop.Invoice"));
            });
        }

        @Test
        @DisplayName("says nothing when only the root reaches its own part")
        void staysSilentWhenTheRootReachesItsPart() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregate("com.shop.Order", "com.shop.OrderLine")
                    .entity("com.shop.OrderLine", "com.shop.Order")
                    .uses("com.shop.Order", "com.shop.OrderLine")
                    .judge();

            assertThat(coded(findings, DomainFindings.BOUNDARY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("who owns a part")
    class Ownership {

        @Test
        @DisplayName("says when two aggregates claim the same part")
        void reportsAPartClaimedTwice() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregate("com.shop.Order", "com.shop.Address")
                    .aggregate("com.shop.Customer", "com.shop.Address")
                    .entity("com.shop.Address", "com.shop.Order")
                    .judge();

            assertThat(coded(findings, DomainFindings.OWNERSHIP))
                    .singleElement()
                    .satisfies(finding -> {
                        assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.Address"));
                        assertThat(finding.severity()).isEqualTo(Severity.CRITICAL);
                        assertThat(finding.relatedTypes())
                                .containsExactly(TypeId.of("com.shop.Customer"), TypeId.of("com.shop.Order"));
                    });
        }
    }

    @Nested
    @DisplayName("aggregates in a circle")
    class Circles {

        @Test
        @DisplayName("says the knot once, not once per way round it")
        void reportsAKnotOnce() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregate("com.shop.Order")
                    .aggregate("com.shop.Customer")
                    .uses("com.shop.Order", "com.shop.Customer")
                    .uses("com.shop.Customer", "com.shop.Order")
                    .judge();

            assertThat(coded(findings, DomainFindings.AGGREGATE_CYCLE))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.relatedTypes())
                            .containsExactly(TypeId.of("com.shop.Customer"), TypeId.of("com.shop.Order")));
        }

        @Test
        @DisplayName("says nothing when one aggregate names another and it stops there")
        void staysSilentWithoutACircle() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregate("com.shop.Order")
                    .aggregate("com.shop.Customer")
                    .uses("com.shop.Order", "com.shop.Customer")
                    .judge();

            assertThat(coded(findings, DomainFindings.AGGREGATE_CYCLE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("storage")
    class Storage {

        @Test
        @DisplayName("says when nothing stores an aggregate")
        void reportsAnAggregateNothingStores() {
            List<Finding> findings =
                    ShopJudgements.shop().aggregate("com.shop.Order").judge();

            assertThat(coded(findings, DomainFindings.NO_REPOSITORY))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.Order")));
        }

        @Test
        @DisplayName("says nothing when a port manages it")
        void staysSilentWhenAPortManagesIt() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregateStoredBy("com.shop.Order", "com.shop.Orders")
                    .repository("com.shop.Orders", "com.shop.Order")
                    .judge();

            assertThat(coded(findings, DomainFindings.NO_REPOSITORY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("what the domain names")
    class Purity {

        @Test
        @DisplayName("says when a domain type names an adapter")
        void reportsADomainTypeNamingAnAdapter() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregate("com.shop.Order")
                    .drivenAdapter("com.shop.SqlOrders")
                    .uses("com.shop.Order", "com.shop.SqlOrders")
                    .judge();

            assertThat(coded(findings, DomainFindings.PURITY)).singleElement().satisfies(finding -> {
                assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.Order"));
                assertThat(finding.severity()).isEqualTo(Severity.CRITICAL);
                assertThat(finding.relatedTypes()).containsExactly(TypeId.of("com.shop.SqlOrders"));
            });
        }

        @Test
        @DisplayName("says nothing when the adapter is the one naming the domain")
        void staysSilentWhenTheAdapterNamesTheDomain() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregate("com.shop.Order")
                    .drivenAdapter("com.shop.SqlOrders")
                    .uses("com.shop.SqlOrders", "com.shop.Order")
                    .judge();

            assertThat(coded(findings, DomainFindings.PURITY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("identity and change")
    class Shape {

        @Test
        @DisplayName("says when an entity has nothing to tell its instances apart")
        void reportsAnEntityWithoutIdentity() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregate("com.shop.Order", "com.shop.OrderLine")
                    .entityWithoutIdentity("com.shop.OrderLine", "com.shop.Order")
                    .judge();

            assertThat(coded(findings, DomainFindings.NO_IDENTITY))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.OrderLine")));
        }

        @Test
        @DisplayName("says when a value can be changed in place")
        void reportsAMutableValue() {
            List<Finding> findings = ShopJudgements.shop()
                    .mutableValue("com.shop.Money", "setAmount")
                    .judge();

            assertThat(coded(findings, DomainFindings.MUTABLE_VALUE))
                    .singleElement()
                    .satisfies(finding -> assertThat(finding.message()).contains("setAmount"));
        }

        @Test
        @DisplayName("says nothing about a value that only answers questions")
        void staysSilentAboutAnImmutableValue() {
            List<Finding> findings =
                    ShopJudgements.shop().value("com.shop.Money").judge();

            assertThat(coded(findings, DomainFindings.MUTABLE_VALUE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("a part nothing distinguishes from a value")
    class Undecidable {

        @Test
        @DisplayName("names it, and says the declaration that would settle it")
        void reportsAPartWithAPlatformIdentity() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregateHolding("com.shop.Owner", "com.shop.Pet")
                    .valueWithIdentityField("com.shop.Pet")
                    .judge();

            assertThat(coded(findings, DomainFindings.UNDECIDABLE_PART))
                    .singleElement()
                    .satisfies(finding -> {
                        assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.Pet"));
                        assertThat(finding.severity()).isEqualTo(Severity.MINOR);
                        assertThat(finding.remediations()).isNotEmpty();
                    });
        }

        @Test
        @DisplayName("says nothing about a value that carries no identity")
        void staysSilentAboutAPlainValue() {
            List<Finding> findings = ShopJudgements.shop()
                    .aggregateHolding("com.shop.Order", "com.shop.Money")
                    .value("com.shop.Money")
                    .judge();

            assertThat(coded(findings, DomainFindings.UNDECIDABLE_PART)).isEmpty();
        }
    }

    @Nested
    @DisplayName("the order findings come in")
    class Order {

        @Test
        @DisplayName("is the same on every run")
        void isStable() {
            ShopJudgements shop = ShopJudgements.shop()
                    .aggregate("com.shop.Order")
                    .aggregate("com.shop.Customer")
                    .uses("com.shop.Order", "com.shop.Customer")
                    .uses("com.shop.Customer", "com.shop.Order");

            assertThat(shop.judge()).isEqualTo(shop.judge());
            assertThat(shop.judge().stream().map(Finding::code).map(IssueCode::value))
                    .isSorted();
        }
    }
}
