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

class DomainIndexTest {

    private final DomainIndex domain = ShopModelFixtures.shopModel().domainIndex();

    @Nested
    @DisplayName("Streams by kind")
    class StreamsByKind {

        @Test
        @DisplayName("aggregate roots are streamed in identity order")
        void aggregateRootsAreStreamed() {
            assertThat(domain.aggregateRoots())
                    .extracting(AggregateRoot::qualifiedName)
                    .containsExactly("com.shop.Customer", "com.shop.Order");
        }

        @Test
        @DisplayName("entities are streamed")
        void entitiesAreStreamed() {
            assertThat(domain.entities()).extracting(Entity::qualifiedName).containsExactly("com.shop.OrderLine");
        }

        @Test
        @DisplayName("value objects are streamed")
        void valueObjectsAreStreamed() {
            assertThat(domain.valueObjects())
                    .extracting(ValueObject::qualifiedName)
                    .containsExactly("com.shop.Money");
        }

        @Test
        @DisplayName("identifiers are streamed in identity order")
        void identifiersAreStreamed() {
            assertThat(domain.identifiers())
                    .extracting(Identifier::qualifiedName)
                    .containsExactly("com.shop.CustomerId", "com.shop.OrderId");
        }

        @Test
        @DisplayName("domain events are streamed")
        void domainEventsAreStreamed() {
            assertThat(domain.domainEvents())
                    .extracting(DomainEvent::qualifiedName)
                    .containsExactly("com.shop.OrderPlaced");
        }

        @Test
        @DisplayName("domain services are streamed")
        void domainServicesAreStreamed() {
            assertThat(domain.domainServices())
                    .extracting(DomainService::qualifiedName)
                    .containsExactly("com.shop.PricingService");
        }
    }

    @Nested
    @DisplayName("Aggregate navigation")
    class AggregateNavigation {

        @Test
        @DisplayName("an aggregate root is found by id")
        void aggregateRootIsFoundById() {
            assertThat(domain.aggregateRoot(ShopModelFixtures.ORDER))
                    .map(AggregateRoot::qualifiedName)
                    .contains("com.shop.Order");
        }

        @Test
        @DisplayName("a non-aggregate id answers empty")
        void nonAggregateIdAnswersEmpty() {
            assertThat(domain.aggregateRoot(ShopModelFixtures.MONEY)).isEmpty();
        }

        @Test
        @DisplayName("the entities of an aggregate are resolved")
        void entitiesOfAggregateAreResolved() {
            assertThat(domain.entitiesOf(ShopModelFixtures.order()))
                    .extracting(Entity::qualifiedName)
                    .containsExactly("com.shop.OrderLine");
        }

        @Test
        @DisplayName("the value objects of an aggregate are resolved")
        void valueObjectsOfAggregateAreResolved() {
            assertThat(domain.valueObjectsOf(ShopModelFixtures.order()))
                    .extracting(ValueObject::qualifiedName)
                    .containsExactly("com.shop.Money");
        }

        @Test
        @DisplayName("an aggregate without composition resolves to empty lists")
        void aggregateWithoutCompositionResolvesToEmptyLists() {
            assertThat(domain.entitiesOf(ShopModelFixtures.customer())).isEmpty();
            assertThat(domain.valueObjectsOf(ShopModelFixtures.customer())).isEmpty();
        }

        @Test
        @DisplayName("a composition reference absent from the model is skipped")
        void compositionReferenceAbsentFromModelIsSkipped() {
            DomainIndex sparse = ArchModel.builder()
                    .addType(ShopModelFixtures.order())
                    .build()
                    .domainIndex();

            assertThat(sparse.entitiesOf(ShopModelFixtures.order())).isEmpty();
            assertThat(sparse.valueObjectsOf(ShopModelFixtures.order())).isEmpty();
        }
    }
}
