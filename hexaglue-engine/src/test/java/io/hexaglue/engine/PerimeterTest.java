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

package io.hexaglue.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import io.hexaglue.model.config.HexaGlueConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PerimeterTest {

    private static final CodeModel MODEL = CodeModel.builder()
            .addType(source("com.acme.Order"))
            .addType(source("com.acme.internal.OrderLine"))
            .addType(source("com.acmetools.Reporting"))
            .addType(source("com.other.Invoice"))
            .addType(TypeNode.externalStub(
                    TypeId.of("org.springframework.data.repository.Repository"), TypeNature.INTERFACE))
            .build();

    private static TypeNode source(String qualifiedName) {
        return TypeNode.builder(TypeId.of(qualifiedName), TypeNature.CLASS).build();
    }

    private static List<String> covered(AnalysisScope scope) {
        return Perimeter.of(MODEL, scope).types().stream()
                .map(type -> type.id().qualifiedName())
                .toList();
    }

    @Nested
    @DisplayName("owes a verdict")
    class OwesAVerdict {

        @Test
        @DisplayName("on every analyzed type when nothing is scoped")
        void onEveryAnalyzedType() {
            assertThat(covered(AnalysisScope.everything()))
                    .containsExactly(
                            "com.acme.Order",
                            "com.acme.internal.OrderLine",
                            "com.acmetools.Reporting",
                            "com.other.Invoice");
        }

        @Test
        @DisplayName("on the base package and what it contains, on segment boundaries only")
        void onTheBasePackageAndWhatItContains() {
            AnalysisScope scope = new AnalysisScope(Optional.of("com.acme"), List.of(), List.of());

            assertThat(covered(scope)).containsExactly("com.acme.Order", "com.acme.internal.OrderLine");
        }

        @Test
        @DisplayName("on an included package only, when inclusions are named")
        void onAnIncludedPackageOnly() {
            AnalysisScope scope = new AnalysisScope(Optional.empty(), List.of("com.other"), List.of());

            assertThat(covered(scope)).containsExactly("com.other.Invoice");
        }

        @Test
        @DisplayName("on nothing an exclusion covers")
        void onNothingAnExclusionCovers() {
            AnalysisScope scope = new AnalysisScope(Optional.of("com.acme"), List.of(), List.of("com.acme.internal"));

            assertThat(covered(scope)).containsExactly("com.acme.Order");
        }
    }

    @Nested
    @DisplayName("owes nothing")
    class OwesNothing {

        @Test
        @DisplayName("on a classpath stub, which is not the user's code")
        void onAClasspathStub() {
            assertThat(Perimeter.of(MODEL, AnalysisScope.everything())
                            .contains(TypeId.of("org.springframework.data.repository.Repository")))
                    .isFalse();
        }

        @Test
        @DisplayName("on a type of no analyzed module")
        void onATypeOfNoAnalyzedModule() {
            assertThat(Perimeter.of(MODEL, AnalysisScope.everything()).contains(TypeId.of("com.acme.Unknown")))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("says what it left out")
    class SaysWhatItLeftOut {

        private static List<String> excluded(AnalysisScope scope) {
            return Perimeter.of(MODEL, scope).excluded().stream()
                    .map(exclusion -> exclusion.type().qualifiedName())
                    .toList();
        }

        @Test
        @DisplayName("naming a type the base package does not cover, and why")
        void namingATypeTheBasePackageDoesNotCover() {
            AnalysisScope scope = new AnalysisScope(Optional.of("com.acme"), List.of(), List.of());

            // A type read and then denied a verdict vanishes from the model: nothing downstream
            // can tell it from a type that was never written.
            assertThat(excluded(scope)).containsExactly("com.acmetools.Reporting", "com.other.Invoice");
            assertThat(Perimeter.of(MODEL, scope).excluded().get(0).reason()).contains("com.acme");
        }

        @Test
        @DisplayName("naming a type an exclusion sent out")
        void namingATypeAnExclusionSentOut() {
            AnalysisScope scope = new AnalysisScope(Optional.empty(), List.of(), List.of("com.acme.internal"));

            assertThat(excluded(scope)).containsExactly("com.acme.internal.OrderLine");
        }

        @Test
        @DisplayName("keeping quiet about a classpath stub, which is not the user's code")
        void keepingQuietAboutAClasspathStub() {
            assertThat(excluded(AnalysisScope.everything())).isEmpty();
        }
    }

    @Nested
    @DisplayName("is derived once")
    class IsDerivedOnce {

        @Test
        @DisplayName("when the context is assembled from the configuration")
        void whenTheContextIsAssembled() {
            EngineContext context = EngineContext.of(MODEL, KnowledgePacks.embedded(), HexaGlueConfig.defaults());

            assertThat(context.perimeter().contains(TypeId.of("com.acme.Order")))
                    .isTrue();
            assertThat(context.code()).isSameAs(MODEL);
        }
    }
}
