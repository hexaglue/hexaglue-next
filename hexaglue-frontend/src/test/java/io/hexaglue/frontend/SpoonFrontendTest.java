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
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpoonFrontendTest {

    @TempDir
    Path sources;

    private CodeModel analyze() {
        return SpoonFrontend.analyze(FrontendRequest.of(sources)).code();
    }

    private CodeModel analyze(AnalysisScope scope) {
        return SpoonFrontend.analyze(FrontendRequest.builder()
                        .sourceRoot(sources)
                        .scope(scope)
                        .build())
                .code();
    }

    /** The types read from source, leaving out the stubs standing for classpath types. */
    private List<TypeId> analyzedIds(CodeModel model) {
        return model.types().stream()
                .filter(node -> !node.external())
                .map(TypeNode::id)
                .toList();
    }

    private TypeNode node(CodeModel model, String qualifiedName) {
        return model.type(TypeId.of(qualifiedName))
                .orElseThrow(() -> new AssertionError("no node for " + qualifiedName + ", model holds "
                        + model.types().stream().map(TypeNode::id).toList()));
    }

    @Nested
    @DisplayName("type discovery")
    class TypeDiscovery {

        @Test
        @DisplayName("analyzes the top-level types of every source root")
        void analyzesTopLevelTypes() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(
                    sources, "com/acme/OrderId.java", "package com.acme; public record OrderId(String v) {}");

            CodeModel model = analyze();

            assertThat(analyzedIds(model)).containsExactly(TypeId.of("com.acme.Order"), TypeId.of("com.acme.OrderId"));
        }

        @Test
        @DisplayName("analyzes nested types and links them to their enclosing type")
        void analyzesNestedTypes() {
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    public class Order {
                        public static class Line {
                            public static class Detail {}
                        }
                        public enum Status { NEW }
                    }
                    """);

            CodeModel model = analyze();

            assertThat(analyzedIds(model))
                    .containsExactly(
                            TypeId.of("com.acme.Order"),
                            TypeId.of("com.acme.Order$Line"),
                            TypeId.of("com.acme.Order$Line$Detail"),
                            TypeId.of("com.acme.Order$Status"));
            TypeNode line = node(model, "com.acme.Order$Line");
            assertThat(line.isNested()).isTrue();
            assertThat(line.enclosingType()).contains(TypeId.of("com.acme.Order"));
            assertThat(node(model, "com.acme.Order").enclosingType()).isEmpty();
        }

        @Test
        @DisplayName("ignores anonymous and local classes")
        void ignoresAnonymousAndLocalClasses() {
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    public class Order {
                        Runnable r = new Runnable() { public void run() {} };
                        void go() {
                            class Local {}
                            new Local();
                        }
                    }
                    """);

            CodeModel model = analyze();

            assertThat(analyzedIds(model)).containsExactly(TypeId.of("com.acme.Order"));
        }

        @Test
        @DisplayName("produces no analyzed type when the source root is empty")
        void producesNoTypeWhenSourceRootIsEmpty() {
            assertThat(analyzedIds(analyze())).isEmpty();
        }
    }

    @Nested
    @DisplayName("type shape")
    class TypeShape {

        @Test
        @DisplayName("reads the Java form of every declaration")
        void readsTheJavaForm() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "com/acme/Repo.java", "package com.acme; public interface Repo {}");
            SourceFixtures.write(
                    sources, "com/acme/OrderId.java", "package com.acme; public record OrderId(String v) {}");
            SourceFixtures.write(sources, "com/acme/Status.java", "package com.acme; public enum Status { NEW }");
            SourceFixtures.write(sources, "com/acme/Marker.java", "package com.acme; public @interface Marker {}");

            CodeModel model = analyze();

            assertThat(node(model, "com.acme.Order").nature()).isEqualTo(TypeNature.CLASS);
            assertThat(node(model, "com.acme.Repo").nature()).isEqualTo(TypeNature.INTERFACE);
            assertThat(node(model, "com.acme.OrderId").nature()).isEqualTo(TypeNature.RECORD);
            assertThat(node(model, "com.acme.Status").nature()).isEqualTo(TypeNature.ENUM);
            assertThat(node(model, "com.acme.Marker").nature()).isEqualTo(TypeNature.ANNOTATION);
        }

        @Test
        @DisplayName("reads declaration modifiers")
        void readsModifiers() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public final class Order {}");
            SourceFixtures.write(sources, "com/acme/Base.java", "package com.acme; abstract class Base {}");

            CodeModel model = analyze();

            assertThat(node(model, "com.acme.Order").modifiers()).containsExactly(Modifier.PUBLIC, Modifier.FINAL);
            assertThat(node(model, "com.acme.Base").modifiers()).containsExactly(Modifier.ABSTRACT);
        }

        @Test
        @DisplayName("reads the extended class, leaving implicit supertypes out")
        void readsTheExtendedClass() {
            SourceFixtures.write(sources, "com/acme/Base.java", "package com.acme; public class Base {}");
            SourceFixtures.write(
                    sources, "com/acme/Order.java", "package com.acme; public class Order extends Base {}");
            SourceFixtures.write(
                    sources, "com/acme/OrderId.java", "package com.acme; public record OrderId(String v) {}");
            SourceFixtures.write(sources, "com/acme/Status.java", "package com.acme; public enum Status { NEW }");

            CodeModel model = analyze();

            assertThat(node(model, "com.acme.Order").superClass()).contains(TypeRef.of("com.acme.Base"));
            assertThat(node(model, "com.acme.Base").superClass()).isEmpty();
            assertThat(node(model, "com.acme.OrderId").superClass()).isEmpty();
            assertThat(node(model, "com.acme.Status").superClass()).isEmpty();
        }

        @Test
        @DisplayName("reads implemented interfaces with their type arguments")
        void readsInterfacesWithTypeArguments() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Order.java",
                    "package com.acme; public class Order implements java.lang.Comparable<Order>, java.io.Serializable {}");

            CodeModel model = analyze();

            assertThat(node(model, "com.acme.Order").interfaces())
                    .extracting(TypeRef::toDisplayString)
                    .containsExactly("java.io.Serializable", "java.lang.Comparable<com.acme.Order>");
        }

        @Test
        @DisplayName("reads the permitted subtypes of a sealed type")
        void readsPermittedSubtypes() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Shape.java",
                    "package com.acme; public sealed interface Shape permits Circle, Square {}");
            SourceFixtures.write(
                    sources,
                    "com/acme/Circle.java",
                    "package com.acme; public record Circle(int r) implements Shape {}");
            SourceFixtures.write(
                    sources,
                    "com/acme/Square.java",
                    "package com.acme; public record Square(int s) implements Shape {}");

            CodeModel model = analyze();

            assertThat(node(model, "com.acme.Shape").permittedSubtypes())
                    .extracting(TypeRef::qualifiedName)
                    .containsExactly("com.acme.Circle", "com.acme.Square");
            assertThat(node(model, "com.acme.Shape").modifiers()).contains(Modifier.SEALED);
        }

        @Test
        @DisplayName("keeps the type variable of a generic supertype distinct from a wildcard")
        void keepsTypeVariablesDistinctFromWildcards() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Repo.java",
                    "package com.acme; public interface Repo<T> extends java.util.function.Supplier<T> {}");
            SourceFixtures.write(
                    sources,
                    "com/acme/Numbers.java",
                    "package com.acme; public interface Numbers extends java.util.function.Supplier<? extends Number> {}");

            CodeModel model = analyze();

            TypeRef generic = node(model, "com.acme.Repo").interfaces().get(0);
            assertThat(generic.typeArguments().get(0)).isInstanceOf(TypeRef.TypeVariable.class);
            TypeRef bounded = node(model, "com.acme.Numbers").interfaces().get(0);
            assertThat(bounded.typeArguments().get(0)).isInstanceOf(TypeRef.Wildcard.class);
            assertThat(bounded.toDisplayString()).isEqualTo("java.util.function.Supplier<? extends java.lang.Number>");
        }

        @Test
        @DisplayName("keeps array dimensions and lower-bounded wildcards")
        void keepsArraysAndLowerBoundedWildcards() {
            SourceFixtures.write(
                    sources,
                    "com/acme/Matrix.java",
                    "package com.acme; public interface Matrix extends java.util.function.Supplier<int[][]> {}");
            SourceFixtures.write(
                    sources,
                    "com/acme/Sink.java",
                    "package com.acme; public interface Sink extends java.util.function.Consumer<? super Number> {}");

            CodeModel model = analyze();

            assertThat(node(model, "com.acme.Matrix").interfaces().get(0).toDisplayString())
                    .isEqualTo("java.util.function.Supplier<int[][]>");
            assertThat(node(model, "com.acme.Sink").interfaces().get(0).toDisplayString())
                    .isEqualTo("java.util.function.Consumer<? super java.lang.Number>");
        }

        @Test
        @DisplayName("reads the documentation of a type")
        void readsDocumentation() {
            SourceFixtures.write(sources, "com/acme/Order.java", """
                    package com.acme;
                    /**
                     * A customer order.
                     *
                     * @since 1.0
                     */
                    public class Order {}
                    """);

            CodeModel model = analyze();

            assertThat(node(model, "com.acme.Order").documentation()).contains("A customer order.");
        }

        @Test
        @DisplayName("reports no documentation when a type carries none of substance")
        void reportsNoDocumentationWhenThereIsNone() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "com/acme/Line.java", """
                    package com.acme;
                    /**
                     * @since 1.0
                     */
                    public class Line {}
                    """);

            CodeModel model = analyze();

            assertThat(node(model, "com.acme.Order").documentation()).isEmpty();
            assertThat(node(model, "com.acme.Line").documentation()).isEmpty();
        }

        @Test
        @DisplayName("locates a type by a path relative to its source root")
        void locatesTypesRelativeToTheSourceRoot() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme;\npublic class Order {}\n");

            CodeModel model = analyze();

            Optional<SourceLocation> location = node(model, "com.acme.Order").sourceLocation();
            assertThat(location).map(SourceLocation::filePath).contains("com/acme/Order.java");
            assertThat(location).map(SourceLocation::lineStart).contains(2);
        }
    }

    @Nested
    @DisplayName("analysis perimeter")
    class Perimeter {

        @Test
        @DisplayName("keeps only the included package prefixes when any is given")
        void keepsOnlyIncludedPackages() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "org/other/Thing.java", "package org.other; public class Thing {}");

            CodeModel model = analyze(new AnalysisScope(Optional.empty(), List.of("com.acme"), List.of()));

            assertThat(analyzedIds(model)).containsExactly(TypeId.of("com.acme.Order"));
        }

        @Test
        @DisplayName("drops the excluded package prefixes")
        void dropsExcludedPackages() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(
                    sources, "com/acme/internal/Helper.java", "package com.acme.internal; public class Helper {}");

            CodeModel model = analyze(new AnalysisScope(Optional.empty(), List.of(), List.of("com.acme.internal")));

            assertThat(analyzedIds(model)).containsExactly(TypeId.of("com.acme.Order"));
        }

        @Test
        @DisplayName("matches package prefixes on whole segments only")
        void matchesWholePackageSegments() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "com/acmetools/Tool.java", "package com.acmetools; public class Tool {}");

            CodeModel model = analyze(new AnalysisScope(Optional.empty(), List.of("com.acme"), List.of()));

            assertThat(analyzedIds(model)).containsExactly(TypeId.of("com.acme.Order"));
        }

        @Test
        @DisplayName("does not restrict the perimeter by the base package alone")
        void basePackageAloneDoesNotRestrict() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "org/other/Thing.java", "package org.other; public class Thing {}");

            CodeModel model = analyze(new AnalysisScope(Optional.of("com.acme"), List.of(), List.of()));

            assertThat(analyzedIds(model)).hasSize(2);
        }

        @Test
        @DisplayName("leaves generated types out of the analyzed set")
        void leavesGeneratedTypesOut() {
            SourceFixtures.write(sources, "com/acme/OrderMapperImpl.java", """
                    package com.acme;
                    @javax.annotation.processing.Generated("org.mapstruct.ap.MappingProcessor")
                    public class OrderMapperImpl {}
                    """);
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");

            CodeModel model = analyze();

            assertThat(analyzedIds(model)).containsExactly(TypeId.of("com.acme.Order"));
        }

        @Test
        @DisplayName("leaves a nested type out when its enclosing type is out of the perimeter")
        void leavesNestedTypesOfExcludedTypesOut() {
            SourceFixtures.write(
                    sources,
                    "com/acme/internal/Helper.java",
                    "package com.acme.internal; public class Helper { public static class Inner {} }");

            CodeModel model = analyze(new AnalysisScope(Optional.empty(), List.of(), List.of("com.acme.internal")));

            assertThat(analyzedIds(model)).isEmpty();
        }
    }

    @Nested
    @DisplayName("determinism")
    class DeterministicOutput {

        @Test
        @DisplayName("renders the same type list on repeated runs")
        void rendersTheSameTypeListOnRepeatedRuns() {
            SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");
            SourceFixtures.write(sources, "com/acme/Zebra.java", "package com.acme; public class Zebra {}");
            SourceFixtures.write(sources, "com/acme/Alpha.java", "package com.acme; public class Alpha {}");

            List<TypeId> first = analyzedIds(analyze());
            List<TypeId> second = analyzedIds(analyze());

            assertThat(first)
                    .containsExactly(
                            TypeId.of("com.acme.Alpha"), TypeId.of("com.acme.Order"), TypeId.of("com.acme.Zebra"))
                    .isEqualTo(second);
        }
    }
}
