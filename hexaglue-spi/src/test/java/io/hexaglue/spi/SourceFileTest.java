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

package io.hexaglue.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A generated type is named, never placed. What keeps a backend from writing outside the directory
 * a host chose for it is therefore not a check on a path — it is that a plugin has no path to
 * give.
 */
class SourceFileTest {

    @Nested
    @DisplayName("says where it goes")
    class SaysWhereItGoes {

        @Test
        @DisplayName("deriving the path from the names, never taking one")
        void derivingThePathFromTheNames() {
            SourceFile source = SourceFile.of("com.acme.shop", "OrderEntity", "class OrderEntity {}");

            assertThat(source.path()).isEqualTo("com/acme/shop/OrderEntity.java");
            assertThat(source.qualifiedName()).isEqualTo("com.acme.shop.OrderEntity");
        }

        @Test
        @DisplayName("and naming the module it belongs in, when the plugin routes it")
        void andNamingTheModuleItBelongsIn() {
            SourceFile routed =
                    SourceFile.of("com.acme.shop", "OrderEntity", "class X {}").in("shop-persistence");

            assertThat(routed.module()).contains("shop-persistence");
            assertThat(SourceFile.of("com.acme.shop", "OrderEntity", "class X {}")
                            .module())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("refuses a name that is not one")
    class RefusesANameThatIsNotOne {

        /**
         * None of these is screened out as a special case: a path separator, a parent step and a
         * drive letter are simply not Java identifiers, which is the only thing being asked.
         */
        @ParameterizedTest
        @ValueSource(
                strings = {"..", "com/acme", "com..acme", "com.acme.", "", " ", "com.acme-shop", "C:", "com.2acme"})
        @DisplayName("in the package")
        void inThePackage(String packageName) {
            assertThatThrownBy(() -> SourceFile.of(packageName, "OrderEntity", "class X {}"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"..", "Order/Entity", "Order.Entity", "", " ", "Order Entity", "1Order", "-Order"})
        @DisplayName("and in the type")
        void andInTheType(String typeName) {
            assertThatThrownBy(() -> SourceFile.of("com.acme", typeName, "class X {}"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("and a module stated as nothing at all")
        void andAModuleStatedAsNothing() {
            assertThatThrownBy(() -> SourceFile.of("com.acme", "OrderEntity", "class X {}")
                            .in(" "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("while letting through what Java itself allows")
        void whileLettingThroughWhatJavaAllows() {
            assertThat(SourceFile.of("com.acme.shop_2", "Order$Entity", "class X {}")
                            .path())
                    .isEqualTo("com/acme/shop_2/Order$Entity.java");
        }
    }
}
