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

package io.hexaglue.model.declaration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.TypeRef;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnnotationTest {

    @Nested
    @DisplayName("Annotation uses")
    class AnnotationUses {

        @Test
        @DisplayName("keeps typed attribute values readable")
        void keepsTypedValuesReadable() {
            Annotation table = Annotation.of(
                    "jakarta.persistence.Table", Map.of("name", AnnotationValue.ofString("purchase_order")));

            assertThat(table.hasValue("name")).isTrue();
            assertThat(table.value("name"))
                    .containsInstanceOf(AnnotationValue.StringValue.class)
                    .map(value -> ((AnnotationValue.StringValue) value).value())
                    .contains("purchase_order");
        }

        @Test
        @DisplayName("attributes iterate in name order")
        void attributesIterateInNameOrder() {
            Annotation annotation = Annotation.of(
                    "com.a.Config",
                    Map.of(
                            "zeta", AnnotationValue.ofString("z"),
                            "alpha", AnnotationValue.ofString("a"),
                            "mid", AnnotationValue.ofString("m")));

            assertThat(annotation.values().keySet()).containsExactly("alpha", "mid", "zeta");
        }

        @Test
        @DisplayName("simple name is derived from the qualified name")
        void simpleNameIsDerived() {
            assertThat(Annotation.of("jakarta.persistence.Entity").simpleName()).isEqualTo("Entity");
        }

        @Test
        @DisplayName("is() matches on the exact qualified name only")
        void isMatchesExactQualifiedName() {
            Annotation entity = Annotation.of("jakarta.persistence.Entity");

            assertThat(entity.is("jakarta.persistence.Entity")).isTrue();
            assertThat(entity.is("org.jmolecules.ddd.annotation.Entity")).isFalse();
        }

        @Test
        @DisplayName("a blank qualified name is rejected")
        void blankQualifiedNameIsRejected() {
            assertThatIllegalArgumentException().isThrownBy(() -> Annotation.of(" "));
        }

        @Test
        @DisplayName("an absent attribute answers empty")
        void absentAttributeAnswersEmpty() {
            assertThat(Annotation.of("com.a.Marker").value("missing")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Typed values")
    class TypedValues {

        @Test
        @DisplayName("primitive values expose typed accessors")
        void primitiveValuesExposeTypedAccessors() {
            AnnotationValue.PrimitiveValue intValue = new AnnotationValue.PrimitiveValue(42);
            AnnotationValue.PrimitiveValue boolValue = new AnnotationValue.PrimitiveValue(true);
            AnnotationValue.PrimitiveValue charValue = new AnnotationValue.PrimitiveValue('x');

            assertThat(intValue.asInt()).isEqualTo(42);
            assertThat(intValue.asLong()).isEqualTo(42L);
            assertThat(intValue.asDouble()).isEqualTo(42.0);
            assertThat(boolValue.asBoolean()).isTrue();
            assertThat(charValue.asChar()).isEqualTo('x');
            assertThat(intValue.kind()).isEqualTo(AnnotationValue.Kind.PRIMITIVE);
        }

        @Test
        @DisplayName("a non-primitive payload is rejected")
        void nonPrimitivePayloadIsRejected() {
            assertThatIllegalArgumentException().isThrownBy(() -> AnnotationValue.ofPrimitive(new Object()));
        }

        @Test
        @DisplayName("enum values keep type and constant")
        void enumValuesKeepTypeAndConstant() {
            AnnotationValue.EnumValue value =
                    (AnnotationValue.EnumValue) AnnotationValue.ofEnum("jakarta.persistence.FetchType", "LAZY");

            assertThat(value.enumType()).isEqualTo("jakarta.persistence.FetchType");
            assertThat(value.constantName()).isEqualTo("LAZY");
            assertThat(value.kind()).isEqualTo(AnnotationValue.Kind.ENUM);
        }

        @Test
        @DisplayName("class values expose the referenced type")
        void classValuesExposeReferencedType() {
            AnnotationValue.ClassValue value =
                    (AnnotationValue.ClassValue) AnnotationValue.ofClass(TypeRef.of("com.a.Converter"));

            assertThat(value.qualifiedName()).isEqualTo("com.a.Converter");
            assertThat(value.kind()).isEqualTo(AnnotationValue.Kind.CLASS);
        }

        @Test
        @DisplayName("nested annotations stay typed recursively")
        void nestedAnnotationsStayTyped() {
            Annotation index =
                    Annotation.of("jakarta.persistence.Index", Map.of("name", AnnotationValue.ofString("idx")));
            AnnotationValue.NestedAnnotationValue nested =
                    (AnnotationValue.NestedAnnotationValue) AnnotationValue.ofAnnotation(index);

            assertThat(nested.annotation().value("name")).isPresent();
            assertThat(nested.kind()).isEqualTo(AnnotationValue.Kind.ANNOTATION);
        }

        @Test
        @DisplayName("arrays keep declaration order and extract strings")
        void arraysKeepOrderAndExtractStrings() {
            AnnotationValue.ArrayValue array = (AnnotationValue.ArrayValue) AnnotationValue.ofArray(List.of(
                    AnnotationValue.ofString("first"),
                    AnnotationValue.ofPrimitive(2),
                    AnnotationValue.ofString("second")));

            assertThat(array.values()).hasSize(3);
            assertThat(array.asStrings()).containsExactly("first", "second");
            assertThat(array.kind()).isEqualTo(AnnotationValue.Kind.ARRAY);
        }

        @Test
        @DisplayName("a blank enum constant is rejected")
        void blankEnumConstantIsRejected() {
            assertThatIllegalArgumentException().isThrownBy(() -> AnnotationValue.ofEnum("com.a.E", " "));
        }
    }
}
