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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TypeIdTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("of creates an id from a qualified name")
        void ofCreatesIdFromQualifiedName() {
            TypeId id = TypeId.of("com.example.Order");

            assertThat(id.qualifiedName()).isEqualTo("com.example.Order");
        }

        @Test
        @DisplayName("null qualified name is rejected")
        void nullQualifiedNameIsRejected() {
            assertThatNullPointerException().isThrownBy(() -> TypeId.of(null));
        }

        @Test
        @DisplayName("blank qualified name is rejected")
        void blankQualifiedNameIsRejected() {
            assertThatIllegalArgumentException().isThrownBy(() -> TypeId.of("  "));
        }
    }

    @Nested
    @DisplayName("Name decomposition")
    class NameDecomposition {

        @Test
        @DisplayName("simple name is the last segment")
        void simpleNameIsLastSegment() {
            assertThat(TypeId.of("com.example.Order").simpleName()).isEqualTo("Order");
        }

        @Test
        @DisplayName("simple name of a nested type is the innermost name")
        void simpleNameOfNestedType() {
            assertThat(TypeId.of("com.example.Order$OrderLine").simpleName()).isEqualTo("OrderLine");
        }

        @Test
        @DisplayName("simple name of an unpackaged type is the name itself")
        void simpleNameWithoutPackage() {
            assertThat(TypeId.of("Order").simpleName()).isEqualTo("Order");
        }

        @Test
        @DisplayName("package name is everything before the top-level type")
        void packageNameIsPrefix() {
            assertThat(TypeId.of("com.example.Order").packageName()).isEqualTo("com.example");
        }

        @Test
        @DisplayName("package name of a nested type is the package of its top-level type")
        void packageNameOfNestedType() {
            assertThat(TypeId.of("com.example.Order$OrderLine").packageName()).isEqualTo("com.example");
        }

        @Test
        @DisplayName("package name of an unpackaged type is empty")
        void packageNameWithoutPackage() {
            assertThat(TypeId.of("Order").packageName()).isEmpty();
        }

        @Test
        @DisplayName("a nested id knows it is nested")
        void nestedIdKnowsItIsNested() {
            assertThat(TypeId.of("com.example.Order$OrderLine").isNested()).isTrue();
            assertThat(TypeId.of("com.example.Order").isNested()).isFalse();
        }
    }

    @Nested
    @DisplayName("Identity and ordering")
    class IdentityAndOrdering {

        @Test
        @DisplayName("equality is by qualified name")
        void equalityByQualifiedName() {
            assertThat(TypeId.of("com.example.Order")).isEqualTo(TypeId.of("com.example.Order"));
        }

        @Test
        @DisplayName("ordering is lexicographic on qualified names")
        void orderingIsLexicographic() {
            assertThat(TypeId.of("com.a.First")).isLessThan(TypeId.of("com.b.Second"));
        }

        @Test
        @DisplayName("toString is the qualified name")
        void stringFormIsQualifiedName() {
            assertThat(TypeId.of("com.example.Order")).hasToString("com.example.Order");
        }
    }
}
