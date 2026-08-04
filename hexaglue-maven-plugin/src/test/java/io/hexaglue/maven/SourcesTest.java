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

import io.hexaglue.spi.SourceFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Where a generated type lands, and which run is the one to place it.
 */
class SourcesTest {

    @TempDir
    Path root;

    private static final SourceFile HERE = SourceFile.of("com.acme.shop", "OrderEntity", "package com.acme.shop;");
    private static final SourceFile ROUTED = SourceFile.of("com.acme.shop", "OrderMapper", "package com.acme.shop;")
            .in("shop-persistence");

    @Test
    @DisplayName("a type stating no module belongs to whoever is building")
    void aTypeStatingNoModuleBelongsHere() {
        assertThat(Sources.addressedTo(List.of(HERE), "shop-core")).containsExactly(HERE);
        assertThat(Sources.addressedElsewhere(List.of(HERE), "shop-core")).isEmpty();
    }

    @Test
    @DisplayName("and one naming this very module belongs here too")
    void oneNamingThisModuleBelongsHere() {
        assertThat(Sources.addressedTo(List.of(ROUTED), "shop-persistence")).containsExactly(ROUTED);
        assertThat(Sources.addressedElsewhere(List.of(ROUTED), "shop-persistence"))
                .isEmpty();
    }

    /**
     * A goal runs inside one module and writes into that module's build directory. What a backend
     * addressed elsewhere is left to the run of the module it names — said, never dropped.
     */
    @Test
    @DisplayName("but one naming another module is left to that module's own run")
    void oneNamingAnotherModuleIsLeftToIt() {
        assertThat(Sources.addressedTo(List.of(HERE, ROUTED), "shop-core")).containsExactly(HERE);
        assertThat(Sources.addressedElsewhere(List.of(HERE, ROUTED), "shop-core"))
                .containsExactly(ROUTED);
    }

    @Test
    @DisplayName("writes a type where its package says, under the one root")
    void writesATypeWhereItsPackageSays() throws Exception {
        Sources.write(List.of(HERE), root);

        Path written = root.resolve("com/acme/shop/OrderEntity.java");
        assertThat(written).exists();
        assertThat(Files.readString(written)).isEqualTo("package com.acme.shop;");
    }

    @Test
    @DisplayName("and writing nothing leaves the root alone")
    void writingNothingLeavesTheRootAlone() throws Exception {
        Sources.write(List.of(), root);

        assertThat(Files.list(root)).isEmpty();
    }
}
