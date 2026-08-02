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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Confidence;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HexaGlueConfigTest {

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("the default configuration analyzes everything, gates nothing, generates at HIGH")
        void defaultConfigurationIsDocumentedPosture() {
            HexaGlueConfig config = HexaGlueConfig.defaults();

            assertThat(config.analysis().basePackage()).isEmpty();
            assertThat(config.classification().explicit()).isEmpty();
            assertThat(config.validation().failOnUnclassified()).isFalse();
            assertThat(config.generation().minConfidence()).isEqualTo(Confidence.HIGH);
        }
    }

    @Nested
    @DisplayName("Shape")
    class Shape {

        @Test
        @DisplayName("a custom configuration composes the four blocks")
        void customConfigurationComposesBlocks() {
            AnalysisScope scope = new AnalysisScope(Optional.of("com.shop"), List.of(), List.of());
            ClassificationConfig classification =
                    new ClassificationConfig(Map.of(TypeId.of("com.shop.Order"), ArchKind.AGGREGATE_ROOT));
            ValidationConfig validation =
                    ValidationConfig.builder().failOnUnclassified(true).build();
            GenerationConfig generation = new GenerationConfig(Confidence.EXPLICIT);

            HexaGlueConfig config = new HexaGlueConfig(scope, classification, validation, generation);

            assertThat(config.analysis().basePackage()).contains("com.shop");
            assertThat(config.classification().declaredKind(TypeId.of("com.shop.Order")))
                    .contains(ArchKind.AGGREGATE_ROOT);
            assertThat(config.validation().failOnUnclassified()).isTrue();
            assertThat(config.generation().minConfidence()).isEqualTo(Confidence.EXPLICIT);
        }

        @Test
        @DisplayName("the generation default threshold is HIGH")
        void generationDefaultThresholdIsHigh() {
            assertThat(GenerationConfig.defaults().minConfidence()).isEqualTo(Confidence.HIGH);
        }
    }
}
