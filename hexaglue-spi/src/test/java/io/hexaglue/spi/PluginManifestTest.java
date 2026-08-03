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

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PluginManifestTest {

    @Test
    @DisplayName("declares an identifier, its dependencies and the options it answers to")
    void declaresWhatTheExecutorNeeds() {
        PluginManifest manifest = new PluginManifest(
                "io.hexaglue.plugin.living-doc", List.of("io.hexaglue.plugin.audit"), Set.of("outputDirectory"));

        assertThat(manifest.id()).isEqualTo("io.hexaglue.plugin.living-doc");
        assertThat(manifest.dependsOn()).containsExactly("io.hexaglue.plugin.audit");
        assertThat(manifest.options()).containsExactly("outputDirectory");
    }

    @Test
    @DisplayName("iterates its options in a stated order, whatever the caller passed")
    void iteratesOptionsInStatedOrder() {
        PluginManifest manifest = new PluginManifest("doc", List.of(), Set.of("zeta", "alpha", "mu"));

        assertThat(manifest.options()).containsExactly("alpha", "mu", "zeta");
    }

    @Test
    @DisplayName("refuses a blank identifier")
    void refusesABlankIdentifier() {
        assertThatThrownBy(() -> new PluginManifest("  ", List.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuses to declare the same dependency twice")
    void refusesADuplicateDependency() {
        assertThatThrownBy(() -> new PluginManifest("doc", List.of("a", "a"), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("a");
    }
}
