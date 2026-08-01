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

import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ValidationConfigTest {

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("the default gates are permissive: opting in is explicit")
        void defaultGatesArePermissive() {
            ValidationConfig config = ValidationConfig.defaults();

            assertThat(config.failOnUnclassified()).isFalse();
            assertThat(config.minConfidence()).isEqualTo(Confidence.LOW);
            assertThat(config.failOnAmbiguous()).isFalse();
            assertThat(config.allowInferred()).isTrue();
            assertThat(config.findingThresholds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Shape")
    class Shape {

        @Test
        @DisplayName("armed gates carry their thresholds")
        void armedGatesCarryThresholds() {
            ValidationConfig config = ValidationConfig.builder()
                    .failOnUnclassified(true)
                    .minConfidence(Confidence.HIGH)
                    .failOnAmbiguous(true)
                    .allowInferred(false)
                    .findingThresholds(Map.of(IssueCode.of("HG-DDD-012"), Severity.BLOCKER))
                    .build();

            assertThat(config.failOnUnclassified()).isTrue();
            assertThat(config.minConfidence()).isEqualTo(Confidence.HIGH);
            assertThat(config.failOnAmbiguous()).isTrue();
            assertThat(config.allowInferred()).isFalse();
            assertThat(config.findingThresholds()).containsEntry(IssueCode.of("HG-DDD-012"), Severity.BLOCKER);
        }

        @Test
        @DisplayName("finding thresholds iterate in code order")
        void findingThresholdsIterateInCodeOrder() {
            ValidationConfig config = ValidationConfig.builder()
                    .findingThresholds(Map.of(
                            IssueCode.of("HG-GEN-001"), Severity.MINOR,
                            IssueCode.of("HG-DDD-012"), Severity.BLOCKER,
                            IssueCode.of("HG-DDD-001"), Severity.CRITICAL))
                    .build();

            assertThat(config.findingThresholds().keySet())
                    .containsExactly(
                            IssueCode.of("HG-DDD-001"), IssueCode.of("HG-DDD-012"), IssueCode.of("HG-GEN-001"));
        }
    }
}
