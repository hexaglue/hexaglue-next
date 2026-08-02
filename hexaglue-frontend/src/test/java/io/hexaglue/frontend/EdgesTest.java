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

package io.hexaglue.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Relations toward the classpath are the strongest signal enterprise code carries: one
 * {@code extends JpaRepository<Order, OrderId>} establishes a persistence port, an aggregate and
 * an identifier at once. They only exist if nothing filters edges down to the analyzed packages.
 */
class EdgesTest {

    @TempDir
    Path sources;

    private CodeModel analyze() {
        return SpoonFrontend.analyze(FrontendRequest.of(sources));
    }

    private List<String> edgesOf(CodeModel model, String source) {
        return model.edgesFrom(TypeId.of(source)).stream()
                .map(Edge::toDisplayString)
                .toList();
    }

    @Nested
    @DisplayName("relations toward the classpath")
    class TowardTheClasspath {

        @Test
        @DisplayName("keeps a parameterized supertype whole, arguments included")
        void keepsParameterizedSupertypeWhole() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(
                    sources, "com/acme/OrderId.java", "package com.acme; public record OrderId(String v) {}");
            SourceFixtures.write(sources, "com/acme/OrderRepository.java", """
                    package com.acme;
                    import org.springframework.data.jpa.repository.JpaRepository;
                    public interface OrderRepository extends JpaRepository<Order, OrderId> {}
                    """);

            CodeModel model = analyze();

            assertThat(edgesOf(model, "com.acme.OrderRepository"))
                    .contains(
                            "com.acme.OrderRepository -IMPLEMENTS-> org.springframework.data.jpa.repository.JpaRepository",
                            "com.acme.OrderRepository -TYPE_ARGUMENT-> com.acme.Order[arg 0]",
                            "com.acme.OrderRepository -TYPE_ARGUMENT-> com.acme.OrderId[arg 1]");
        }

        @Test
        @DisplayName("gives every referenced classpath type a lightweight stub")
        void givesReferencedClasspathTypesAStub() {
            SourceFixtures.write(sources, "com/acme/OrderRepository.java", """
                    package com.acme;
                    import org.springframework.data.jpa.repository.JpaRepository;
                    public interface OrderRepository extends JpaRepository<String, Long> {}
                    """);

            CodeModel model = analyze();
            TypeNode stub = model.type(TypeId.of("org.springframework.data.jpa.repository.JpaRepository"))
                    .orElseThrow();

            assertThat(stub.external()).isTrue();
            assertThat(stub.nature()).isEqualTo(TypeNature.INTERFACE);
            assertThat(stub.fields()).isEmpty();
            assertThat(stub.methods()).isEmpty();
            assertThat(stub.sourceLocation()).isEmpty();
        }

