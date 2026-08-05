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

package io.hexaglue.plugin.livingdoc;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.DomainEvent;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.arch.DrivingPort;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.FieldRole;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A shop whose every kind the pages have to say something about: an aggregate with an identity, a
 * part, a value and an event, the ports on both sides of it, and one type nothing could be said
 * about.
 */
final class ShopFixture {

    static final TypeId ORDER = TypeId.of("com.shop.domain.Order");
    static final TypeId ORDER_ID = TypeId.of("com.shop.domain.OrderId");
    static final TypeId ORDER_LINE = TypeId.of("com.shop.domain.OrderLine");
    static final TypeId MONEY = TypeId.of("com.shop.domain.Money");
    static final TypeId ORDER_PLACED = TypeId.of("com.shop.domain.OrderPlaced");
    static final TypeId ORDERS = TypeId.of("com.shop.domain.Orders");
    static final TypeId PLACE_ORDER = TypeId.of("com.shop.application.PlaceOrder");
    static final TypeId STRING_UTILS = TypeId.of("com.shop.util.StringUtils");

    private ShopFixture() {}

    static ArchModel model() {
        return ArchModel.builder()
                .addType(order())
                .addType(orderId())
                .addType(orderLine())
                .addType(money())
                .addType(orderPlaced())
                .addType(orders())
                .addType(placeOrder())
                .addType(stringUtils())
                .build();
    }

    private static Classification verdict(ArchKind kind, Confidence confidence, Basis basis) {
        Classification.Builder builder =
                Classification.builder(kind, confidence, basis, ProofNode.fact(kind + " by fixture"));
        if (kind == ArchKind.DRIVING_PORT) {
            builder.direction(PortDirection.DRIVING);
        }
        if (kind == ArchKind.DRIVEN_PORT) {
            builder.direction(PortDirection.DRIVEN);
        }
        return builder.evidences(List.of(new Evidence(
                        EvidenceTier.GRAPH_RELATION,
                        Confidence.HIGH,
                        "managed-by(" + kind + ")",
                        "a repository names it",
                        Optional.empty(),
                        List.of())))
                .build();
    }

    private static Classification verdict(ArchKind kind) {
        return verdict(kind, Confidence.HIGH, Basis.INFERRED);
    }

    private static AggregateRoot order() {
        Field identity = Field.builder("id", TypeRef.of(ORDER_ID.toString()))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        Field lines = Field.builder(
                        "lines", new TypeRef.Named("java.util.List", List.of(TypeRef.of(ORDER_LINE.toString()))))
                .elementType(TypeRef.of(ORDER_LINE.toString()))
                .build();
        Field total = Field.of("total", TypeRef.of(MONEY.toString()));
        return new AggregateRoot(
                ORDER,
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(identity, lines, total))
                        .build(),
                verdict(ArchKind.AGGREGATE_ROOT),
                Optional.of(identity),
                Optional.of(TypeRef.of(ORDER_ID.toString())),
                List.of(TypeRef.of(ORDER_LINE.toString())),
                List.of(TypeRef.of(MONEY.toString())),
                List.of(TypeRef.of(ORDER_PLACED.toString())),
                Optional.of(TypeRef.of(ORDERS.toString())),
                List.of());
    }

    private static Identifier orderId() {
        return new Identifier(
                ORDER_ID,
                TypeStructure.builder(TypeNature.RECORD).build(),
                verdict(ArchKind.IDENTIFIER, Confidence.EXPLICIT, Basis.DECLARED),
                Optional.of(TypeRef.of("java.util.UUID")));
    }

    private static Entity orderLine() {
        Field identity = Field.builder("id", TypeRef.of("java.util.UUID"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        return new Entity(
                ORDER_LINE,
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(identity))
                        .build(),
                verdict(ArchKind.ENTITY),
                Optional.of(identity),
                Optional.of(TypeRef.of(ORDER.toString())));
    }

    private static ValueObject money() {
        return new ValueObject(
                MONEY,
                TypeStructure.builder(TypeNature.RECORD)
                        .fields(List.of(Field.of("amount", TypeRef.of("java.math.BigDecimal"))))
                        .build(),
                verdict(ArchKind.VALUE_OBJECT));
    }

    private static DomainEvent orderPlaced() {
        return new DomainEvent(
                ORDER_PLACED,
                TypeStructure.builder(TypeNature.RECORD).build(),
                verdict(ArchKind.DOMAIN_EVENT),
                Optional.empty(),
                Optional.empty(),
                Optional.of(TypeRef.of(ORDER.toString())));
    }

    private static DrivenPort orders() {
        Method save = Method.builder("save", TypeRef.of("void"))
                .parameters(List.of(Parameter.of("order", TypeRef.of(ORDER.toString()))))
                .build();
        return new DrivenPort(
                ORDERS,
                TypeStructure.builder(TypeNature.INTERFACE)
                        .methods(List.of(save))
                        .build(),
                verdict(ArchKind.DRIVEN_PORT),
                DrivenPortType.REPOSITORY,
                Optional.of(TypeRef.of(ORDER.toString())));
    }

    private static DrivingPort placeOrder() {
        Method place = Method.builder("place", TypeRef.of(ORDER_ID.toString()))
                .parameters(List.of(Parameter.of("command", TypeRef.of("com.shop.application.PlaceOrderCommand"))))
                .build();
        return new DrivingPort(
                PLACE_ORDER,
                TypeStructure.builder(TypeNature.INTERFACE)
                        .methods(List.of(place))
                        .build(),
                verdict(ArchKind.DRIVING_PORT),
                List.of(),
                List.of(TypeRef.of("com.shop.application.PlaceOrderCommand")),
                List.of(TypeRef.of(ORDER_ID.toString())),
                Optional.empty());
    }

    private static UnclassifiedType stringUtils() {
        return new UnclassifiedType(
                STRING_UTILS,
                TypeStructure.builder(TypeNature.CLASS).build(),
                verdict(ArchKind.UNCLASSIFIED),
                UnclassifiedType.UnclassifiedCategory.UTILITY,
                Optional.of("nothing of the perimeter uses it"));
    }
}
