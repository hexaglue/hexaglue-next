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

package io.hexaglue.frontend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.hexaglue.model.code.CodeModelCapability;
import io.hexaglue.model.config.AnalysisScope;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FrontendRequestTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("reads one source root over the whole perimeter, with no optional extraction")
        void readsOneSourceRootOverTheWholePerimeter() {
            FrontendRequest request = FrontendRequest.of(SOURCE_ROOT);

            assertThat(request.sourceRoots()).containsExactly(SOURCE_ROOT);
            assertThat(request.classpath()).isEmpty();
            assertThat(request.javaVersion()).isEqualTo(FrontendRequest.DEFAULT_JAVA_VERSION);
            assertThat(request.scope()).isEqualTo(AnalysisScope.everything());
            assertThat(request.capabilities()).isEmpty();
            assertThat(request.has(CodeModelCapability.METHOD_BODIES)).isFalse();
        }

        @Test
        @DisplayName("carries every configured input")
        void carriesEveryConfiguredInput() {
            Path jar = Path.of("lib", "spring-data.jar");
            AnalysisScope scope = new AnalysisScope(Optional.of("com.acme"), List.of("com.acme"), List.of());

            FrontendRequest request = FrontendRequest.builder()
                    .sourceRoot(SOURCE_ROOT)
                    .classpathEntry(jar)
                    .javaVersion(21)
                    .scope(scope)
                    .capability(CodeModelCapability.METHOD_BODIES)
                    .build();

            assertThat(request.classpath()).containsExactly(jar);
            assertThat(request.javaVersion()).isEqualTo(21);
            assertThat(request.scope()).isEqualTo(scope);
            assertThat(request.has(CodeModelCapability.METHOD_BODIES)).isTrue();
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects a request without a source root")
        void rejectsRequestWithoutSourceRoot() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> FrontendRequest.builder().build())
                    .withMessageContaining("source root");
        }

        @Test
        @DisplayName("rejects a Java level the parser cannot handle")
        void rejectsUnsupportedJavaVersion() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> FrontendRequest.builder()
                            .sourceRoot(SOURCE_ROOT)
                            .javaVersion(7)
                            .build())
                    .withMessageContaining("javaVersion");
        }

        @Test
        @DisplayName("copies its collections defensively")
        void copiesItsCollectionsDefensively() {
            FrontendRequest request = new FrontendRequest(
                    List.of(SOURCE_ROOT),
                    List.of(),
                    FrontendRequest.DEFAULT_JAVA_VERSION,
                    AnalysisScope.everything(),
                    Set.of(CodeModelCapability.METHOD_BODIES),
                    Optional.empty());

            assertThat(request.capabilities()).containsExactly(CodeModelCapability.METHOD_BODIES);
            assertThat(request.sourceRoots()).isUnmodifiable();
            assertThat(request.classpath()).isUnmodifiable();
        }
    }
}
