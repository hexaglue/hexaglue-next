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

package io.hexaglue.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a project asks of a backend is read but not judged here. Only the backend knows which
 * options it answers to, and it is the one that can name the alternatives when a key is misspelt —
 * so the strictness lives there, one stage later and far more useful.
 */
class PluginOptionsTest {

    @Test
    @DisplayName("reads what the document asks of each backend, keyed by plugin")
    void readsEachBackend() {
        Map<String, Map<String, String>> options = ConfigLoader.pluginOptions("hexaglue.yaml", """
                plugins:
                  io.hexaglue.audit:
                    outputDirectory: report
                  io.hexaglue.living-doc:
                    generateDiagrams: false
                """);

        assertThat(options).containsOnlyKeys("io.hexaglue.audit", "io.hexaglue.living-doc");
        assertThat(options.get("io.hexaglue.audit")).containsEntry("outputDirectory", "report");
    }

    @Test
    @DisplayName("reads a flag or a count as the text a plugin parses, because that is what an author writes")
    void readsScalarsAsText() {
        Map<String, Map<String, String>> options = ConfigLoader.pluginOptions("hexaglue.yaml", """
                plugins:
                  io.hexaglue.living-doc:
                    generateDiagrams: false
                    propertiesPerDiagram: 3
                """);

        assertThat(options.get("io.hexaglue.living-doc"))
                .containsEntry("generateDiagrams", "false")
                .containsEntry("propertiesPerDiagram", "3");
    }

    @Test
    @DisplayName("leaves an option nobody here knows alone, for the plugin to refuse")
    void leavesUnknownOptionsAlone() {
        Map<String, Map<String, String>> options = ConfigLoader.pluginOptions("hexaglue.yaml", """
                plugins:
                  io.hexaglue.audit:
                    somethingNobodyDeclared: true
                """);

        assertThat(options.get("io.hexaglue.audit")).containsEntry("somethingNobodyDeclared", "true");
    }

    @Test
    @DisplayName("says nothing when the document asks nothing of any backend")
    void readsNothingWhenUnstated() {
        assertThat(ConfigLoader.pluginOptions("hexaglue.yaml", "analysis:\n  basePackage: com.acme\n"))
                .isEmpty();
    }

    @Test
    @DisplayName("refuses a backend whose options are not a mapping")
    void refusesAMisshapenBackend() {
        assertThatThrownBy(() -> ConfigLoader.pluginOptions("hexaglue.yaml", "plugins:\n  io.hexaglue.audit: report\n"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("io.hexaglue.audit");
    }

    @Test
    @DisplayName("refuses a root key nobody reads, plugins section or not")
    void refusesAnUnknownRootKey() {
        assertThatThrownBy(() -> ConfigLoader.pluginOptions("hexaglue.yaml", "plugin:\n  io.hexaglue.audit: {}\n"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("plugin");
    }
}