        @Test
        @DisplayName("reads an annotation from a jar as a relation and a stub")
        void readsAnnotationFromAJar() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Order.java",
                    "package com.acme; @jakarta.persistence.Entity public class Order {}");

            CodeModel model = analyze();

            assertThat(edgesOf(model, "com.acme.Order"))
                    .contains("com.acme.Order -ANNOTATED_BY-> jakarta.persistence.Entity");
            assertThat(model.type(TypeId.of("jakarta.persistence.Entity"))
                            .orElseThrow()
                            .nature())
                    .isEqualTo(TypeNature.ANNOTATION);
        }

        @Test
        @DisplayName("leaves analyzed types out of the stubs")
        void leavesAnalyzedTypesOutOfStubs() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "com/acme/Line.java", "package com.acme; public class Line { Order order; }");

            CodeModel model = analyze();

            assertThat(model.type(TypeId.of("com.acme.Order")).orElseThrow().external())
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("declaration relations")
    class DeclarationRelations {

        @Test
        @DisplayName("records extends, implements, permits and declares")
        void recordsDeclarationRelations() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Shape.java",
                    "package com.acme; public sealed interface Shape permits Circle {}");
            SourceFixtures.write(sources, "com/acme/Circle.java", """
                    package com.acme;
                    public final class Circle extends Base implements Shape {
                        static class Inner {}
                    }
                    """);
            SourceFixtures.write(sources, "com/acme/Base.java", "package com.acme; public class Base {}");

            CodeModel model = analyze();

            assertThat(edgesOf(model, "com.acme.Shape")).contains("com.acme.Shape -PERMITS-> com.acme.Circle");
            assertThat(edgesOf(model, "com.acme.Circle"))
                    .contains(
                            "com.acme.Circle -EXTENDS-> com.acme.Base",
                            "com.acme.Circle -IMPLEMENTS-> com.acme.Shape",
                            "com.acme.Circle -DECLARES-> com.acme.Circle$Inner");
        }

        @Test
        @DisplayName("records member relations with the member they come from")
        void recordsMemberRelationsWithProvenance() {
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    import java.util.List;
                    public class Order {
                        private List<Line> lines;
                        public OrderId identity(Line line, int quantity) throws java.io.IOException { return null; }
                    }
                    """);

            CodeModel model = analyze();

            assertThat(edgesOf(model, "com.acme.Order"))
                    .contains(
                            "com.acme.Order -FIELD_TYPE-> java.util.List @lines",
                            "com.acme.Order -TYPE_ARGUMENT-> com.acme.Line @lines[arg 0]",
                            "com.acme.Order -RETURN_TYPE-> com.acme.OrderId @identity",
                            "com.acme.Order -PARAMETER_TYPE-> com.acme.Line @identity[param 0]",
                            "com.acme.Order -THROWS_TYPE-> java.io.IOException @identity");
        }

        @Test
        @DisplayName("records an annotation on a member with the member it annotates")
        void recordsMemberAnnotations() {
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    public class Order {
                        @jakarta.persistence.Id private String id;
                        public void save(@jakarta.validation.Valid Line line) {}
                    }
                    """);

            CodeModel model = analyze();

            assertThat(edgesOf(model, "com.acme.Order"))
                    .contains(
                            "com.acme.Order -ANNOTATED_BY-> jakarta.persistence.Id @id",
                            "com.acme.Order -ANNOTATED_BY-> jakarta.validation.Valid @save[param 0]");
        }

        @Test
        @DisplayName("records constructor parameters with their position")
        void recordsConstructorParameters() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Order.java",
                    "package com.acme; public class Order { public Order(OrderId id, Line line) {} }");

            CodeModel model = analyze();

            assertThat(edgesOf(model, "com.acme.Order"))
                    .contains(
                            "com.acme.Order -PARAMETER_TYPE-> com.acme.OrderId @<init>[param 0]",
                            "com.acme.Order -PARAMETER_TYPE-> com.acme.Line @<init>[param 1]");
        }
    }

    @Nested
    @DisplayName("what carries no identity")
    class NoIdentity {

        @Test
        @DisplayName("emits no relation toward primitives, type variables or wildcards")
        void emitsNoRelationTowardTypesWithoutIdentity() {
            SourceFixtures.write(sources, "com/acme/Box.java", """
                    package com.acme;
                    import java.util.List;
                    public class Box<T> {
                        private int count;
                        private T item;
                        private List<?> anything;
                    }
                    """);

            CodeModel model = analyze();

            assertThat(model.types()).extracting(TypeNode::id).doesNotContain(TypeId.of("T"), TypeId.of("?"));
            assertThat(edgesOf(model, "com.acme.Box"))
                    .containsExactly("com.acme.Box -FIELD_TYPE-> java.util.List @anything");
        }

        @Test
        @DisplayName("reaches the type a wildcard is bounded by")
        void reachesTheTypeAWildcardIsBoundedBy() {
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    import java.util.List;
                    public class Order {
                        private List<? extends Line> lines;
                        private List<? super Line> sink;
                    }
                    """);

            assertThat(edgesOf(analyze(), "com.acme.Order"))
                    .contains(
                            "com.acme.Order -TYPE_ARGUMENT-> com.acme.Line @lines[arg 0]",
                            "com.acme.Order -TYPE_ARGUMENT-> com.acme.Line @sink[arg 0]");
        }

        @Test
        @DisplayName("targets the component type of an array")
        void targetsTheComponentTypeOfAnArray() {
            SourceFixtures.write(
                    sources, "com/acme/Order.java", "package com.acme; public class Order { private Line[] lines; }");

            assertThat(edgesOf(analyze(), "com.acme.Order"))
                    .containsExactly("com.acme.Order -FIELD_TYPE-> com.acme.Line @lines");
        }
    }

    @Nested
    @DisplayName("determinism")
    class DeterministicOutput {

        @Test
        @DisplayName("emits the same edges in the same order on repeated runs")
        void emitsTheSameEdgesOnRepeatedRuns() {
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    import java.util.List;
                    public class Order implements java.io.Serializable, Comparable<Order> {
                        private List<Line> lines;
                        private OrderId id;
                        public void add(Line line) {}
                    }
                    """);

            List<String> first =
                    analyze().edges().stream().map(Edge::toDisplayString).toList();
            List<String> second =
                    analyze().edges().stream().map(Edge::toDisplayString).toList();

            assertThat(first).isEqualTo(second).isNotEmpty();
        }

        @Test
        @DisplayName("indexes edges by source and by target")
        void indexesEdgesBySourceAndTarget() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "com/acme/Line.java", "package com.acme; public class Line extends Order {}");

            CodeModel model = analyze();

            assertThat(model.edgesTo(TypeId.of("com.acme.Order")))
                    .extracting(Edge::kind)
                    .containsExactly(EdgeKind.EXTENDS);
        }
    }
}
