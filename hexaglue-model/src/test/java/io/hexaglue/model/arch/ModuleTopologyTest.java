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

package io.hexaglue.model.arch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.TypeId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModuleTopologyTest {

    @Nested
    @DisplayName("Lookups")
    class Lookups {

        @Test
        @DisplayName("modules are listed in declaration order")
        void modulesAreListedInDeclarationOrder() {
            assertThat(ShopModelFixtures.topology().modules())
                    .extracting(ModuleDescriptor::name)
                    .containsExactly("shop-domain", "shop-infra");
        }

        @Test
        @DisplayName("a module is found by name")
        void moduleIsFoundByName() {
            assertThat(ShopModelFixtures.topology().module("shop-domain"))
                    .map(ModuleDescriptor::role)
                    .contains(ModuleRole.DOMAIN);
        }

        @Test
        @DisplayName("an unknown module name answers empty")
        void unknownModuleNameAnswersEmpty() {
            assertThat(ShopModelFixtures.topology().module("shop-api")).isEmpty();
        }

        @Test
        @DisplayName("modules are filtered by role")
        void modulesAreFilteredByRole() {
            assertThat(ShopModelFixtures.topology().modulesByRole(ModuleRole.INFRASTRUCTURE))
                    .extracting(ModuleDescriptor::name)
                    .containsExactly("shop-infra");
        }

        @Test
        @DisplayName("the module of a type is resolved")
        void moduleOfTypeIsResolved() {
            assertThat(ShopModelFixtures.topology()
                            .moduleOf(ShopModelFixtures.ORDER)
                            .map(ModuleDescriptor::name))
                    .contains("shop-domain");
        }

        @Test
        @DisplayName("an unassigned type answers empty")
        void unassignedTypeAnswersEmpty() {
            assertThat(ShopModelFixtures.topology().moduleOf(TypeId.of("com.shop.Money")))
                    .isEmpty();
        }

        @Test
        @DisplayName("the types of a module are listed in identity order")
        void typesOfModuleAreListedInIdentityOrder() {
            assertThat(ShopModelFixtures.topology().typesInModule("shop-domain"))
                    .containsExactly(
                            ShopModelFixtures.CUSTOMER, ShopModelFixtures.ORDER, ShopModelFixtures.ORDER_REPOSITORY);
        }

        @Test
        @DisplayName("a module without assigned types answers an empty list")
        void moduleWithoutTypesAnswersEmptyList() {
            assertThat(ShopModelFixtures.topology().typesInModule("shop-infra")).isEmpty();
        }

        @Test
        @DisplayName("the empty topology has no modules and no assignments")
        void emptyTopologyHasNothing() {
            ModuleTopology empty = ModuleTopology.empty();

            assertThat(empty.isEmpty()).isTrue();
            assertThat(empty.size()).isZero();
            assertThat(empty.modules()).isEmpty();
            assertThat(empty.moduleOf(ShopModelFixtures.ORDER)).isEmpty();
        }

        @Test
        @DisplayName("a populated topology reports its size")
        void populatedTopologyReportsSize() {
            assertThat(ShopModelFixtures.topology().size()).isEqualTo(2);
            assertThat(ShopModelFixtures.topology().isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("a duplicate module name fails loudly")
        void duplicateModuleNameFailsLoudly() {
            ModuleTopology.Builder builder =
                    ModuleTopology.builder().addModule(ModuleDescriptor.of("shop-domain", ModuleRole.DOMAIN));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> builder.addModule(ModuleDescriptor.of("shop-domain", ModuleRole.SHARED)))
                    .withMessageContaining("duplicate module")
                    .withMessageContaining("shop-domain");
        }

        @Test
        @DisplayName("a type assigned twice fails loudly")
        void typeAssignedTwiceFailsLoudly() {
            ModuleTopology.Builder builder = ModuleTopology.builder()
                    .addModule(ModuleDescriptor.of("shop-domain", ModuleRole.DOMAIN))
                    .addModule(ModuleDescriptor.of("shop-infra", ModuleRole.INFRASTRUCTURE))
                    .assign(ShopModelFixtures.ORDER, "shop-domain");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> builder.assign(ShopModelFixtures.ORDER, "shop-infra"))
                    .withMessageContaining("already assigned")
                    .withMessageContaining("com.shop.Order");
        }

        @Test
        @DisplayName("an assignment to an unregistered module fails at build")
        void assignmentToUnregisteredModuleFailsAtBuild() {
            ModuleTopology.Builder builder = ModuleTopology.builder().assign(ShopModelFixtures.ORDER, "shop-domain");

            assertThatIllegalArgumentException()
                    .isThrownBy(builder::build)
                    .withMessageContaining("unknown module")
                    .withMessageContaining("shop-domain");
        }

        @Test
        @DisplayName("a blank module name is rejected")
        void blankModuleNameIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ModuleDescriptor("  ", ModuleRole.DOMAIN, Optional.empty()))
                    .withMessageContaining("name must not be blank");
        }

        @Test
        @DisplayName("the descriptor factory leaves the base package unknown")
        void descriptorFactoryLeavesBasePackageUnknown() {
            ModuleDescriptor descriptor = ModuleDescriptor.of("shop-domain", ModuleRole.DOMAIN);

            assertThat(descriptor.basePackage()).isEmpty();
            assertThat(descriptor.role()).isEqualTo(ModuleRole.DOMAIN);
        }
    }
}
