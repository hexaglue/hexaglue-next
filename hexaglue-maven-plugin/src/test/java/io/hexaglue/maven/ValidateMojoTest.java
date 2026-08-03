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

import io.hexaglue.model.config.HexaGlueConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Two ways to say the same thing meet here: a document beside the POM, and the parameters of a
 * build. What matters is that saying nothing is not saying no — a parameter left alone must not
 * undo a gate the document armed.
 */
class ValidateMojoTest {

    private static HexaGlueConfig documentArming() {
        return ConfigLoader.load("hexaglue.yaml", """
                analysis:
                  basePackage: com.acme
                validation:
                  failOnUnclassified: true
                  minConfidence: HIGH
                """);
    }

    @Test
    @DisplayName("leaves the document alone when the build states nothing")
    void leavesTheDocumentAloneWhenTheBuildStatesNothing() {
        HexaGlueConfig config = ValidateMojo.configuration(documentArming(), null, null);

        assertThat(config.analysis().basePackage()).contains("com.acme");
        assertThat(config.validation().failOnUnclassified()).isTrue();
    }

    @Test
    @DisplayName("takes the package the build names over the one the document states")
    void takesThePackageTheBuildNames() {
        HexaGlueConfig config = ValidateMojo.configuration(documentArming(), "com.other", null);

        assertThat(config.analysis().basePackage()).contains("com.other");
    }

    @Test
    @DisplayName("disarms a gate when the build says so outright, and only then")
    void disarmsAGateWhenTheBuildSaysSoOutright() {
        HexaGlueConfig disarmed = ValidateMojo.configuration(documentArming(), null, false);

        assertThat(disarmed.validation().failOnUnclassified()).isFalse();
        // The other gates the document armed are untouched: a parameter answers its own question.
        assertThat(disarmed.validation().minConfidence())
                .isEqualTo(documentArming().validation().minConfidence());
    }

    @Test
    @DisplayName("keeps a blank package out of the perimeter, since blank states nothing")
    void keepsABlankPackageOut() {
        HexaGlueConfig config = ValidateMojo.configuration(HexaGlueConfig.defaults(), "  ", null);

        assertThat(config.analysis().basePackage()).isEmpty();
    }
}
