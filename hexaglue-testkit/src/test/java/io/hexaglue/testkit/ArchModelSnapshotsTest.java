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

package io.hexaglue.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ApplicationService;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.arch.DrivingPort;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.FieldRole;
import io.hexaglue.model.declaration.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArchModelSnapshotsTest {

    private static Classification verdict(ArchKind kind) {
        Classification.Builder builder =
                Classification.builder(kind, Confidence.HIGH, Basis.INFERRED, ProofNode.fact(kind + " by fixture"));
        if (kind == ArchKind.DRIVING_PORT) {
            builder.direction(PortDirection.DRIVING);
        }
        if (kind == ArchKind.DRIVEN_PORT) {
            builder.direction(PortDirection.DRIVEN);
        }
        return builder.build();
    }

    private static ArchModel shopModel() {
        Field identity = Field.builder("id", TypeRef.of("com.shop.OrderId"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        Field lines = Field.builder(
                        "lines", new TypeRef.Named("java.util.List", List.of(TypeRef.of("com.shop.OrderLine"))))
                .elementType(TypeRef.of("com.shop.OrderLine"))
                .build();
        AggregateRoot order = new AggregateRoot(
                TypeId.of("com.shop.Order"),
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(identity, lines))
                        .build(),
                verdict(ArchKind.AGGREGATE_ROOT),
                identity,
                TypeRef.of("java.util.UUID"),
                List.of(),
                List.of(),
                List.of(),
                Optional.of(TypeRef.of("com.shop.OrderRepository")),
                List.of());
        Identifier orderId = new Identifier(
                TypeId.of("com.shop.OrderId"),
                TypeStructure.builder(TypeNature.RECORD).build(),
                verdict(ArchKind.IDENTIFIER),
                TypeRef.of("java.util.UUID"));
        ApplicationService checkout = new ApplicationService(
                TypeId.of("com.shop.CheckoutService"),
                TypeStructure.builder(TypeNature.CLASS).build(),
                verdict(ArchKind.APPLICATION_SERVICE));
        DrivenPort repository = new DrivenPort(
                TypeId.of("com.shop.OrderRepository"),
                TypeStructure.builder(TypeNature.INTERFACE)
                        .methods(List.of(
                                Method.of("save", TypeRef.of("com.shop.Order")),
                                Method.of("findById", TypeRef.of("java.util.Optional"))))
                        .build(),
                verdict(ArchKind.DRIVEN_PORT),
                DrivenPortType.REPOSITORY,
                Optional.of(TypeRef.of("com.shop.Order")));
        DrivingPort placeOrder = new DrivingPort(
                TypeId.of("com.shop.PlaceOrder"),
                TypeStructure.builder(TypeNature.INTERFACE)
                        .methods(List.of(Method.of("execute", TypeRef.of("com.shop.Order"))))
                        .build(),
                verdict(ArchKind.DRIVING_PORT),
                List.of(),
                List.of(),
                List.of());
        UnclassifiedType utils = new UnclassifiedType(
                TypeId.of("com.shop.StringUtils"),
                TypeStructure.builder(TypeNature.CLASS).build(),
                verdict(ArchKind.UNCLASSIFIED),
                UnclassifiedType.UnclassifiedCategory.UTILITY,
                Optional.empty());
        return ArchModel.builder()
                .addType(order)
                .addType(orderId)
                .addType(checkout)
                .addType(repository)
                .addType(placeOrder)
                .addType(utils)
                .build();
    }

    @Nested
    @DisplayName("Canonical rendering")
    class CanonicalRendering {

        @Test
        @DisplayName("a classified model serializes to the canonical snapshot")
        void classifiedModelSerializesToCanonicalSnapshot() {
            String expected = """
                    {
                      "domain": [
                        {
                          "qualifiedName": "com.shop.Order",
                          "kind": "AGGREGATE_ROOT",
                          "confidence": "HIGH",
                          "basis": "INFERRED",
                          "nature": "CLASS",
                          "identity": {
                            "field": "id",
                            "type": "com.shop.OrderId"
                          },
                          "properties": [
                            {
                              "name": "id",
                              "type": "com.shop.OrderId",
                              "cardinality": "SINGLE"
                            },
                            {
                              "name": "lines",
                              "type": "java.util.List",
                              "cardinality": "COLLECTION"
                            }
                          ]
                        },
                        {
                          "qualifiedName": "com.shop.OrderId",
                          "kind": "IDENTIFIER",
                          "confidence": "HIGH",
                          "basis": "INFERRED",
                          "nature": "RECORD",
                          "wrappedType": "java.util.UUID",
                          "properties": []
                        }
                      ],
                      "application": [
                        {
                          "qualifiedName": "com.shop.CheckoutService",
                          "kind": "APPLICATION_SERVICE",
                          "confidence": "HIGH",
                          "basis": "INFERRED",
                          "nature": "CLASS"
                        }
                      ],
                      "ports": [
                        {
                          "qualifiedName": "com.shop.OrderRepository",
                          "direction": "DRIVEN",
                          "portType": "REPOSITORY",
                          "confidence": "HIGH",
                          "basis": "INFERRED",
                          "methods": ["findById", "save"]
                        },
                        {
                          "qualifiedName": "com.shop.PlaceOrder",
                          "direction": "DRIVING",
                          "confidence": "HIGH",
                          "basis": "INFERRED",
                          "methods": ["execute"]
                        }
                      ],
                      "unclassified": [
                        {
                          "qualifiedName": "com.shop.StringUtils",
                          "category": "UTILITY"
                        }
                      ]
                    }
                    """;

            assertThat(ArchModelSnapshots.serialize(shopModel())).isEqualTo(expected);
        }

        @Test
        @DisplayName("an empty model serializes to empty sections")
        void emptyModelSerializesToEmptySections() {
            String expected = """
                    {
                      "domain": [],
                      "application": [],
                      "ports": [],
                      "unclassified": []
                    }
                    """;

            assertThat(ArchModelSnapshots.serialize(ArchModel.builder().build()))
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Robustness")
    class Robustness {

        @Test
        @DisplayName("special characters in names are escaped")
        void specialCharactersAreEscaped() {
            UnclassifiedType weird = new UnclassifiedType(
                    TypeId.of("com.shop.We\"ird\\Name"),
                    TypeStructure.builder(TypeNature.CLASS).build(),
                    verdict(ArchKind.UNCLASSIFIED),
                    UnclassifiedType.UnclassifiedCategory.UNKNOWN,
                    Optional.empty());
            ArchModel model = ArchModel.builder().addType(weird).build();

            assertThat(ArchModelSnapshots.serialize(model)).contains("\"com.shop.We\\\"ird\\\\Name\"");
        }

        @Test
        @DisplayName("the rendering is stable across runs")
        void renderingIsStableAcrossRuns() {
            ArchModel model = shopModel();

            Determinism.assertStable(5, () -> ArchModelSnapshots.serialize(model));
        }
    }
}
