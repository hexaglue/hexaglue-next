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

package io.hexaglue.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TypeRefTest {

    @Nested
    @DisplayName("Factories")
    class Factories {

        @Test
        @DisplayName("of creates a named reference")
        void ofCreatesNamedReference() {
            TypeRef ref = TypeRef.of("com.example.Order");

            assertThat(ref).isInstanceOf(TypeRef.Named.class);
            assertThat(ref.qualifiedName()).isEqualTo("com.example.Order");
            assertThat(ref.typeArguments()).isEmpty();
        }

        @Test
        @DisplayName("of recognizes primitives")
        void ofRecognizesPrimitives() {
            TypeRef ref = TypeRef.of("int");

            assertThat(ref).isInstanceOf(TypeRef.Primitive.class);
            assertThat(ref.isPrimitive()).isTrue();
        }

        @Test
        @DisplayName("of recognizes void as a primitive")
        void ofRecognizesVoid() {
            assertThat(TypeRef.of("void").isPrimitive()).isTrue();
        }

        @Test
        @DisplayName("parameterized carries its type arguments in order")
        void parameterizedCarriesArguments() {
            TypeRef ref = TypeRef.parameterized("java.util.Map", TypeRef.of("com.a.Key"), TypeRef.of("com.a.Val"));

            assertThat(ref.isParameterized()).isTrue();
            assertThat(ref.typeArguments())
                    .extracting(TypeRef::qualifiedName)
                    .containsExactly("com.a.Key", "com.a.Val");
        }

        @Test
        @DisplayName("parameterized requires at least one argument")
        void parameterizedRequiresArguments() {
            assertThatIllegalArgumentException().isThrownBy(() -> TypeRef.parameterized("java.util.List"));
        }

        @Test
        @DisplayName("array wraps a component with dimensions")
        void arrayWrapsComponent() {
            TypeRef ref = TypeRef.array(TypeRef.of("int"), 2);

            assertThat(ref.isArray()).isTrue();
            assertThat(ref.qualifiedName()).isEqualTo("int");
        }

        @Test
        @DisplayName("array requires at least one dimension")
        void arrayRequiresDimension() {
            assertThatIllegalArgumentException().isThrownBy(() -> TypeRef.array(TypeRef.of("int"), 0));
        }

        @Test
        @DisplayName("blank qualified name is rejected")
        void blankQualifiedNameIsRejected() {
            assertThatIllegalArgumentException().isThrownBy(() -> TypeRef.of(" "));
        }
    }

    @Nested
    @DisplayName("Wildcards and type variables are distinct")
    class WildcardsAndTypeVariables {

        @Test
        @DisplayName("an unbounded wildcard is not a type variable")
        void unboundedWildcardIsNotTypeVariable() {
            TypeRef wildcard = TypeRef.wildcard();
            TypeRef variable = TypeRef.typeVariable("T");

            assertThat(wildcard).isInstanceOf(TypeRef.Wildcard.class);
            assertThat(variable).isInstanceOf(TypeRef.TypeVariable.class);
            assertThat(wildcard).isNotEqualTo(variable);
        }

        @Test
        @DisplayName("an upper-bounded wildcard keeps its bound")
        void upperBoundedWildcardKeepsBound() {
            TypeRef.Wildcard ref = (TypeRef.Wildcard) TypeRef.wildcardExtends(TypeRef.of("com.example.Shape"));

            assertThat(ref.upperBound()).map(TypeRef::qualifiedName).contains("com.example.Shape");
            assertThat(ref.lowerBound()).isEmpty();
        }

        @Test
        @DisplayName("a lower-bounded wildcard keeps its bound")
        void lowerBoundedWildcardKeepsBound() {
            TypeRef.Wildcard ref = (TypeRef.Wildcard) TypeRef.wildcardSuper(TypeRef.of("com.example.Shape"));

            assertThat(ref.lowerBound()).map(TypeRef::qualifiedName).contains("com.example.Shape");
            assertThat(ref.upperBound()).isEmpty();
        }

        @Test
        @DisplayName("a wildcard cannot have both bounds")
        void wildcardCannotHaveBothBounds() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new TypeRef.Wildcard(
                            java.util.Optional.of(TypeRef.of("com.a.Upper")),
                            java.util.Optional.of(TypeRef.of("com.a.Lower"))));
        }
    }

    @Nested
    @DisplayName("Name decomposition")
    class NameDecomposition {

        @Test
        @DisplayName("simple name is the last segment")
        void simpleNameIsLastSegment() {
            assertThat(TypeRef.of("com.example.Order").simpleName()).isEqualTo("Order");
        }

        @Test
        @DisplayName("simple name of a nested type is the innermost name")
        void simpleNameOfNestedType() {
            assertThat(TypeRef.of("com.example.Order$OrderLine").simpleName()).isEqualTo("OrderLine");
        }

        @Test
        @DisplayName("package name of a named reference")
        void packageNameOfNamedReference() {
            assertThat(TypeRef.of("com.example.Order").packageName()).isEqualTo("com.example");
        }

        @Test
        @DisplayName("package name of a primitive is empty")
        void packageNameOfPrimitiveIsEmpty() {
            assertThat(TypeRef.of("long").packageName()).isEmpty();
        }

        @Test
        @DisplayName("a type variable answers its name")
        void typeVariableAnswersItsName() {
            assertThat(TypeRef.typeVariable("T").qualifiedName()).isEqualTo("T");
        }

        @Test
        @DisplayName("a wildcard answers the placeholder as qualified name")
        void wildcardAnswersPlaceholder() {
            assertThat(TypeRef.wildcard().qualifiedName()).isEqualTo("?");
        }
    }

    @Nested
    @DisplayName("JDK container recognition")
    class JdkContainers {

        @Test
        @DisplayName("Optional is optional-like")
        void optionalIsOptionalLike() {
            assertThat(TypeRef.parameterized("java.util.Optional", TypeRef.of("com.a.Order"))
                            .isOptionalLike())
                    .isTrue();
        }

        @Test
        @DisplayName("List and Set are collection-like")
        void listAndSetAreCollectionLike() {
            assertThat(TypeRef.parameterized("java.util.List", TypeRef.of("com.a.Order"))
                            .isCollectionLike())
                    .isTrue();
            assertThat(TypeRef.parameterized("java.util.Set", TypeRef.of("com.a.Order"))
                            .isCollectionLike())
                    .isTrue();
        }

        @Test
        @DisplayName("arrays are collection-like")
        void arraysAreCollectionLike() {
            assertThat(TypeRef.array(TypeRef.of("com.a.Order"), 1).isCollectionLike())
                    .isTrue();
        }

        @Test
        @DisplayName("Map is map-like but not collection-like")
        void mapIsMapLike() {
            TypeRef ref = TypeRef.parameterized("java.util.Map", TypeRef.of("com.a.K"), TypeRef.of("com.a.V"));

            assertThat(ref.isMapLike()).isTrue();
            assertThat(ref.isCollectionLike()).isFalse();
        }

        @Test
        @DisplayName("Stream is stream-like")
        void streamIsStreamLike() {
            assertThat(TypeRef.parameterized("java.util.stream.Stream", TypeRef.of("com.a.Order"))
                            .isStreamLike())
                    .isTrue();
        }

        @Test
        @DisplayName("a plain domain type is none of the containers")
        void plainTypeIsNoContainer() {
            TypeRef ref = TypeRef.of("com.example.Order");

            assertThat(ref.isOptionalLike()).isFalse();
            assertThat(ref.isCollectionLike()).isFalse();
            assertThat(ref.isMapLike()).isFalse();
            assertThat(ref.isStreamLike()).isFalse();
            assertThat(ref.isPrimitive()).isFalse();
            assertThat(ref.isParameterized()).isFalse();
        }
    }

    @Nested
    @DisplayName("Element unwrapping")
    class ElementUnwrapping {

        @Test
        @DisplayName("unwraps the element of a collection")
        void unwrapsCollectionElement() {
            TypeRef ref = TypeRef.parameterized("java.util.List", TypeRef.of("com.a.Order"));

            assertThat(ref.unwrapElement().qualifiedName()).isEqualTo("com.a.Order");
        }

        @Test
        @DisplayName("unwraps the element of an optional")
        void unwrapsOptionalElement() {
            TypeRef ref = TypeRef.parameterized("java.util.Optional", TypeRef.of("com.a.Order"));

            assertThat(ref.unwrapElement().qualifiedName()).isEqualTo("com.a.Order");
        }

        @Test
        @DisplayName("unwraps the component of an array")
        void unwrapsArrayComponent() {
            TypeRef component = TypeRef.of("com.a.Order");
            TypeRef ref = TypeRef.array(component, 1);

            assertThat(ref.unwrapElement()).isSameAs(component);
        }

        @Test
        @DisplayName("unwraps the element of a stream")
        void unwrapsStreamElement() {
            TypeRef ref = TypeRef.parameterized("java.util.stream.Stream", TypeRef.of("com.a.Order"));

            assertThat(ref.unwrapElement().qualifiedName()).isEqualTo("com.a.Order");
        }

        @Test
        @DisplayName("a parameterized map unwraps to itself: its element is ambiguous")
        void parameterizedMapUnwrapsToItself() {
            TypeRef ref = TypeRef.parameterized("java.util.Map", TypeRef.of("com.a.K"), TypeRef.of("com.a.V"));

            assertThat(ref.unwrapElement()).isSameAs(ref);
        }

        @Test
        @DisplayName("a non-container unwraps to itself")
        void nonContainerUnwrapsToItself() {
            TypeRef ref = TypeRef.of("com.example.Order");

            assertThat(ref.unwrapElement()).isSameAs(ref);
        }

        @Test
        @DisplayName("first argument is empty when not parameterized")
        void firstArgumentEmptyWhenNotParameterized() {
            assertThat(TypeRef.of("com.example.Order").firstArgument()).isEmpty();
        }

        @Test
        @DisplayName("first argument returns the first type argument")
        void firstArgumentReturnsFirst() {
            TypeRef ref = TypeRef.parameterized("java.util.List", TypeRef.of("com.a.Order"));

            assertThat(ref.firstArgument()).map(TypeRef::qualifiedName).contains("com.a.Order");
        }
    }

    @Nested
    @DisplayName("Display")
    class Display {

        @Test
        @DisplayName("a named reference displays its qualified name")
        void namedDisplaysQualifiedName() {
            assertThat(TypeRef.of("com.example.Order").toDisplayString()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("a parameterized reference displays its arguments recursively")
        void parameterizedDisplaysArguments() {
            TypeRef ref = TypeRef.parameterized(
                    "java.util.Map",
                    TypeRef.of("java.lang.String"),
                    TypeRef.parameterized("java.util.List", TypeRef.of("com.a.Order")));

            assertThat(ref.toDisplayString()).isEqualTo("java.util.Map<java.lang.String, java.util.List<com.a.Order>>");
        }

        @Test
        @DisplayName("an array displays its dimensions")
        void arrayDisplaysDimensions() {
            assertThat(TypeRef.array(TypeRef.of("int"), 2).toDisplayString()).isEqualTo("int[][]");
        }

        @Test
        @DisplayName("wildcards display their bounds")
        void wildcardsDisplayBounds() {
            assertThat(TypeRef.wildcard().toDisplayString()).isEqualTo("?");
            assertThat(TypeRef.wildcardExtends(TypeRef.of("com.a.Shape")).toDisplayString())
                    .isEqualTo("? extends com.a.Shape");
            assertThat(TypeRef.wildcardSuper(TypeRef.of("com.a.Shape")).toDisplayString())
                    .isEqualTo("? super com.a.Shape");
        }

        @Test
        @DisplayName("a type variable displays its name")
        void typeVariableDisplaysName() {
            assertThat(TypeRef.typeVariable("T").toDisplayString()).isEqualTo("T");
        }
    }
}
