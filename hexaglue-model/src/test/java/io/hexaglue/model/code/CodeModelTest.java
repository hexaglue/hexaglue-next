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

package io.hexaglue.model.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CodeModelTest {

    private static final TypeId ORDER = TypeId.of("com.a.Order");
    private static final TypeId ORDER_ID = TypeId.of("com.a.OrderId");
    private static final TypeId REPOSITORY = TypeId.of("com.a.OrderRepository");
    private static final TypeId JPA_REPOSITORY = TypeId.of("org.springframework.data.jpa.repository.JpaRepository");

    private CodeModel exampleModel() {
        return CodeModel.builder()
                .addModule(ModuleNode.of("shop-domain"))
                .addType(TypeNode.builder(ORDER, TypeNature.CLASS).build())
                .addType(TypeNode.builder(ORDER_ID, TypeNature.RECORD).build())
                .addType(TypeNode.builder(REPOSITORY, TypeNature.INTERFACE).build())
                .addType(TypeNode.externalStub(JPA_REPOSITORY, TypeNature.INTERFACE))
                .addEdge(Edge.of(REPOSITORY, EdgeKind.IMPLEMENTS, JPA_REPOSITORY))
                .addEdge(new Edge(
                        REPOSITORY,
                        ORDER,
                        EdgeKind.TYPE_ARGUMENT,
                        Optional.empty(),
                        OptionalInt.empty(),
                        OptionalInt.of(0)))
                .addEdge(new Edge(
                        REPOSITORY,
                        ORDER_ID,
                        EdgeKind.TYPE_ARGUMENT,
                        Optional.empty(),
                        OptionalInt.empty(),
                        OptionalInt.of(1)))
                .supertypes(REPOSITORY, List.of(JPA_REPOSITORY))
                .build();
    }

    @Nested
    @DisplayName("Lookups")
    class Lookups {

        @Test
        @DisplayName("types iterate in identity order")
        void typesIterateInIdentityOrder() {
            assertThat(exampleModel().types())
                    .extracting(node -> node.id().qualifiedName())
                    .containsExactly(
                            "com.a.Order",
                            "com.a.OrderId",
                            "com.a.OrderRepository",
                            "org.springframework.data.jpa.repository.JpaRepository");
        }

        @Test
        @DisplayName("a type is found by id")
        void typeIsFoundById() {
            assertThat(exampleModel().type(ORDER)).map(TypeNode::nature).contains(TypeNature.CLASS);
        }

        @Test
        @DisplayName("an unknown id answers empty")
        void unknownIdAnswersEmpty() {
            assertThat(exampleModel().type(TypeId.of("com.a.Unknown"))).isEmpty();
        }

        @Test
        @DisplayName("modules are listed")
        void modulesAreListed() {
            assertThat(exampleModel().modules()).extracting(ModuleNode::name).containsExactly("shop-domain");
        }
    }

    @Nested
    @DisplayName("Edges with provenance")
    class EdgesWithProvenance {

        @Test
        @DisplayName("the repository declaration yields its edges toward the external stub and the type arguments")
        void repositoryDeclarationYieldsItsEdges() {
            List<Edge> outgoing = exampleModel().edgesFrom(REPOSITORY);

            assertThat(outgoing).hasSize(3);
            assertThat(outgoing.get(0).kind()).isEqualTo(EdgeKind.IMPLEMENTS);
            assertThat(outgoing.get(1).typeArgumentIndex()).hasValue(0);
            assertThat(outgoing.get(2).typeArgumentIndex()).hasValue(1);
        }

        @Test
        @DisplayName("incoming edges are indexed by target")
        void incomingEdgesAreIndexedByTarget() {
            assertThat(exampleModel().edgesTo(ORDER)).hasSize(1);
            assertThat(exampleModel().edgesTo(JPA_REPOSITORY)).hasSize(1);
            assertThat(exampleModel().edgesTo(TypeId.of("com.a.Unknown"))).isEmpty();
        }

        @Test
        @DisplayName("an edge renders its provenance")
        void edgeRendersItsProvenance() {
            Edge edge = new Edge(
                    REPOSITORY,
                    ORDER,
                    EdgeKind.RETURN_TYPE,
                    Optional.of("findById"),
                    OptionalInt.empty(),
                    OptionalInt.empty());

            assertThat(edge.toDisplayString()).isEqualTo("com.a.OrderRepository -RETURN_TYPE-> com.a.Order @findById");
        }
    }

    @Nested
    @DisplayName("Closure and capabilities")
    class ClosureAndCapabilities {

        @Test
        @DisplayName("the supertype closure is stored and queried")
        void supertypeClosureIsStoredAndQueried() {
            assertThat(exampleModel().supertypesOf(REPOSITORY)).containsExactly(JPA_REPOSITORY);
            assertThat(exampleModel().supertypesOf(ORDER)).isEmpty();
        }

        @Test
        @DisplayName("body facts require their capability")
        void bodyFactsRequireTheirCapability() {
            CodeModel.Builder builder =
                    CodeModel.builder().addBodyFacts(new MethodBodyFacts(ORDER, "total", List.of(), List.of()));

            assertThatIllegalArgumentException().isThrownBy(builder::build).withMessageContaining("METHOD_BODIES");
        }

        @Test
        @DisplayName("body facts are available under their capability")
        void bodyFactsAreAvailableUnderCapability() {
            CodeModel model = CodeModel.builder()
                    .capability(CodeModelCapability.METHOD_BODIES)
                    .addBodyFacts(new MethodBodyFacts(
                            ORDER,
                            "total",
                            List.of(new MethodBodyFacts.Invocation(ORDER_ID, "value")),
                            List.of(new MethodBodyFacts.Instantiation(ORDER_ID))))
                    .build();

            assertThat(model.capabilities()).containsExactly(CodeModelCapability.METHOD_BODIES);
            assertThat(model.bodyFacts()).hasSize(1);
            assertThat(model.bodyFacts().get(0).invocations()).hasSize(1);
            assertThat(model.bodyFacts().get(0).instantiations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Loud failures")
    class LoudFailures {

        @Test
        @DisplayName("a duplicate type id is rejected")
        void duplicateTypeIdIsRejected() {
            CodeModel.Builder builder = CodeModel.builder()
                    .addType(TypeNode.builder(ORDER, TypeNature.CLASS).build())
                    .addType(TypeNode.builder(ORDER, TypeNature.RECORD).build());

            assertThatIllegalArgumentException().isThrownBy(builder::build).withMessageContaining("duplicate type id");
        }
    }
}
