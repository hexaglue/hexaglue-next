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

package io.hexaglue.plugin.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.hexaglue.spi.PluginConfig;
import io.hexaglue.spi.PluginConfigException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * An option read as a bare word is an option that fails somewhere else, later, in a message that
 * names neither the option nor what would have been right.
 */
class JpaOptionsTest {

    private static JpaOptions read(Map<String, String> stated) {
        return JpaOptions.from(PluginConfig.of("io.hexaglue.jpa", stated));
    }

    @Nested
    @DisplayName("works from what the build states")
    class WorksFromWhatTheBuildStates {

        @Test
        @DisplayName("and falls back on its own defaults for the rest")
        void fallsBackOnItsOwnDefaults() {
            JpaOptions options = read(Map.of("entitySuffix", "Row"));

            assertThat(options.entitySuffix()).isEqualTo("Row");
            assertThat(options.embeddableSuffix()).isEqualTo("Embeddable");
            assertThat(options.tablePrefix()).isEmpty();
            assertThat(options.embeddables()).isTrue();
            assertThat(options.targetModule()).isEmpty();
        }

        @Test
        @DisplayName("naming what it generates after the type it stores")
        void namingWhatItGenerates() {
            JpaOptions options = read(Map.of("entitySuffix", "Row", "embeddableSuffix", "Value"));

            assertThat(options.entityFor("Order")).isEqualTo("OrderRow");
            assertThat(options.embeddableFor("Money")).isEqualTo("MoneyValue");
        }

        @Test
        @DisplayName("and answering to every key it declares, and to no other")
        void answeringToEveryKeyItDeclares() {
            JpaOptions options = read(Map.of(
                    "entitySuffix", "Row",
                    "embeddableSuffix", "Value",
                    "tablePrefix", "shop_",
                    "generateEmbeddables", "false",
                    "generateRepositories", "false",
                    "repositorySuffix", "Store",
                    "idStrategy", "SEQUENCE",
                    "targetModule", "shop-persistence"));

            assertThat(options.tablePrefix()).isEqualTo("shop_");
            assertThat(options.embeddables()).isFalse();
            assertThat(options.repositories()).isFalse();
            assertThat(options.repositoryFor("Order")).isEqualTo("OrderStore");
            assertThat(options.identity()).isEqualTo(IdentityStrategy.SEQUENCE);
            assertThat(options.targetModule()).contains("shop-persistence");
            assertThat(JpaOptions.KEYS)
                    .containsExactlyInAnyOrder(
                            "entitySuffix",
                            "embeddableSuffix",
                            "repositorySuffix",
                            "tablePrefix",
                            "generateEmbeddables",
                            "generateRepositories",
                            "idStrategy",
                            "targetModule");
        }
    }

    @Nested
    @DisplayName("the strategy an identity falls back on")
    class TheIdentityStrategy {

        /**
         * A domain that owns its identities builds them itself, so the store records what it is
         * handed. The carrière read the same default and documented the opposite; the code was
         * right and its documentation was not.
         */
        @Test
        @DisplayName("is assigned by the domain unless the build says otherwise")
        void isAssignedByTheDomainByDefault() {
            assertThat(JpaOptions.defaults().identity()).isEqualTo(IdentityStrategy.ASSIGNED);
            assertThat(read(Map.of()).identity()).isEqualTo(IdentityStrategy.ASSIGNED);
        }

        @Test
        @DisplayName("is read whatever case the build wrote it in")
        void isReadWhateverCase() {
            assertThat(read(Map.of("idStrategy", "identity")).identity()).isEqualTo(IdentityStrategy.IDENTITY);
            assertThat(read(Map.of("idStrategy", " AUTO ")).identity()).isEqualTo(IdentityStrategy.AUTO);
        }

        @Test
        @DisplayName("and a word that is not one of them is refused, with the ones that are named")
        void andAWordThatIsNotOneIsRefused() {
            assertThatThrownBy(() -> read(Map.of("idStrategy", "SERIAL")))
                    .isInstanceOf(PluginConfigException.class)
                    .satisfies(refused -> {
                        String message =
                                ((PluginConfigException) refused).diagnostic().message();
                        assertThat(message)
                                .contains("idStrategy")
                                .contains("SERIAL")
                                .contains("ASSIGNED", "SEQUENCE");
                    });
        }
    }

    @Nested
    @DisplayName("refuses what would not survive being made into a name")
    class RefusesWhatWouldNotSurvive {

        @Test
        @DisplayName("a suffix that is not usable in a type name")
        void aSuffixThatIsNotUsable() {
            assertThatThrownBy(() -> read(Map.of("entitySuffix", "-Row"))).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> read(Map.of("embeddableSuffix", "Va lue")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("but allows none at all, which is a project storing types under their own name")
        void butAllowsNoneAtAll() {
            assertThat(read(Map.of("entitySuffix", "")).entityFor("Order")).isEqualTo("Order");
        }

        @Test
        @DisplayName("and a module stated as nothing at all")
        void andAModuleStatedAsNothing() {
            assertThatThrownBy(() -> read(Map.of("targetModule", " "))).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
