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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnalysisScopeTest {

    @Nested
    @DisplayName("Shape")
    class Shape {

        @Test
        @DisplayName("the default scope covers everything")
        void defaultScopeCoversEverything() {
            AnalysisScope scope = AnalysisScope.everything();

            assertThat(scope.basePackage()).isEmpty();
            assertThat(scope.includePackages()).isEmpty();
            assertThat(scope.excludePackages()).isEmpty();
        }

        @Test
        @DisplayName("a delimited scope carries its package prefixes")
        void delimitedScopeCarriesPrefixes() {
            AnalysisScope scope =
                    new AnalysisScope(Optional.of("com.shop"), List.of("com.shop.domain"), List.of("com.shop.legacy"));

            assertThat(scope.basePackage()).contains("com.shop");
            assertThat(scope.includePackages()).containsExactly("com.shop.domain");
            assertThat(scope.excludePackages()).containsExactly("com.shop.legacy");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("a glob pattern is rejected: prefixes only")
        void globPatternIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new AnalysisScope(Optional.empty(), List.of(), List.of("*.util.*")))
                    .withMessageContaining("package prefix");
        }

        @Test
        @DisplayName("a blank prefix is rejected")
        void blankPrefixIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new AnalysisScope(Optional.empty(), List.of(" "), List.of()))
                    .withMessageContaining("package prefix");
        }

        @Test
        @DisplayName("a blank base package is rejected")
        void blankBasePackageIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new AnalysisScope(Optional.of(" "), List.of(), List.of()))
                    .withMessageContaining("basePackage");
        }
    }
}
