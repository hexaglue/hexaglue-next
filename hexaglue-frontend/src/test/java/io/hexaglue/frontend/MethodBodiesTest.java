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
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.CodeModelCapability;
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.MethodBodyFacts;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What a method body does is expensive to read and useful only to some rules, so it is extracted
 * on demand — and when it is, in one pass rather than one pass per question asked.
 */
class MethodBodiesTest {

    @TempDir
    Path sources;

    @BeforeEach
    void writeService() {
        SourceFixtures.write(sources, "com/acme/OrderId.java", "package com.acme; public record OrderId(String v) {}");
        SourceFixtures.write(sources, "com/acme/OrderService.java", """
                package com.acme;
                import java.util.ArrayList;
                import java.util.List;
                public class OrderService {
                    private final List<OrderId> handled;
                    public OrderService() {
                        handled = new ArrayList<>();
                        handled.add(new OrderId("seed"));
                    }
                    public String describe(OrderId id, boolean verbose) {
                        if (verbose && id != null) {
                            return id.toString();
                        }
                        for (OrderId handledId : handled) {
                            handledId.hashCode();
                        }
                        return "";
                    }
                }
                """);
    }

    private CodeModel analyze(boolean withBodies) {
        FrontendRequest.Builder request = FrontendRequest.builder().sourceRoot(sources);
        if (withBodies) {
            request.capability(CodeModelCapability.METHOD_BODIES);
        }
        return SpoonFrontend.analyze(request.build());
    }

    private Method methodOf(CodeModel model, String name) {
        return model.type(TypeId.of("com.acme.OrderService")).orElseThrow().methods().stream()
                .filter(method -> name.equals(method.name()))
                .findFirst()
                .orElseThrow();
    }

    @Nested
    @DisplayName("without the capability")
    class WithoutTheCapability {

        @Test
        @DisplayName("extracts nothing from bodies and says so")
        void extractsNothingFromBodies() {
            CodeModel model = analyze(false);

            assertThat(model.capabilities()).isEmpty();
            assertThat(model.bodyFacts()).isEmpty();
            assertThat(methodOf(model, "describe").cyclomaticComplexity()).isEmpty();
            assertThat(model.edges()).extracting(Edge::kind).doesNotContain(EdgeKind.INVOKES, EdgeKind.INSTANTIATES);
        }
    }

    @Nested
    @DisplayName("with the capability")
    class WithTheCapability {

        @Test
        @DisplayName("records the capability it ran with")
        void recordsTheCapability() {
            assertThat(analyze(true).capabilities()).containsExactly(CodeModelCapability.METHOD_BODIES);
        }

        @Test
        @DisplayName("reads what a body invokes and instantiates")
        void readsInvocationsAndInstantiations() {
            CodeModel model = analyze(true);

            MethodBodyFacts describe = model.bodyFacts().stream()
                    .filter(facts -> "describe".equals(facts.methodName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(describe.declaringType()).isEqualTo(TypeId.of("com.acme.OrderService"));
            assertThat(describe.invocations())
                    .extracting(MethodBodyFacts.Invocation::methodName)
                    .contains("toString", "hashCode");
            MethodBodyFacts constructor = model.bodyFacts().stream()
                    .filter(facts -> "<init>".equals(facts.methodName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(constructor.instantiations())
                    .extracting(MethodBodyFacts.Instantiation::target)
                    .contains(TypeId.of("com.acme.OrderId"), TypeId.of("java.util.ArrayList"));
        }

        @Test
        @DisplayName("counts the decisions a body takes")
        void countsDecisions() {
            CodeModel model = analyze(true);

            // One path, plus the if, its && and the loop.
            assertThat(methodOf(model, "describe").cyclomaticComplexity()).hasValue(4);
        }

        @Test
        @DisplayName("counts every kind of branch a body can take")
        void countsEveryKindOfBranch() {
            SourceFixtures.write(sources, "com/acme/Branches.java", """
                    package com.acme;
                    public class Branches {
                        public int all(int input, boolean flag) {
                            int total = flag || input > 0 ? 1 : 0;
                            while (total < 2) {
                                total++;
                            }
                            do {
                                total--;
                            } while (total > 5);
                            for (int step = 0; step < 3; step++) {
                                total += step;
                            }
                            switch (input) {
                                case 1: total++; break;
                                default: break;
                            }
                            try {
                                total += input;
                            } catch (RuntimeException failure) {
                                total = 0;
                            }
                            return total;
                        }
                    }
                    """);

            CodeModel model = analyze(true);

            // One path, plus ||, the ternary, while, do, for, the case, the default case and the catch.
            assertThat(model.type(TypeId.of("com.acme.Branches"))
                            .orElseThrow()
                            .methods()
                            .get(0)
                            .cyclomaticComplexity())
                    .hasValue(9);
        }

        @Test
        @DisplayName("records body relations with the member they come from")
        void recordsBodyRelationsWithProvenance() {
            CodeModel model = analyze(true);

            List<String> edges = model.edgesFrom(TypeId.of("com.acme.OrderService")).stream()
                    .map(Edge::toDisplayString)
                    .toList();
            assertThat(edges)
                    .contains(
                            "com.acme.OrderService -INSTANTIATES-> com.acme.OrderId @<init>",
                            "com.acme.OrderService -INSTANTIATES-> java.util.ArrayList @<init>",
                            "com.acme.OrderService -INVOKES-> com.acme.OrderId @describe");
        }

        @Test
        @DisplayName("stubs a classpath type reached only from a body")
        void stubsClasspathTypesReachedFromBodies() {
            CodeModel model = analyze(true);

            assertThat(model.type(TypeId.of("java.util.ArrayList"))
                            .orElseThrow()
                            .external())
                    .isTrue();
        }

        @Test
        @DisplayName("reads the same bodies on repeated runs")
        void readsTheSameBodiesOnRepeatedRuns() {
            assertThat(analyze(true).bodyFacts()).isEqualTo(analyze(true).bodyFacts());
        }
    }
}
