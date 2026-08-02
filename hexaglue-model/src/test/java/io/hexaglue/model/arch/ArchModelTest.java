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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.ProofNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArchModelTest {

    @Nested
    @DisplayName("Lookups")
    class Lookups {

        @Test
        @DisplayName("types iterate in identity order")
        void typesIterateInIdentityOrder() {
            assertThat(ShopModelFixtures.shopModel().types())
                    .extracting(ArchType::qualifiedName)
                    .containsExactly(
                            "com.shop.CheckoutService",
                            "com.shop.Customer",
                            "com.shop.CustomerId",
                            "com.shop.JpaOrderRepository",
                            "com.shop.Money",
                            "com.shop.Order",
                            "com.shop.OrderId",
                            "com.shop.OrderLine",
                            "com.shop.OrderPlaced",
                            "com.shop.OrderRepository",
                            "com.shop.OrderRestController",
                            "com.shop.PaymentGateway",
                            "com.shop.PlaceOrder",
                            "com.shop.PricingService",
                            "com.shop.StringUtils");
        }

        @Test
        @DisplayName("a type is found by id")
        void typeIsFoundById() {
            assertThat(ShopModelFixtures.shopModel().type(ShopModelFixtures.ORDER))
                    .map(ArchType::kind)
                    .contains(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("an unknown id answers empty")
        void unknownIdAnswersEmpty() {
            assertThat(ShopModelFixtures.shopModel().type(TypeId.of("com.shop.Unknown")))
                    .isEmpty();
        }

        @Test
        @DisplayName("all narrows to a concrete arch type in identity order")
        void allNarrowsToConcreteArchType() {
            assertThat(ShopModelFixtures.shopModel().all(AggregateRoot.class))
                    .extracting(AggregateRoot::qualifiedName)
                    .containsExactly("com.shop.Customer", "com.shop.Order");
        }

        @Test
        @DisplayName("all matches the sealed branches too")
        void allMatchesSealedBranches() {
            assertThat(ShopModelFixtures.shopModel().all(DomainType.class)).hasSize(8);
            assertThat(ShopModelFixtures.shopModel().all(PortType.class)).hasSize(3);
            assertThat(ShopModelFixtures.shopModel().all(AdapterType.class))
                    .extracting(AdapterType::qualifiedName)
                    .containsExactly("com.shop.JpaOrderRepository", "com.shop.OrderRestController");
        }
    }

    @Nested
    @DisplayName("Provenance access")
    class ProvenanceAccess {

        @Test
        @DisplayName("the classification of a type is read by id")
        void classificationIsReadById() {
            assertThat(ShopModelFixtures.shopModel().classificationOf(ShopModelFixtures.ORDER))
                    .map(Classification::kind)
                    .contains(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("explain returns the proof tree of the verdict")
        void explainReturnsProofTree() {
            assertThat(ShopModelFixtures.shopModel().explain(ShopModelFixtures.ORDER))
                    .map(ProofNode::conclusion)
                    .contains("AGGREGATE_ROOT by fixture");
        }

        @Test
        @DisplayName("provenance of an unknown id answers empty")
        void provenanceOfUnknownIdAnswersEmpty() {
            TypeId unknown = TypeId.of("com.shop.Unknown");

            assertThat(ShopModelFixtures.shopModel().classificationOf(unknown)).isEmpty();
            assertThat(ShopModelFixtures.shopModel().explain(unknown)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("a duplicate type id fails loudly")
        void duplicateTypeIdFailsLoudly() {
            ArchModel.Builder builder =
                    ArchModel.builder().addType(ShopModelFixtures.money()).addType(ShopModelFixtures.money());

            assertThatIllegalArgumentException()
                    .isThrownBy(builder::build)
                    .withMessageContaining("duplicate type id")
                    .withMessageContaining("com.shop.Money");
        }

        @Test
        @DisplayName("the module topology defaults to empty")
        void moduleTopologyDefaultsToEmpty() {
            ArchModel model =
                    ArchModel.builder().addType(ShopModelFixtures.money()).build();

            assertThat(model.moduleTopology().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("the module topology is exposed when provided")
        void moduleTopologyIsExposedWhenProvided() {
            assertThat(ShopModelFixtures.shopModel()
                            .moduleTopology()
                            .moduleOf(ShopModelFixtures.ORDER)
                            .map(ModuleDescriptor::name))
                    .contains("shop-domain");
        }
    }
}
