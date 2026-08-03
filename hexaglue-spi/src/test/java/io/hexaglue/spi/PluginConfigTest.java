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

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Plugin options are opaque to the model — only the plugin knows its own vocabulary. What the
 * shared contract owes is the same strictness the configuration loader applies one stage earlier:
 * a value the plugin cannot read is an error that names the key and the value, never a default
 * applied in silence.
 */
class PluginConfigTest {

    private static PluginConfig config(Map<String, String> options) {
        return PluginConfig.of("doc", options);
    }

    @Nested
    @DisplayName("reading a value")
    class Reading {

        @Test
        @DisplayName("returns the text of a stated option")
        void returnsStatedText() {
            assertThat(config(Map.of("title", "Architecture")).text("title")).contains("Architecture");
        }

        @Test
        @DisplayName("returns nothing for an option the document never stated")
        void returnsNothingForAnUnstatedOption() {
            assertThat(config(Map.of()).text("title")).isEmpty();
        }

        @Test
        @DisplayName("reads a flag both ways, and falls back only when unstated")
        void readsAFlag() {
            assertThat(config(Map.of("diagrams", "true")).flag("diagrams", false))
                    .isTrue();
            assertThat(config(Map.of("diagrams", "false")).flag("diagrams", true))
                    .isFalse();
            assertThat(config(Map.of()).flag("diagrams", true)).isTrue();
        }

        @Test
        @DisplayName("reads a number, and falls back only when unstated")
        void readsANumber() {
            assertThat(config(Map.of("depth", "12")).number("depth", 3)).isEqualTo(12);
            assertThat(config(Map.of()).number("depth", 3)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("a value the plugin cannot read")
    class Malformed {

        @Test
        @DisplayName("refuses a flag that is neither true nor false")
        void refusesAnAmbiguousFlag() {
            assertThatThrownBy(() -> config(Map.of("diagrams", "yes")).flag("diagrams", false))
                    .isInstanceOf(PluginConfigException.class)
                    .hasMessageContaining("diagrams")
                    .hasMessageContaining("yes");
        }

        @Test
        @DisplayName("refuses a number that is not one")
        void refusesANonNumber() {
            assertThatThrownBy(() -> config(Map.of("depth", "deep")).number("depth", 3))
                    .isInstanceOf(PluginConfigException.class)
                    .hasMessageContaining("depth")
                    .hasMessageContaining("deep");
        }

        @Test
        @DisplayName("names the plugin whose option it is")
        void namesThePlugin() {
            assertThatThrownBy(() -> config(Map.of("depth", "deep")).number("depth", 3))
                    .hasMessageContaining("doc");
        }
    }
}
