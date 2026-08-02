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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.FieldRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A small classified shop domain shared by the container and index tests: two aggregates (Order
 * composing an entity and a value object, Customer referencing orders by identifier), their
 * identifiers, one event, three ports, the two adapters wired to them, application and domain
 * services, and a utility fallback.
 */
final class ShopModelFixtures {

    static final TypeId ORDER = TypeId.of("com.shop.Order");
    static final TypeId ORDER_ID = TypeId.of("com.shop.OrderId");
    static final TypeId ORDER_LINE = TypeId.of("com.shop.OrderLine");
    static final TypeId MONEY = TypeId.of("com.shop.Money");
    static final TypeId ORDER_PLACED = TypeId.of("com.shop.OrderPlaced");
    static final TypeId ORDER_REPOSITORY = TypeId.of("com.shop.OrderRepository");
    static final TypeId PAYMENT_GATEWAY = TypeId.of("com.shop.PaymentGateway");
    static final TypeId PLACE_ORDER = TypeId.of("com.shop.PlaceOrder");
    static final TypeId CUSTOMER = TypeId.of("com.shop.Customer");
    static final TypeId CUSTOMER_ID = TypeId.of("com.shop.CustomerId");
    static final TypeId ORDER_REST_CONTROLLER = TypeId.of("com.shop.OrderRestController");
    static final TypeId JPA_ORDER_REPOSITORY = TypeId.of("com.shop.JpaOrderRepository");
    static final TypeId CHECKOUT_SERVICE = TypeId.of("com.shop.CheckoutService");
    static final TypeId PRICING_SERVICE = TypeId.of("com.shop.PricingService");
    static final TypeId STRING_UTILS = TypeId.of("com.shop.StringUtils");

    private ShopModelFixtures() {}

    static Classification verdict(ArchKind kind) {
        Classification.Builder builder =
                Classification.builder(kind, Confidence.HIGH, Basis.INFERRED, ProofNode.fact(kind + " by fixture"));
        if (kind == ArchKind.DRIVING_PORT || kind == ArchKind.DRIVING_ADAPTER) {
            builder.direction(PortDirection.DRIVING);
        }
        if (kind == ArchKind.DRIVEN_PORT || kind == ArchKind.DRIVEN_ADAPTER) {
            builder.direction(PortDirection.DRIVEN);
        }
        return builder.build();
    }

    static TypeStructure structure(TypeNature nature) {
        return TypeStructure.builder(nature).build();
    }

