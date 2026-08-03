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

import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MembersTest {

    @TempDir
    Path sources;

    private TypeNode node(String qualifiedName) {
        CodeModel model = SpoonFrontend.analyze(FrontendRequest.of(sources)).code();
        return model.type(TypeId.of(qualifiedName)).orElseThrow();
    }

    @Nested
    @DisplayName("fields")
    class Fields {

        @Test
        @DisplayName("reads declared fields in declaration order, with their shape")
        void readsDeclaredFields() {
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    import java.util.List;
                    public class Order {
                        /** The order identity. */
                        private final OrderId id;
                        private List<String> lines;
                        static int counter;
                    }
                    """);

            TypeNode order = node("com.acme.Order");

            assertThat(order.fields()).extracting(Field::name).containsExactly("id", "lines", "counter");
            Field id = order.fields().get(0);
            assertThat(id.type()).isEqualTo(TypeRef.of("com.acme.OrderId"));
            assertThat(id.modifiers()).containsExactly(Modifier.PRIVATE, Modifier.FINAL);
            assertThat(id.documentation()).contains("The order identity.");
            assertThat(id.sourceLocation()).isPresent();
            assertThat(order.fields().get(1).type().toDisplayString()).isEqualTo("java.util.List<java.lang.String>");
        }

        @Test
        @DisplayName("keeps record components as fields")
        void keepsRecordComponentsAsFields() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Money.java",
                    "package com.acme; public record Money(java.math.BigDecimal amount, String currency) {}");

            assertThat(node("com.acme.Money").fields()).extracting(Field::name).containsExactly("amount", "currency");
        }

        @Test
        @DisplayName("leaves enum constants out of the declared state")
        void leavesEnumConstantsOut() {
            SourceFixtures.write(sources, "com/acme/Status.java", """
                    package com.acme;
                    public enum Status {
                        NEW, PAID;
                        private final boolean terminal = false;
                    }
                    """);

            assertThat(node("com.acme.Status").fields()).extracting(Field::name).containsExactly("terminal");
        }

        @Test
        @DisplayName("reads field annotations with their values")
        void readsFieldAnnotations() {
            SourceFixtures.write(
                    sources, "com/acme/Column.java", "package com.acme; public @interface Column { String name(); }");
            SourceFixtures.write(
                    sources,
                    "com/acme/Order.java",
                    "package com.acme; public class Order { @Column(name = \"total\") private int total; }");

            Field total = node("com.acme.Order").fields().get(0);
            assertThat(total.hasAnnotation("com.acme.Column")).isTrue();
            assertThat(total.annotations().get(0).value("name")).isPresent();
        }

        @Test
        @DisplayName("states facts only: semantic roles are left to the engine")
        void statesFactsOnly() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Order.java",
                    "package com.acme; import java.util.List; public class Order { private OrderId id; private List<Line> lines; }");

            TypeNode order = node("com.acme.Order");
            assertThat(order.fields()).allSatisfy(field -> {
                assertThat(field.roles()).isEmpty();
                assertThat(field.wrappedType()).isEmpty();
                assertThat(field.elementType()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("methods")
    class Methods {

        @Test
        @DisplayName("reads declared methods with their signature and documentation")
        void readsDeclaredMethods() {
            SourceFixtures.write(sources, "com/acme/OrderRepository.java", """
                    package com.acme;
                    import java.util.Optional;
                    public interface OrderRepository {
                        /** Finds one order. */
                        Optional<Order> findById(OrderId id) throws java.io.IOException;
                        void save(Order order);
                    }
                    """);

            TypeNode repository = node("com.acme.OrderRepository");

            assertThat(repository.methods()).extracting(Method::name).containsExactly("findById", "save");
            Method findById = repository.methods().get(0);
            assertThat(findById.returnType().toDisplayString()).isEqualTo("java.util.Optional<com.acme.Order>");
            assertThat(findById.parameters()).containsExactly(Parameter.of("id", TypeRef.of("com.acme.OrderId")));
            assertThat(findById.thrownExceptions()).containsExactly(TypeRef.of("java.io.IOException"));
            assertThat(findById.documentation()).contains("Finds one order.");
            assertThat(findById.sourceLocation()).isPresent();
            assertThat(findById.roles()).isEmpty();
            assertThat(findById.cyclomaticComplexity()).isEmpty();
        }

        @Test
        @DisplayName("reads the default modifier of an interface method")
        void readsDefaultModifier() {
            SourceFixtures.write(sources, "com/acme/Repo.java", """
                    package com.acme;
                    public interface Repo {
                        void save(Order order);
                        default boolean isEmpty() { return true; }
                    }
                    """);

            TypeNode repo = node("com.acme.Repo");
            assertThat(repo.methods().get(0).modifiers()).contains(Modifier.DEFAULT);
            assertThat(repo.methods().get(1).modifiers()).doesNotContain(Modifier.DEFAULT);
        }

        @Test
        @DisplayName("orders methods deterministically by signature")
        void ordersMethodsBySignature() {
            SourceFixtures.write(sources, "com/acme/Service.java", """
                    package com.acme;
                    public class Service {
                        public void zeta() {}
                        public void alpha(int second) {}
                        public void alpha() {}
                    }
                    """);

            assertThat(node("com.acme.Service").methods())
                    .extracting(Method::signature)
                    .containsExactly("alpha()", "alpha(int)", "zeta()");
        }

        @Test
        @DisplayName("keeps parameter annotations")
        void keepsParameterAnnotations() {
            SourceFixtures.write(sources, "com/acme/Valid.java", "package com.acme; public @interface Valid {}");
            SourceFixtures.write(
                    sources,
                    "com/acme/Service.java",
                    "package com.acme; public class Service { public void save(@Valid Order order) {} }");

            Parameter parameter =
                    node("com.acme.Service").methods().get(0).parameters().get(0);
            assertThat(parameter.hasAnnotation("com.acme.Valid")).isTrue();
        }
    }

    @Nested
    @DisplayName("constructors")
    class Constructors {

        @Test
        @DisplayName("reads declared constructors with their parameters")
        void readsDeclaredConstructors() {
            SourceFixtures.write(
                    sources, "com/acme/OrderId.java", "package com.acme; public record OrderId(String value) {}");
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    public class Order {
                        private final OrderId id;
                        public Order(OrderId id) { this.id = id; }
                        Order() { this(null); }
                    }
                    """);

            assertThat(node("com.acme.Order").constructors())
                    .extracting(Constructor::signature)
                    .containsExactly("()", "(OrderId)");
        }

        @Test
        @DisplayName("reports only what the source declares, not what the language implies")
        void reportsOnlyDeclaredMembers() {
            SourceFixtures.write(
                    sources, "com/acme/OrderId.java", "package com.acme; public record OrderId(String value) {}");

            TypeNode orderId = node("com.acme.OrderId");
            assertThat(orderId.constructors()).isEmpty();
            assertThat(orderId.methods()).isEmpty();
            assertThat(orderId.fields()).extracting(Field::name).containsExactly("value");
        }

        @Test
        @DisplayName("has no constructor on an interface")
        void hasNoConstructorOnInterface() {
            SourceFixtures.write(sources, "com/acme/Repo.java", "package com.acme; public interface Repo {}");

            assertThat(node("com.acme.Repo").constructors()).isEmpty();
        }
    }
}
