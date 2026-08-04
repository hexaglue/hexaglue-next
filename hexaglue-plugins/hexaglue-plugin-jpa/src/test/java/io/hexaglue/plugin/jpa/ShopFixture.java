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

package io.hexaglue.plugin.jpa;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.DomainEvent;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.arch.TypeStructure;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RemediationHint;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.FieldRole;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A shop with one of everything the store has to have a shape for: an aggregate identified by a
 * wrapper, a part of its own, a value written into its row, a collection, a reference to another
 * aggregate by identity, and a constant that belongs to no row at all.
 */
// One method per type of the shop, which is what makes the fixture readable: splitting it would
// scatter a single model across two files without making any of it clearer.
@SuppressWarnings("PMD.TooManyMethods")
final class ShopFixture {

    static final TypeId ORDER = TypeId.of("com.shop.domain.Order");
    static final TypeId ORDER_ID = TypeId.of("com.shop.domain.OrderId");
    static final TypeId ORDER_LINE = TypeId.of("com.shop.domain.OrderLine");
    static final TypeId LINE_ID = TypeId.of("com.shop.domain.LineId");
    static final TypeId MONEY = TypeId.of("com.shop.domain.Money");
    static final TypeId CUSTOMER = TypeId.of("com.shop.domain.Customer");
    static final TypeId CUSTOMER_ID = TypeId.of("com.shop.domain.CustomerId");
    static final TypeId SHIPMENT = TypeId.of("com.shop.domain.Shipment");
    static final TypeId SHIPMENT_ID = TypeId.of("com.shop.domain.ShipmentId");
    static final TypeId TAG_ID = TypeId.of("com.shop.domain.TagId");
    static final TypeId ORDER_PLACED = TypeId.of("com.shop.domain.OrderPlaced");
    static final TypeId INVOICE = TypeId.of("com.shop.domain.Invoice");
    static final TypeId INVOICE_ID = TypeId.of("com.shop.domain.InvoiceId");
    static final TypeId ORDERS = TypeId.of("com.shop.domain.Orders");
    static final TypeId INVOICES = TypeId.of("com.shop.domain.Invoices");
    static final TypeId AUDITING = TypeId.of("com.shop.domain.Auditing");
    static final TypeId NOTIFYING = TypeId.of("com.shop.domain.Notifying");
    static final TypeId CUSTOMERS = TypeId.of("com.shop.domain.Customers");

    private static final TypeRef TEXT = TypeRef.of("java.lang.String");
    private static final TypeRef UUID = TypeRef.of("java.util.UUID");
    private static final TypeRef VOID = TypeRef.of("void");

    private ShopFixture() {}

    static ArchModel model() {
        return model(Confidence.HIGH);
    }

    /**
     * The same shop, read with whatever certainty a test wants to put the threshold against.
     *
     * @param confidence how sure the analysis is said to be about the aggregate
     * @return the model
     */
    static ArchModel model(Confidence confidence) {
        return ArchModel.builder()
                .addType(order(confidence))
                .addType(identifier(ORDER_ID))
                .addType(identifier(LINE_ID))
                .addType(identifier(CUSTOMER_ID))
                .addType(orderLine())
                .addType(money())
                .addType(customer())
                .addType(shipment())
                .addType(unwrappedIdentifier())
                .addType(orderPlaced())
                .addType(invoice())
                .addType(identifier(INVOICE_ID))
                .addType(orders())
                .addType(invoices())
                .addType(auditing())
                .addType(notifying())
                .addType(customers())
                .build();
    }

    private static Classification verdict(ArchKind kind, Confidence confidence) {
        return Classification.builder(kind, confidence, Basis.INFERRED, ProofNode.fact(kind + " by fixture"))
                .remediations(List.of(RemediationHint.configureExplicit(TypeId.of("com.shop.domain.Order"), kind)))
                .build();
    }

    private static Field field(String name, TypeRef type, Set<FieldRole> roles) {
        return Field.builder(name, type).roles(roles).build();
    }

