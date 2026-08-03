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

package io.hexaglue.model.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.model.arch.ModuleRole;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModulesConfigTest {

    @Nested
    @DisplayName("Shape")
    class Shape {

        @Test
        @DisplayName("a module carries the role the project declared for it")
        void carriesTheDeclaredRole() {
            ModulesConfig config = new ModulesConfig(Map.of("shop-domain", ModuleRole.DOMAIN));

            assertThat(config.roleOf("shop-domain")).contains(ModuleRole.DOMAIN);
        }

        @Test
        @DisplayName("a module the project said nothing about has no role")
        void hasNoRoleWhenNothingWasSaid() {
            ModulesConfig config = new ModulesConfig(Map.of("shop-domain", ModuleRole.DOMAIN));

            assertThat(config.roleOf("shop-infra")).isEmpty();
        }

        @Test
        @DisplayName("the declarations read in name order, whatever order they were stated in")
        void readsInNameOrder() {
            Map<String, ModuleRole> stated = new LinkedHashMap<>();
            stated.put("shop-infra", ModuleRole.INFRASTRUCTURE);
            stated.put("shop-domain", ModuleRole.DOMAIN);

            ModulesConfig config = new ModulesConfig(stated);

            assertThat(config.roles())
                    .containsExactly(
                            Map.entry("shop-domain", ModuleRole.DOMAIN),
                            Map.entry("shop-infra", ModuleRole.INFRASTRUCTURE));
        }

        @Test
        @DisplayName("a role stated for a blank module name names nothing and is refused")
        void refusesABlankModuleName() {
            assertThatThrownBy(() -> new ModulesConfig(Map.of(" ", ModuleRole.DOMAIN)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }
    }

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("the default posture declares no role, which is what a single-module project states")
        void defaultsToNothingDeclared() {
            assertThat(ModulesConfig.defaults().roles()).isEmpty();
        }

        @Test
        @DisplayName("the fifth block of the configuration is the role channel")
        void isTheFifthBlockOfTheConfiguration() {
            assertThat(HexaGlueConfig.defaults().modules().roles()).isEmpty();
        }
    }
}
