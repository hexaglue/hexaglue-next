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

import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.AnalysisScope;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Reading contexts at a fixed depth from the left is how a codebase rooted one segment deeper than
 * another ends up with a single context called after its company name. What decides is the
 * distance from the root, never the distance from nothing.
 */
class BoundedContextsTest {

    private static BoundedContexts contexts(AnalysisScope scope, String... qualifiedNames) {
        CodeModel.Builder code = CodeModel.builder();
        for (String name : qualifiedNames) {
            code.addType(TypeNode.builder(TypeId.of(name), TypeNature.CLASS).build());
        }
        CodeModel model = code.build();
        return BoundedContexts.of(Perimeter.of(model, scope), scope);
    }

    private static AnalysisScope rootedAt(String basePackage) {
        return new AnalysisScope(Optional.of(basePackage), List.of(), List.of());
    }

    @Nested
    @DisplayName("with a stated root")
    class StatedRoot {

        @Test
        @DisplayName("reads the segment right after the root, whatever depth the root is at")
        void readsTheSegmentAfterTheRoot() {
            BoundedContexts shallow =
                    contexts(rootedAt("io.shop"), "io.shop.orders.domain.Order", "io.shop.billing.domain.Invoice");
            BoundedContexts deep = contexts(
                    rootedAt("com.acme.erp"),
                    "com.acme.erp.orders.domain.Order",
                    "com.acme.erp.billing.domain.Invoice");

            assertThat(shallow.names()).containsExactly("billing", "orders");
            assertThat(deep.names()).containsExactly("billing", "orders");
        }

        @Test
        @DisplayName("names the context of a type")
        void namesTheContextOfAType() {
            BoundedContexts contexts =
                    contexts(rootedAt("com.acme"), "com.acme.orders.domain.Order", "com.acme.billing.Invoice");

            assertThat(contexts.of(TypeId.of("com.acme.orders.domain.Order"))).contains("orders");
            assertThat(contexts.of(TypeId.of("com.acme.billing.Invoice"))).contains("billing");
        }

        @Test
        @DisplayName("puts a type sitting at the root itself in no context")
        void leavesTheRootItselfOutside() {
            BoundedContexts contexts = contexts(rootedAt("com.acme"), "com.acme.Application", "com.acme.orders.Order");

            assertThat(contexts.of(TypeId.of("com.acme.Application"))).isEmpty();
            assertThat(contexts.names()).containsExactly("orders");
        }

        @Test
        @DisplayName("lists the types of a context")
        void listsTheTypesOfAContext() {
            BoundedContexts contexts = contexts(
                    rootedAt("com.acme"),
                    "com.acme.orders.domain.Order",
                    "com.acme.orders.api.OrderController",
                    "com.acme.billing.Invoice");

            assertThat(contexts.typesOf("orders"))
                    .containsExactly(
                            TypeId.of("com.acme.orders.api.OrderController"),
                            TypeId.of("com.acme.orders.domain.Order"));
        }
    }

    @Nested
    @DisplayName("without a stated root")
    class InferredRoot {

        @Test
        @DisplayName("takes the longest package prefix every type shares as the root")
        void infersTheRoot() {
            BoundedContexts contexts = contexts(
                    AnalysisScope.everything(),
                    "com.acme.erp.orders.domain.Order",
                    "com.acme.erp.billing.domain.Invoice");

            assertThat(contexts.root()).isEqualTo("com.acme.erp");
            assertThat(contexts.names()).containsExactly("billing", "orders");
        }

        @Test
        @DisplayName("reads a codebase whose packages share nothing as several contexts")
        void readsUnrelatedPackages() {
            BoundedContexts contexts =
                    contexts(AnalysisScope.everything(), "orders.domain.Order", "billing.domain.Invoice");

            assertThat(contexts.root()).isEmpty();
            assertThat(contexts.names()).containsExactly("billing", "orders");
        }

        @Test
        @DisplayName("reads a codebase in a single package as no context at all")
        void readsASinglePackage() {
            BoundedContexts contexts = contexts(AnalysisScope.everything(), "com.acme.Order", "com.acme.Invoice");

            assertThat(contexts.root()).isEqualTo("com.acme");
            assertThat(contexts.names()).isEmpty();
        }
    }
}
