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
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompositionIndexTest {

    private final CompositionIndex compositions = ShopModelFixtures.shopModel().compositionIndex();

    @Nested
    @DisplayName("Embedding")
    class Embedding {

        @Test
        @DisplayName("the types embedded by an aggregate are its entities then its value objects")
        void embeddedByListsEntitiesThenValueObjects() {
            assertThat(compositions.embeddedBy(ShopModelFixtures.ORDER))
                    .containsExactly(ShopModelFixtures.ORDER_LINE, ShopModelFixtures.MONEY);
        }

        @Test
        @DisplayName("an aggregate without composition embeds nothing")
        void aggregateWithoutCompositionEmbedsNothing() {
            assertThat(compositions.embeddedBy(ShopModelFixtures.CUSTOMER)).isEmpty();
        }

        @Test
        @DisplayName("a non-aggregate embeds nothing")
        void nonAggregateEmbedsNothing() {
            assertThat(compositions.embeddedBy(ShopModelFixtures.MONEY)).isEmpty();
            assertThat(compositions.embeddedBy(TypeId.of("com.shop.Unknown"))).isEmpty();
        }

        @Test
        @DisplayName("the owner of an embedded type is found")
        void ownerOfEmbeddedTypeIsFound() {
            assertThat(compositions.embeddedIn(ShopModelFixtures.ORDER_LINE)).containsExactly(ShopModelFixtures.ORDER);
            assertThat(compositions.embeddedIn(ShopModelFixtures.MONEY)).containsExactly(ShopModelFixtures.ORDER);
        }

        @Test
        @DisplayName("composition presence is reported")
        void compositionPresenceIsReported() {
            assertThat(compositions.hasCompositions(ShopModelFixtures.ORDER)).isTrue();
            assertThat(compositions.hasCompositions(ShopModelFixtures.CUSTOMER)).isFalse();
        }
    }

    @Nested
    @DisplayName("Identity ownership")
    class IdentityOwnership {

        @Test
        @DisplayName("the identifier of an aggregate is its identity field type")
        void identifierOfAggregateIsItsIdentityFieldType() {
            assertThat(compositions.identifierOf(ShopModelFixtures.ORDER)).contains(ShopModelFixtures.ORDER_ID);
            assertThat(compositions.identifierOf(ShopModelFixtures.CUSTOMER)).contains(ShopModelFixtures.CUSTOMER_ID);
        }

        @Test
        @DisplayName("a non-aggregate has no identifier")
        void nonAggregateHasNoIdentifier() {
            assertThat(compositions.identifierOf(ShopModelFixtures.MONEY)).isEmpty();
        }

        @Test
        @DisplayName("the aggregate owning an identifier is found")
        void aggregateOwningIdentifierIsFound() {
            assertThat(compositions.aggregateOf(ShopModelFixtures.ORDER_ID)).contains(ShopModelFixtures.ORDER);
            assertThat(compositions.aggregateOf(ShopModelFixtures.CUSTOMER_ID)).contains(ShopModelFixtures.CUSTOMER);
        }

        @Test
        @DisplayName("an aggregate whose identity was never named answers empty, and indexes nothing")
        void aggregateWithoutANamedIdentityIndexesNothing() {
            AggregateRoot nameless = new AggregateRoot(
                    TypeId.of("com.shop.Basket"),
                    TypeStructure.builder(TypeNature.CLASS).build(),
                    ShopModelFixtures.verdict(ArchKind.AGGREGATE_ROOT),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of(),
                    List.of(),
                    Optional.empty(),
                    List.of());
            CompositionIndex index =
                    ArchModel.builder().addType(nameless).build().compositionIndex();

            assertThat(index.identifierOf(nameless.id())).isEmpty();
        }

        @Test
        @DisplayName("a type that identifies no aggregate answers empty")
        void typeIdentifyingNoAggregateAnswersEmpty() {
            assertThat(compositions.aggregateOf(ShopModelFixtures.MONEY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cross-aggregate references")
    class CrossAggregateReferences {

        @Test
        @DisplayName("a field typed as another aggregate's identifier is a reference")
        void fieldTypedAsAnotherAggregateIdentifierIsReference() {
            assertThat(compositions.referencesFrom(ShopModelFixtures.ORDER))
                    .containsExactly(new CompositionIndex.AggregateReference(
                            ShopModelFixtures.ORDER, ShopModelFixtures.CUSTOMER_ID, ShopModelFixtures.CUSTOMER));
        }

        @Test
        @DisplayName("a collection of identifiers references through its element type")
        void collectionOfIdentifiersReferencesThroughElementType() {
            assertThat(compositions.referencesFrom(ShopModelFixtures.CUSTOMER))
                    .containsExactly(new CompositionIndex.AggregateReference(
                            ShopModelFixtures.CUSTOMER, ShopModelFixtures.ORDER_ID, ShopModelFixtures.ORDER));
        }

        @Test
        @DisplayName("an aggregate's own identity field is not a reference")
        void ownIdentityFieldIsNotReference() {
            assertThat(compositions.referencesFrom(ShopModelFixtures.ORDER))
                    .extracting(CompositionIndex.AggregateReference::aggregateType)
                    .doesNotContain(ShopModelFixtures.ORDER);
        }

        @Test
        @DisplayName("an entity holding its owning aggregate's identifier is not a reference")
        void entityHoldingOwningAggregateIdentifierIsNotReference() {
            assertThat(compositions.referencesFrom(ShopModelFixtures.ORDER_LINE))
                    .isEmpty();
        }

        @Test
        @DisplayName("an entity referencing a foreign aggregate is a reference")
        void entityReferencingForeignAggregateIsReference() {
            Field customerReference = Field.of("customerId", TypeRef.of("com.shop.CustomerId"));
            Entity orderLine = new Entity(
                    ShopModelFixtures.ORDER_LINE,
                    TypeStructure.builder(TypeNature.CLASS)
                            .fields(List.of(customerReference))
                            .build(),
                    ShopModelFixtures.verdict(ArchKind.ENTITY),
                    Optional.empty(),
                    Optional.of(TypeRef.of("com.shop.Order")));
            CompositionIndex index = ArchModel.builder()
                    .addType(ShopModelFixtures.order())
                    .addType(ShopModelFixtures.customer())
                    .addType(orderLine)
                    .build()
                    .compositionIndex();

            assertThat(index.referencesFrom(ShopModelFixtures.ORDER_LINE))
                    .containsExactly(new CompositionIndex.AggregateReference(
                            ShopModelFixtures.ORDER_LINE, ShopModelFixtures.CUSTOMER_ID, ShopModelFixtures.CUSTOMER));
        }

        @Test
        @DisplayName("an unknown source holds no references")
        void unknownSourceHoldsNoReferences() {
            assertThat(compositions.referencesFrom(TypeId.of("com.shop.Unknown")))
                    .isEmpty();
        }

        @Test
        @DisplayName("the types referencing an aggregate are found")
        void typesReferencingAggregateAreFound() {
            assertThat(compositions.referencedBy(ShopModelFixtures.CUSTOMER)).containsExactly(ShopModelFixtures.ORDER);
            assertThat(compositions.referencedBy(ShopModelFixtures.ORDER)).containsExactly(ShopModelFixtures.CUSTOMER);
        }
    }
}
