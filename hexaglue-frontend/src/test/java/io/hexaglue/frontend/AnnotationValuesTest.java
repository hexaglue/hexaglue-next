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
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.AnnotationValue;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reading annotation values is what makes framework knowledge usable: a rule that cannot read
 * {@code @Table(name = "orders")} cannot tell a persistence mapping from a domain intent.
 */
class AnnotationValuesTest {

    @TempDir
    Path sources;

    @BeforeEach
    void writeAnnotationTypes() {
        SourceFixtures.write(sources, "com/acme/meta/Table.java", """
                package com.acme.meta;
                public @interface Table {
                    String name();
                    boolean unique() default true;
                    int length() default 255;
                    Strategy strategy() default Strategy.AUTO;
                    Class<?> converter() default Object.class;
                    String[] columns() default {};
                    Index[] indexes() default {};
                }
                """);
        SourceFixtures.write(
                sources,
                "com/acme/meta/Index.java",
                "package com.acme.meta; public @interface Index { String name(); }");
        SourceFixtures.write(
                sources,
                "com/acme/meta/Strategy.java",
                "package com.acme.meta; public enum Strategy { AUTO, IDENTITY }");
        SourceFixtures.write(
                sources, "com/acme/meta/Audited.java", "package com.acme.meta; public @interface Audited {}");
    }

    private Annotation annotationOf(String typeName, String annotationQualifiedName) {
        CodeModel model = SpoonFrontend.analyze(FrontendRequest.of(sources));
        TypeNode node = model.type(TypeId.of(typeName)).orElseThrow();
        return node.annotations().stream()
                .filter(annotation -> annotation.is(annotationQualifiedName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + annotationQualifiedName + " on " + typeName));
    }

    private AnnotationValue valueOf(String attribute) {
        return annotationOf("com.acme.Order", "com.acme.meta.Table")
                .value(attribute)
                .orElseThrow(() -> new AssertionError("no attribute " + attribute));
    }

    private void writeOrderAnnotatedWith(String annotations) {
        SourceFixtures.write(
                sources,
                "com/acme/Order.java",
                "package com.acme;\nimport com.acme.meta.*;\n" + annotations + "\npublic class Order {}\n");
    }

    @Test
    @DisplayName("reads a string attribute without degrading it")
    void readsStringAttribute() {
        writeOrderAnnotatedWith("@Table(name = \"orders\")");

        assertThat(valueOf("name")).isEqualTo(AnnotationValue.ofString("orders"));
    }

    @Test
    @DisplayName("reads boolean and numeric attributes as typed primitives")
    void readsPrimitiveAttributes() {
        writeOrderAnnotatedWith("@Table(name = \"orders\", unique = false, length = 50)");

        assertThat(valueOf("unique")).isEqualTo(AnnotationValue.ofPrimitive(Boolean.FALSE));
        assertThat(valueOf("length")).isEqualTo(AnnotationValue.ofPrimitive(50));
    }

    @Test
    @DisplayName("reads an enum constant as its type and constant name")
    void readsEnumConstant() {
        writeOrderAnnotatedWith("@Table(name = \"orders\", strategy = Strategy.IDENTITY)");

        assertThat(valueOf("strategy")).isEqualTo(AnnotationValue.ofEnum("com.acme.meta.Strategy", "IDENTITY"));
    }

    @Test
    @DisplayName("reads a class literal as a type reference")
    void readsClassLiteral() {
        writeOrderAnnotatedWith("@Table(name = \"orders\", converter = String.class)");

        AnnotationValue converter = valueOf("converter");
        assertThat(converter.kind()).isEqualTo(AnnotationValue.Kind.CLASS);
        assertThat(((AnnotationValue.ClassRefValue) converter).qualifiedName()).isEqualTo("java.lang.String");
    }

    @Test
    @DisplayName("reads an array attribute element by element")
    void readsArrayAttribute() {
        writeOrderAnnotatedWith("@Table(name = \"orders\", columns = {\"id\", \"total\"})");

        assertThat(valueOf("columns"))
                .isEqualTo(AnnotationValue.ofArray(
                        List.of(AnnotationValue.ofString("id"), AnnotationValue.ofString("total"))));
    }

    @Test
    @DisplayName("reads nested annotations without flattening them to text")
    void readsNestedAnnotations() {
        writeOrderAnnotatedWith("@Table(name = \"orders\", indexes = {@Index(name = \"by_total\")})");

        AnnotationValue indexes = valueOf("indexes");
        assertThat(indexes.kind()).isEqualTo(AnnotationValue.Kind.ARRAY);
        AnnotationValue first = ((AnnotationValue.ArrayValue) indexes).values().get(0);
        assertThat(first.kind()).isEqualTo(AnnotationValue.Kind.ANNOTATION);
        Annotation nested = ((AnnotationValue.NestedAnnotationValue) first).annotation();
        assertThat(nested.is("com.acme.meta.Index")).isTrue();
        assertThat(nested.value("name")).contains(AnnotationValue.ofString("by_total"));
    }

    @Test
    @DisplayName("keeps every annotation of a declaration, in declaration order")
    void keepsEveryAnnotationInDeclarationOrder() {
        writeOrderAnnotatedWith("@Audited\n@Table(name = \"orders\")");

        CodeModel model = SpoonFrontend.analyze(FrontendRequest.of(sources));
        assertThat(model.type(TypeId.of("com.acme.Order")).orElseThrow().annotations())
                .extracting(Annotation::qualifiedName)
                .containsExactly("com.acme.meta.Audited", "com.acme.meta.Table");
    }

    @Test
    @DisplayName("reads an annotation without attributes as an empty value map")
    void readsAnnotationWithoutAttributes() {
        writeOrderAnnotatedWith("@Audited");

        assertThat(annotationOf("com.acme.Order", "com.acme.meta.Audited").values())
                .isEmpty();
    }

    @Test
    @DisplayName("folds a constant expression into the value it denotes")
    void foldsConstantExpressions() {
        writeOrderAnnotatedWith("@Table(name = \"orders\", length = 10 * 5)");

        assertThat(valueOf("length")).isEqualTo(AnnotationValue.ofPrimitive(50));
    }
}
