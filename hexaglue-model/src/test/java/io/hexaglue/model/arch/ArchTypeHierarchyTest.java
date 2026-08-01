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
import io.hexaglue.model.Modifier;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArchTypeHierarchyTest {

    private static Classification verdict(ArchKind kind) {
        Classification.Builder builder =
                Classification.builder(kind, Confidence.HIGH, Basis.INFERRED, ProofNode.fact("test verdict"));
        if (kind == ArchKind.DRIVING_PORT) {
            builder.direction(PortDirection.DRIVING);
        }
        if (kind == ArchKind.DRIVEN_PORT) {
            builder.direction(PortDirection.DRIVEN);
        }
        return builder.build();
    }

    private static TypeStructure emptyStructure(TypeNature nature) {
        return TypeStructure.builder(nature).build();
    }

    @Nested
    @DisplayName("Kind coherence")
    class KindCoherenceInvariant {

        @Test
        @DisplayName("a record only accepts a verdict of its own kind")
        void recordOnlyAcceptsVerdictOfItsOwnKind() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ValueObject(
                            TypeId.of("com.a.Money"), emptyStructure(TypeNature.RECORD), verdict(ArchKind.ENTITY)))
                    .withMessageContaining("ENTITY verdict in a VALUE_OBJECT record");
        }

        @Test
        @DisplayName("every concrete kind builds with its own verdict")
        void everyConcreteKindBuildsWithItsOwnVerdict() {
            TypeStructure structure = emptyStructure(TypeNature.CLASS);

            assertThat(new ValueObject(TypeId.of("com.a.V"), structure, verdict(ArchKind.VALUE_OBJECT)).kind())
                    .isEqualTo(ArchKind.VALUE_OBJECT);
            assertThat(new ApplicationService(TypeId.of("com.a.S"), structure, verdict(ArchKind.APPLICATION_SERVICE))
                            .kind())
                    .isEqualTo(ArchKind.APPLICATION_SERVICE);
            assertThat(new CommandHandler(TypeId.of("com.a.C"), structure, verdict(ArchKind.COMMAND_HANDLER)).kind())
                    .isEqualTo(ArchKind.COMMAND_HANDLER);
            assertThat(new QueryHandler(TypeId.of("com.a.Q"), structure, verdict(ArchKind.QUERY_HANDLER)).kind())
                    .isEqualTo(ArchKind.QUERY_HANDLER);
        }
    }

    @Nested
    @DisplayName("Domain enrichments")
    class DomainEnrichments {

        @Test
        @DisplayName("an aggregate root carries its identity and composition")
        void aggregateRootCarriesIdentityAndComposition() {
            Field identity = Field.builder("id", TypeRef.of("com.a.OrderId"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            AggregateRoot aggregate = new AggregateRoot(
                    TypeId.of("com.a.Order"),
                    emptyStructure(TypeNature.CLASS),
                    verdict(ArchKind.AGGREGATE_ROOT),
                    identity,
                    TypeRef.of("java.util.UUID"),
                    List.of(TypeRef.of("com.a.OrderLine")),
                    List.of(TypeRef.of("com.a.Money")),
                    List.of(TypeRef.of("com.a.OrderPlaced")),
                    Optional.of(TypeRef.of("com.a.OrderRepository")),
                    List.of(Invariant.of("validateTotal", "Total is never negative")));

            assertThat(aggregate.kind()).isEqualTo(ArchKind.AGGREGATE_ROOT);
            assertThat(aggregate.identityField().isIdentity()).isTrue();
            assertThat(aggregate.effectiveIdentityType().qualifiedName()).isEqualTo("java.util.UUID");
            assertThat(aggregate.hasComposition()).isTrue();
            assertThat(aggregate.drivenPort()).isPresent();
            assertThat(aggregate.invariants()).hasSize(1);
            assertThat(aggregate.simpleName()).isEqualTo("Order");
        }

        @Test
        @DisplayName("an entity knows its identity and owner when established")
        void entityKnowsIdentityAndOwner() {
            Entity entity = new Entity(
                    TypeId.of("com.a.OrderLine"),
                    emptyStructure(TypeNature.CLASS),
                    verdict(ArchKind.ENTITY),
                    Optional.of(Field.of("id", TypeRef.of("java.lang.Long"))),
                    Optional.of(TypeRef.of("com.a.Order")));

            assertThat(entity.hasIdentity()).isTrue();
            assertThat(entity.hasOwningAggregate()).isTrue();
        }

        @Test
        @DisplayName("an aggregate without composition and an orphan entity answer negatively")
        void emptyEnrichmentsAnswerNegatively() {
            AggregateRoot bare = new AggregateRoot(
                    TypeId.of("com.a.Config"),
                    emptyStructure(TypeNature.CLASS),
                    verdict(ArchKind.AGGREGATE_ROOT),
                    Field.of("id", TypeRef.of("java.lang.Long")),
                    TypeRef.of("java.lang.Long"),
                    List.of(),
                    List.of(),
                    List.of(),
                    Optional.empty(),
                    List.of());
            Entity orphan = new Entity(
                    TypeId.of("com.a.Orphan"),
                    emptyStructure(TypeNature.CLASS),
                    verdict(ArchKind.ENTITY),
                    Optional.empty(),
                    Optional.empty());
            DomainService pureService = new DomainService(
                    TypeId.of("com.a.TaxRules"),
                    emptyStructure(TypeNature.CLASS),
                    verdict(ArchKind.DOMAIN_SERVICE),
                    List.of(),
                    List.of());

            assertThat(bare.hasComposition()).isFalse();
            assertThat(orphan.hasIdentity()).isFalse();
            assertThat(orphan.hasOwningAggregate()).isFalse();
            assertThat(pureService.hasInjectedPorts()).isFalse();
        }

        @Test
        @DisplayName("a single-field value object exposes its wrapped field")
        void singleFieldValueObjectExposesWrappedField() {
            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                    .fields(List.of(Field.of("amount", TypeRef.of("java.math.BigDecimal"))))
                    .build();
            ValueObject money = new ValueObject(TypeId.of("com.a.Money"), structure, verdict(ArchKind.VALUE_OBJECT));

            assertThat(money.isSingleValue()).isTrue();
            assertThat(money.wrappedField()).map(Field::name).contains("amount");
        }

        @Test
        @DisplayName("a multi-field value object wraps nothing")
        void multiFieldValueObjectWrapsNothing() {
            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                    .fields(List.of(
                            Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                            Field.of("currency", TypeRef.of("java.lang.String"))))
                    .build();
            ValueObject money = new ValueObject(TypeId.of("com.a.Money"), structure, verdict(ArchKind.VALUE_OBJECT));

            assertThat(money.isSingleValue()).isFalse();
            assertThat(money.wrappedField()).isEmpty();
        }

        @Test
        @DisplayName("an identifier wraps its underlying type")
        void identifierWrapsUnderlyingType() {
            Identifier identifier = new Identifier(
                    TypeId.of("com.a.OrderId"),
                    emptyStructure(TypeNature.RECORD),
                    verdict(ArchKind.IDENTIFIER),
                    TypeRef.of("java.util.UUID"));

            assertThat(identifier.wrappedType().qualifiedName()).isEqualTo("java.util.UUID");
        }

        @Test
        @DisplayName("a domain event without derived links builds through its factory")
        void domainEventBuildsThroughFactory() {
            DomainEvent event = DomainEvent.of(
                    TypeId.of("com.a.OrderPlaced"), emptyStructure(TypeNature.RECORD), verdict(ArchKind.DOMAIN_EVENT));

            assertThat(event.aggregateIdField()).isEmpty();
            assertThat(event.sourceAggregate()).isEmpty();
        }

        @Test
        @DisplayName("a domain service lists its injected ports")
        void domainServiceListsInjectedPorts() {
            DomainService service = new DomainService(
                    TypeId.of("com.a.PricingService"),
                    emptyStructure(TypeNature.CLASS),
                    verdict(ArchKind.DOMAIN_SERVICE),
                    List.of(TypeRef.of("com.a.RateProvider")),
                    List.of());

            assertThat(service.hasInjectedPorts()).isTrue();
        }
    }

    @Nested
    @DisplayName("Ports")
    class Ports {

        @Test
        @DisplayName("a driving port exposes its direction and use cases")
        void drivingPortExposesDirectionAndUseCases() {
            DrivingPort port = new DrivingPort(
                    TypeId.of("com.a.PlaceOrderUseCase"),
                    emptyStructure(TypeNature.INTERFACE),
                    verdict(ArchKind.DRIVING_PORT),
                    List.of(new UseCase(
                            io.hexaglue.model.declaration.Method.of("place", TypeRef.of("void")),
                            Optional.empty(),
                            UseCase.UseCaseType.COMMAND)),
                    List.of(),
                    List.of());

            assertThat(port.direction()).isEqualTo(PortDirection.DRIVING);
            assertThat(port.useCases()).hasSize(1);
            assertThat(port.useCases().get(0).type().isCommand()).isTrue();
            assertThat(port.useCases().get(0).type().isQuery()).isFalse();
        }

        @Test
        @DisplayName("a driven port knows its family and managed aggregate")
        void drivenPortKnowsFamilyAndManagedAggregate() {
            DrivenPort port = new DrivenPort(
                    TypeId.of("com.a.OrderRepository"),
                    emptyStructure(TypeNature.INTERFACE),
                    verdict(ArchKind.DRIVEN_PORT),
                    DrivenPortType.REPOSITORY,
                    Optional.of(TypeRef.of("com.a.Order")));

            assertThat(port.direction()).isEqualTo(PortDirection.DRIVEN);
            assertThat(port.isRepository()).isTrue();
            assertThat(port.isGateway()).isFalse();
            assertThat(port.managedAggregate()).isPresent();
            assertThat(DrivenPortType.REPOSITORY.description()).isNotBlank();
        }

        @Test
        @DisplayName("a gateway is not a repository")
        void gatewayIsNotRepository() {
            DrivenPort gateway = new DrivenPort(
                    TypeId.of("com.a.PaymentGateway"),
                    emptyStructure(TypeNature.INTERFACE),
                    verdict(ArchKind.DRIVEN_PORT),
                    DrivenPortType.GATEWAY,
                    Optional.empty());

            assertThat(gateway.isGateway()).isTrue();
            assertThat(gateway.isRepository()).isFalse();
        }

        @Test
        @DisplayName("use case types separate commands from queries")
        void useCaseTypesSeparateCommandsFromQueries() {
            assertThat(UseCase.UseCaseType.QUERY.isCommand()).isFalse();
            assertThat(UseCase.UseCaseType.QUERY.isQuery()).isTrue();
            assertThat(UseCase.UseCaseType.COMMAND_QUERY.isCommand()).isTrue();
            assertThat(UseCase.UseCaseType.COMMAND_QUERY.isQuery()).isTrue();
        }
    }

    @Nested
    @DisplayName("Categorized fallback")
    class CategorizedFallback {

        @Test
        @DisplayName("an unclassified type states its category and reason")
        void unclassifiedTypeStatesCategoryAndReason() {
            UnclassifiedType unclassified = new UnclassifiedType(
                    TypeId.of("com.a.SomeHelper"),
                    emptyStructure(TypeNature.CLASS),
                    verdict(ArchKind.UNCLASSIFIED),
                    UnclassifiedType.UnclassifiedCategory.UTILITY,
                    Optional.of("stateless static helpers only"));

            assertThat(unclassified.kind()).isEqualTo(ArchKind.UNCLASSIFIED);
            assertThat(unclassified.category()).isEqualTo(UnclassifiedType.UnclassifiedCategory.UTILITY);
            assertThat(unclassified.reason()).contains("stateless static helpers only");
        }

        @Test
        @DisplayName("the categories cover every documented cause")
        void categoriesCoverEveryDocumentedCause() {
            assertThat(UnclassifiedType.UnclassifiedCategory.values())
                    .extracting(Enum::name)
                    .containsExactly("CONFLICTING", "UTILITY", "OUT_OF_SCOPE", "TECHNICAL", "AMBIGUOUS", "UNKNOWN");
        }
    }

    @Nested
    @DisplayName("Structure")
    class Structure {

        @Test
        @DisplayName("a structure finds fields by name and role")
        void structureFindsFieldsByNameAndRole() {
            Field identity = Field.builder("id", TypeRef.of("com.a.OrderId"))
                    .roles(Set.of(FieldRole.IDENTITY))
                    .build();
            TypeStructure structure = TypeStructure.builder(TypeNature.CLASS)
                    .modifiers(Set.of(Modifier.PUBLIC, Modifier.SEALED))
                    .fields(List.of(identity, Field.of("note", TypeRef.of("java.lang.String"))))
                    .build();

            assertThat(structure.field("id")).isPresent();
            assertThat(structure.field("missing")).isEmpty();
            assertThat(structure.fieldsWithRole(FieldRole.IDENTITY)).containsExactly(identity);
            assertThat(structure.isSealed()).isTrue();
            assertThat(structure.isClass()).isTrue();
            assertThat(structure.isInterface()).isFalse();
            assertThat(structure.isRecord()).isFalse();
        }

        @Test
        @DisplayName("an interface structure answers its nature and is not sealed by default")
        void interfaceStructureAnswersItsNature() {
            TypeStructure structure = emptyStructure(TypeNature.INTERFACE);

            assertThat(structure.isInterface()).isTrue();
            assertThat(structure.isClass()).isFalse();
            assertThat(structure.isRecord()).isFalse();
            assertThat(structure.isSealed()).isFalse();
        }

        @Test
        @DisplayName("a full structure keeps every builder component")
        void fullStructureKeepsEveryBuilderComponent() {
            TypeStructure structure = TypeStructure.builder(TypeNature.RECORD)
                    .modifiers(Set.of(Modifier.PUBLIC))
                    .documentation("An order.")
                    .superClass(TypeRef.of("com.a.Base"))
                    .interfaces(List.of(TypeRef.of("java.io.Serializable")))
                    .permittedSubtypes(List.of(TypeRef.of("com.a.Special")))
                    .fields(List.of(Field.of("id", TypeRef.of("com.a.OrderId"))))
                    .methods(List.of(io.hexaglue.model.declaration.Method.of("total", TypeRef.of("int"))))
                    .constructors(List.of(io.hexaglue.model.declaration.Constructor.noArg()))
                    .annotations(List.of(io.hexaglue.model.declaration.Annotation.of("com.a.Marker")))
                    .nestedTypes(List.of(TypeRef.of("com.a.Order$Line")))
                    .sourceLocation(new io.hexaglue.model.SourceLocation("Order.java", 1, 30))
                    .build();

            assertThat(structure.isRecord()).isTrue();
            assertThat(structure.documentation()).contains("An order.");
            assertThat(structure.superClass()).isPresent();
            assertThat(structure.interfaces()).hasSize(1);
            assertThat(structure.permittedSubtypes()).hasSize(1);
            assertThat(structure.fields()).hasSize(1);
            assertThat(structure.methods()).hasSize(1);
            assertThat(structure.constructors()).hasSize(1);
            assertThat(structure.annotations()).hasSize(1);
            assertThat(structure.nestedTypes()).hasSize(1);
            assertThat(structure.sourceLocation()).isPresent();
        }
    }
}
