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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClassificationConfigTest {

    private static final TypeId ORDER = TypeId.of("com.shop.Order");
    private static final TypeId ORDER_REPOSITORY = TypeId.of("com.shop.OrderRepository");

    @Nested
    @DisplayName("Declared intent")
    class DeclaredIntent {

        @Test
        @DisplayName("a declared type answers its kind")
        void declaredTypeAnswersItsKind() {
            ClassificationConfig config = new ClassificationConfig(Map.of(ORDER, ArchKind.AGGREGATE_ROOT));

            assertThat(config.declaredKind(ORDER)).contains(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("an undeclared type answers empty")
        void undeclaredTypeAnswersEmpty() {
            ClassificationConfig config = new ClassificationConfig(Map.of(ORDER, ArchKind.AGGREGATE_ROOT));

            assertThat(config.declaredKind(TypeId.of("com.shop.Unknown"))).isEmpty();
        }

        @Test
        @DisplayName("the empty configuration declares nothing")
        void emptyConfigurationDeclaresNothing() {
            ClassificationConfig config = ClassificationConfig.empty();

            assertThat(config.explicit()).isEmpty();
            assertThat(config.declaredKind(ORDER)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Shape")
    class Shape {

        @Test
        @DisplayName("declarations iterate in type identity order whatever the input order")
        void declarationsIterateInIdentityOrder() {
            Map<TypeId, ArchKind> unordered = new LinkedHashMap<>();
            unordered.put(ORDER_REPOSITORY, ArchKind.DRIVEN_PORT);
            unordered.put(ORDER, ArchKind.AGGREGATE_ROOT);

            ClassificationConfig config = new ClassificationConfig(unordered);

            assertThat(config.explicit().keySet())
                    .extracting(TypeId::qualifiedName)
                    .containsExactly("com.shop.Order", "com.shop.OrderRepository");
        }

        @Test
        @DisplayName("the declarations are copied away from the caller")
        void declarationsAreCopiedFromTheCaller() {
            Map<TypeId, ArchKind> source = new HashMap<>();
            source.put(ORDER, ArchKind.AGGREGATE_ROOT);
            ClassificationConfig config = new ClassificationConfig(source);

            source.put(ORDER_REPOSITORY, ArchKind.DRIVEN_PORT);

            assertThat(config.explicit()).hasSize(1);
        }

        @Test
        @DisplayName("declaring the fallback kind is rejected")
        void declaringTheFallbackKindIsRejected() {
            Map<TypeId, ArchKind> declarations = Map.of(ORDER, ArchKind.UNCLASSIFIED);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ClassificationConfig(declarations))
                    .withMessageContaining("com.shop.Order")
                    .withMessageContaining("UNCLASSIFIED");
        }
    }
}