    /**
     * Order: identified by a wrapper, holds a value, a collection of its own parts, and another
     * aggregate named by its identity.
     */
    private static AggregateRoot order(Confidence confidence) {
        Field id = Field.builder("id", ref(ORDER_ID))
                .roles(Set.of(FieldRole.IDENTITY, FieldRole.EMBEDDED))
                .wrappedType(UUID)
                .build();
        Field lines = Field.builder("lines", TypeRef.parameterized("java.util.List", ref(ORDER_LINE)))
                .elementType(ref(ORDER_LINE))
                .roles(Set.of(FieldRole.COLLECTION))
                .build();
        Field constant = Field.builder("PREFIX", TEXT)
                .modifiers(Set.of(Modifier.STATIC, Modifier.FINAL))
                .build();
        return new AggregateRoot(
                ORDER,
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(
                                constant,
                                id,
                                field("total", ref(MONEY), Set.of(FieldRole.EMBEDDED)),
                                field("customer", ref(CUSTOMER_ID), Set.of(FieldRole.EMBEDDED)),
                                field("shipment", ref(SHIPMENT), Set.of(FieldRole.AGGREGATE_REFERENCE)),
                                field("invoice", ref(INVOICE), Set.of(FieldRole.AGGREGATE_REFERENCE)),
                                lines))
                        .build(),
                verdict(ArchKind.AGGREGATE_ROOT, confidence),
                Optional.of(id),
                Optional.of(UUID),
                List.of(ref(ORDER_LINE)),
                List.of(ref(MONEY)),
                List.of(),
                Optional.empty(),
                List.of());
    }

    private static Entity orderLine() {
        Field id = Field.builder("id", ref(LINE_ID))
                .roles(Set.of(FieldRole.IDENTITY, FieldRole.EMBEDDED))
                .wrappedType(UUID)
                .build();
        return new Entity(
                ORDER_LINE,
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(
                                id,
                                field("label", TEXT, Set.of()),
                                field("value", TEXT, Set.of()),
                                field("tag", ref(TAG_ID), Set.of(FieldRole.EMBEDDED))))
                        .methods(List.of(
                                answers("id", ref(LINE_ID)),
                                answers("label", TEXT),
                                answers("value", TEXT),
                                answers("tag", ref(TAG_ID))))
                        .constructors(List.of(Constructor.of(List.of(
                                Parameter.of("id", ref(LINE_ID)),
                                Parameter.of("label", TEXT),
                                Parameter.of("value", TEXT),
                                Parameter.of("tag", ref(TAG_ID))))))
                        .build(),
                verdict(ArchKind.ENTITY, Confidence.HIGH),
                Optional.of(id),
                Optional.of(ref(ORDER)));
    }

    /** A value with two components, which is what makes it something to write into a row. */
    private static ValueObject money() {
        TypeRef decimal = TypeRef.of("java.math.BigDecimal");
        return new ValueObject(
                MONEY,
                TypeStructure.builder(TypeNature.RECORD)
                        .fields(List.of(field("amount", decimal, Set.of()), field("currency", TEXT, Set.of())))
                        .methods(List.of(answers("amount", decimal), answers("currency", TEXT)))
                        .constructors(List.of(Constructor.of(
                                List.of(Parameter.of("amount", decimal), Parameter.of("currency", TEXT)))))
                        .build(),
                verdict(ArchKind.VALUE_OBJECT, Confidence.HIGH));
    }

    /** An aggregate with no identity the analysis could name. */
    private static AggregateRoot customer() {
        return new AggregateRoot(
                CUSTOMER,
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(field("name", TEXT, Set.of())))
                        .build(),
                verdict(ArchKind.AGGREGATE_ROOT, Confidence.HIGH),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                List.of());
    }

    /** A part held whole rather than named by its identity: a row of its own, joined to. */
    private static Entity shipment() {
        Field id = Field.builder("id", ref(SHIPMENT_ID))
                .roles(Set.of(FieldRole.IDENTITY, FieldRole.EMBEDDED))
                .wrappedType(UUID)
                .build();
        return new Entity(
                SHIPMENT,
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(id, field("carrier", TEXT, Set.of())))
                        .build(),
                verdict(ArchKind.ENTITY, Confidence.HIGH),
                Optional.of(id),
                Optional.of(ref(ORDER)));
    }

    /** Another aggregate held whole rather than named by its identity — a row it is joined to. */
    private static AggregateRoot invoice() {
        Field id = Field.builder("id", ref(INVOICE_ID))
                .roles(Set.of(FieldRole.IDENTITY, FieldRole.EMBEDDED))
                .wrappedType(UUID)
                .build();
        return new AggregateRoot(
                INVOICE,
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(id, field("reference", TEXT, Set.of())))
                        .methods(List.of(answers("id", ref(INVOICE_ID)), answers("reference", TEXT)))
                        .constructors(List.of(Constructor.of(
                                List.of(Parameter.of("id", ref(INVOICE_ID)), Parameter.of("reference", TEXT)))))
                        .build(),
                verdict(ArchKind.AGGREGATE_ROOT, Confidence.HIGH),
                Optional.of(id),
                Optional.of(UUID),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                List.of());
    }

    /** An identity the analysis could not see inside: stored as itself, for want of anything else. */
    private static Identifier unwrappedIdentifier() {
        return new Identifier(
                TAG_ID,
                TypeStructure.builder(TypeNature.CLASS)
                        .fields(List.of(field("left", TEXT, Set.of()), field("right", TEXT, Set.of())))
                        .build(),
                verdict(ArchKind.IDENTIFIER, Confidence.HIGH),
                Optional.empty());
    }

    /** What the domain announces. The store has no shape for it, and says nothing about it. */
    private static DomainEvent orderPlaced() {
        return new DomainEvent(
                ORDER_PLACED,
                TypeStructure.builder(TypeNature.RECORD)
                        .fields(List.of(field("at", TEXT, Set.of())))
                        .build(),
                verdict(ArchKind.DOMAIN_EVENT, Confidence.HIGH),
                Optional.empty(),
                Optional.empty(),
                Optional.of(ref(ORDER)));
    }

    /**
     * The way out the aggregate is kept behind. Three questions: one by identity, which the store
     * already answers; one by a value the aggregate holds; and one asking whether there is any.
     */
    private static DrivenPort orders() {
        return new DrivenPort(
                ORDERS,
                TypeStructure.builder(TypeNature.INTERFACE)
                        .methods(List.of(
                                asks("findById", TypeRef.parameterized("java.util.Optional", ref(ORDER)), ORDER_ID),
                                asks("of", TypeRef.parameterized("java.util.List", ref(ORDER)), CUSTOMER_ID),
                                asks("one", TypeRef.parameterized("java.util.Optional", ref(ORDER)), SHIPMENT),
                                asks(
                                        "alsoByCustomer",
                                        TypeRef.parameterized("java.util.List", ref(ORDER)),
                                        CUSTOMER_ID),
                                asks("byNothingKnown", ref(ORDER), TypeId.of("java.time.Instant")),
                                asks("has", TypeRef.of("boolean"), CUSTOMER_ID),
                                asks("howMany", TypeRef.of("long"), CUSTOMER_ID),
                                takes("archive", VOID, "order", ref(ORDER))))
                        .build(),
                port(),
                DrivenPortType.REPOSITORY,
                Optional.of(ref(ORDER)));
    }

    /**
     * The way out an invoice is kept behind, asking only things the store has an answer for: one
     * question per shape a store operation has, plus one the generated interface has to declare.
     */
    private static DrivenPort invoices() {
        TypeRef invoice = ref(INVOICE);
        TypeRef identity = ref(INVOICE_ID);
        return new DrivenPort(
                INVOICES,
                TypeStructure.builder(TypeNature.INTERFACE)
                        .methods(List.of(
                                takes("save", VOID, "invoice", invoice),
                                takes("store", invoice, "invoice", invoice),
                                takes("findById", TypeRef.parameterized("java.util.Optional", invoice), "id", identity),
                                takes("existsById", TypeRef.of("boolean"), "id", identity),
                                takes("delete", VOID, "id", identity),
                                Method.of("all", TypeRef.parameterized("java.util.List", invoice)),
                                Method.of("howMany", TypeRef.of("long")),
                                takes(
                                        "withReference",
                                        TypeRef.parameterized("java.util.List", invoice),
                                        "reference",
                                        TEXT)))
                        .build(),
                port(),
                DrivenPortType.REPOSITORY,
                Optional.of(invoice));
    }

    /** A store for an aggregate whose identity nothing names: there is no key to serve rows by. */
    private static DrivenPort customers() {
        return new DrivenPort(
                CUSTOMERS,
                TypeStructure.builder(TypeNature.INTERFACE).build(),
                port(),
                DrivenPortType.REPOSITORY,
                Optional.of(ref(CUSTOMER)));
    }

    /** A way out that is not a store at all: nothing is written for it. */
    private static DrivenPort notifying() {
        return new DrivenPort(
                NOTIFYING,
                TypeStructure.builder(TypeNature.INTERFACE).build(),
                port(),
                DrivenPortType.GATEWAY,
                Optional.of(ref(ORDER)));
    }

    /** A way out that keeps nothing: there is no table it could be served from. */
    private static DrivenPort auditing() {
        return new DrivenPort(
                AUDITING,
                TypeStructure.builder(TypeNature.INTERFACE).build(),
                port(),
                DrivenPortType.REPOSITORY,
                Optional.empty());
    }

    private static Classification port() {
        return Classification.builder(
                        ArchKind.DRIVEN_PORT, Confidence.HIGH, Basis.INFERRED, ProofNode.fact("DRIVEN_PORT by fixture"))
                .direction(PortDirection.DRIVEN)
                .build();
    }

    /** A reader: no parameters, answering with the type of the field it stands for. */
    private static Method answers(String name, TypeRef type) {
        return Method.of(name, type);
    }

    private static Method asks(String name, TypeRef answer, TypeId argument) {
        return takes(name, answer, "what", ref(argument));
    }

    /** A question taking one value, under the name the port gave it. */
    private static Method takes(String name, TypeRef answer, String parameter, TypeRef type) {
        return Method.builder(name, answer)
                .parameters(List.of(Parameter.of(parameter, type)))
                .build();
    }

    private static Identifier identifier(TypeId id) {
        return new Identifier(
                id,
                TypeStructure.builder(TypeNature.RECORD)
                        .fields(List.of(field("value", UUID, Set.of())))
                        .methods(List.of(answers("value", UUID)))
                        .constructors(List.of(Constructor.of(List.of(Parameter.of("value", UUID)))))
                        .build(),
                verdict(ArchKind.IDENTIFIER, Confidence.HIGH),
                Optional.of(UUID));
    }

    static TypeRef ref(TypeId id) {
        return TypeRef.of(id.qualifiedName());
    }
}