    static AggregateRoot order() {
        Field identity = Field.builder("id", TypeRef.of("com.shop.OrderId"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        Field customerReference = Field.of("customerId", TypeRef.of("com.shop.CustomerId"));
        Field lines = Field.builder(
                        "lines", new TypeRef.Named("java.util.List", List.of(TypeRef.of("com.shop.OrderLine"))))
                .elementType(TypeRef.of("com.shop.OrderLine"))
                .build();
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .fields(List.of(identity, customerReference, lines))
                .build();
        return new AggregateRoot(
                ORDER,
                structure,
                verdict(ArchKind.AGGREGATE_ROOT),
                identity,
                TypeRef.of("java.util.UUID"),
                List.of(TypeRef.of("com.shop.OrderLine")),
                List.of(TypeRef.of("com.shop.Money")),
                List.of(TypeRef.of("com.shop.OrderPlaced")),
                Optional.of(TypeRef.of("com.shop.OrderRepository")),
                List.of());
    }

    static AggregateRoot customer() {
        Field identity = Field.builder("id", TypeRef.of("com.shop.CustomerId"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        Field orderReferences = Field.builder(
                        "orderIds", new TypeRef.Named("java.util.List", List.of(TypeRef.of("com.shop.OrderId"))))
                .elementType(TypeRef.of("com.shop.OrderId"))
                .build();
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .fields(List.of(identity, orderReferences))
                .build();
        return new AggregateRoot(
                CUSTOMER,
                structure,
                verdict(ArchKind.AGGREGATE_ROOT),
                identity,
                TypeRef.of("java.util.UUID"),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                List.of());
    }

    static Entity orderLine() {
        Field owningReference = Field.of("orderId", TypeRef.of("com.shop.OrderId"));
        Field quantity = Field.of("quantity", new TypeRef.Primitive("int"));
        TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                .fields(List.of(owningReference, quantity))
                .build();
        return new Entity(
                ORDER_LINE,
                structure,
                verdict(ArchKind.ENTITY),
                Optional.of(Field.of("id", TypeRef.of("java.lang.Long"))),
                Optional.of(TypeRef.of("com.shop.Order")));
    }

    static ValueObject money() {
        return new ValueObject(MONEY, structure(TypeNature.RECORD), verdict(ArchKind.VALUE_OBJECT));
    }

    static Identifier orderId() {
        return new Identifier(
                ORDER_ID, structure(TypeNature.RECORD), verdict(ArchKind.IDENTIFIER), TypeRef.of("java.util.UUID"));
    }

    static Identifier customerId() {
        return new Identifier(
                CUSTOMER_ID, structure(TypeNature.RECORD), verdict(ArchKind.IDENTIFIER), TypeRef.of("java.util.UUID"));
    }

    static DomainEvent orderPlaced() {
        return DomainEvent.of(ORDER_PLACED, structure(TypeNature.RECORD), verdict(ArchKind.DOMAIN_EVENT));
    }

    static DrivenPort orderRepository() {
        return new DrivenPort(
                ORDER_REPOSITORY,
                structure(TypeNature.INTERFACE),
                verdict(ArchKind.DRIVEN_PORT),
                DrivenPortType.REPOSITORY,
                Optional.of(TypeRef.of("com.shop.Order")));
    }

    static DrivenPort paymentGateway() {
        return new DrivenPort(
                PAYMENT_GATEWAY,
                structure(TypeNature.INTERFACE),
                verdict(ArchKind.DRIVEN_PORT),
                DrivenPortType.GATEWAY,
                Optional.empty());
    }

    static DrivingPort placeOrder() {
        return new DrivingPort(
                PLACE_ORDER,
                structure(TypeNature.INTERFACE),
                verdict(ArchKind.DRIVING_PORT),
                List.of(),
                List.of(),
                List.of());
    }

    static DrivingAdapter orderRestController() {
        return new DrivingAdapter(
                ORDER_REST_CONTROLLER,
                structure(TypeNature.CLASS),
                verdict(ArchKind.DRIVING_ADAPTER),
                List.of(TypeRef.of("com.shop.PlaceOrder")));
    }

    static DrivenAdapter jpaOrderRepository() {
        return new DrivenAdapter(
                JPA_ORDER_REPOSITORY,
                structure(TypeNature.CLASS),
                verdict(ArchKind.DRIVEN_ADAPTER),
                List.of(TypeRef.of("com.shop.OrderRepository")));
    }

    static ApplicationService checkoutService() {
        return new ApplicationService(
                CHECKOUT_SERVICE, structure(TypeNature.CLASS), verdict(ArchKind.APPLICATION_SERVICE));
    }

    static DomainService pricingService() {
        return new DomainService(
                PRICING_SERVICE, structure(TypeNature.CLASS), verdict(ArchKind.DOMAIN_SERVICE), List.of(), List.of());
    }

    static UnclassifiedType stringUtils() {
        return new UnclassifiedType(
                STRING_UTILS,
                structure(TypeNature.CLASS),
                verdict(ArchKind.UNCLASSIFIED),
                UnclassifiedType.UnclassifiedCategory.UTILITY,
                Optional.of("utility holder"));
    }

    static ModuleTopology topology() {
        return ModuleTopology.builder()
                .addModule(new ModuleDescriptor("shop-domain", ModuleRole.DOMAIN, Optional.of("com.shop")))
                .addModule(ModuleDescriptor.of("shop-infra", ModuleRole.INFRASTRUCTURE))
                .assign(ORDER, "shop-domain")
                .assign(CUSTOMER, "shop-domain")
                .assign(ORDER_REPOSITORY, "shop-domain")
                .build();
    }

    static ArchModel shopModel() {
        return ArchModel.builder()
                .addType(order())
                .addType(orderId())
                .addType(orderLine())
                .addType(money())
                .addType(orderPlaced())
                .addType(orderRepository())
                .addType(paymentGateway())
                .addType(placeOrder())
                .addType(orderRestController())
                .addType(jpaOrderRepository())
                .addType(customer())
                .addType(customerId())
                .addType(checkoutService())
                .addType(pricingService())
                .addType(stringUtils())
                .moduleTopology(topology())
                .build();
    }
}
