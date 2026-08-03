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

import io.hexaglue.model.TypeId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.ModuleNode;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.testkit.SourceFixtures;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Which module a type came from is something only the host knows: the same package can be spread
 * over several modules, and nothing inside a type says which one compiled it. So the reading is
 * told, and it records it — on what it read, never on a type the classpath merely supplied.
 */
class ModuleReadingTest {

    @TempDir
    Path sources;

    private CodeModel read(String moduleName) {
        FrontendRequest.Builder request = FrontendRequest.builder();
        if (moduleName == null) {
            request.sourceRoot(sources);
        } else {
            request.sourceRoot(sources, moduleName);
        }
        return SpoonFrontend.analyze(request.build()).code();
    }

    @Test
    @DisplayName("records the module on every type it read")
    void recordsTheModuleOnWhatItRead() {
        SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");

        CodeModel code = read("shop-domain");

        assertThat(code.type(TypeId.of("com.acme.Order")).map(TypeNode::moduleName))
                .contains(Optional.of("shop-domain"));
        assertThat(code.modules()).containsExactly(new ModuleNode("shop-domain", Optional.empty()));
    }

    @Test
    @DisplayName("records nothing when the host says nothing, which is a single-module project")
    void recordsNothingOnASingleModuleProject() {
        SourceFixtures.write(sources, "com/acme/Order.java", "package com.acme; public class Order {}");

        CodeModel code = read(null);

        assertThat(code.type(TypeId.of("com.acme.Order")).map(TypeNode::moduleName))
                .contains(Optional.empty());
        assertThat(code.modules()).isEmpty();
    }

    @Test
    @DisplayName("leaves a type the classpath supplied outside the module's boundary")
    void leavesStubsOutsideTheModule() {
        SourceFixtures.write(
                sources, "com/acme/Order.java", "package com.acme; public class Order extends java.util.ArrayList {}");

        CodeModel code = read("shop-domain");

        assertThat(code.type(TypeId.of("java.util.ArrayList")).map(TypeNode::moduleName))
                .contains(Optional.empty());
    }

    @Test
    @DisplayName("reads a whole reactor in one pass, each type stamped with the root it came from")
    void readsAWholeReactorInOnePass() {
        Path domain = sources.resolve("shop-domain");
        Path infra = sources.resolve("shop-infra");
        SourceFixtures.write(domain, "com/acme/Order.java", "package com.acme; public class Order {}");
        SourceFixtures.write(
                infra,
                "com/acme/jpa/OrderRecord.java",
                "package com.acme.jpa; public class OrderRecord { com.acme.Order order; }");

        CodeModel code = SpoonFrontend.analyze(FrontendRequest.builder()
                        .sourceRoot(domain, "shop-domain")
                        .sourceRoot(infra, "shop-infra")
                        .build())
                .code();

        assertThat(code.type(TypeId.of("com.acme.Order")).map(TypeNode::moduleName))
                .contains(Optional.of("shop-domain"));
        assertThat(code.type(TypeId.of("com.acme.jpa.OrderRecord")).map(TypeNode::moduleName))
                .contains(Optional.of("shop-infra"));
        assertThat(code.modules())
                .containsExactly(
                        new ModuleNode("shop-domain", Optional.empty()),
                        new ModuleNode("shop-infra", Optional.empty()));
    }

    @Test
    @DisplayName("a reference across two modules resolves to the type, not to a stub of it")
    void resolvesReferencesAcrossModules() {
        Path domain = sources.resolve("shop-domain");
        Path infra = sources.resolve("shop-infra");
        SourceFixtures.write(domain, "com/acme/Order.java", "package com.acme; public class Order {}");
        SourceFixtures.write(
                infra,
                "com/acme/jpa/OrderRecord.java",
                "package com.acme.jpa; public class OrderRecord { com.acme.Order order; }");

        CodeModel code = SpoonFrontend.analyze(FrontendRequest.builder()
                        .sourceRoot(domain, "shop-domain")
                        .sourceRoot(infra, "shop-infra")
                        .build())
                .code();

        assertThat(code.edges())
                .anySatisfy(edge -> assertThat(edge.source().qualifiedName()).isEqualTo("com.acme.jpa.OrderRecord"));
        assertThat(code.type(TypeId.of("com.acme.Order")).map(TypeNode::moduleName))
                .contains(Optional.of("shop-domain"));
    }

    @Test
    @DisplayName("a root read without a module leaves its types outside any module")
    void aRootWithoutAModuleLeavesItsTypesUnstamped() {
        Path domain = sources.resolve("shop-domain");
        Path scripts = sources.resolve("scripts");
        SourceFixtures.write(domain, "com/acme/Order.java", "package com.acme; public class Order {}");
        SourceFixtures.write(scripts, "com/acme/Tooling.java", "package com.acme; public class Tooling {}");

        CodeModel code = SpoonFrontend.analyze(FrontendRequest.builder()
                        .sourceRoot(domain, "shop-domain")
                        .sourceRoot(scripts)
                        .build())
                .code();

        assertThat(code.type(TypeId.of("com.acme.Tooling")).map(TypeNode::moduleName))
                .contains(Optional.empty());
        assertThat(code.modules()).containsExactly(new ModuleNode("shop-domain", Optional.empty()));
    }
}
