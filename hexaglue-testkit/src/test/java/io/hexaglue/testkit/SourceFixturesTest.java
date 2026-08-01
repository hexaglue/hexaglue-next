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

package io.hexaglue.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceFixturesTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("writes a fixture under nested directories")
    void writesNestedFixture() throws Exception {
        Path written = SourceFixtures.write(tempDir, "com/acme/Order.java", "package com.acme;\nclass Order {}\n");

        assertThat(written).isEqualTo(tempDir.resolve("com/acme/Order.java"));
        assertThat(Files.readString(written)).contains("class Order {}");
    }

    @Test
    @DisplayName("rejects a blank relative path")
    void rejectsBlankPath() {
        assertThatThrownBy(() -> SourceFixtures.write(tempDir, "  ", "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("rejects an absolute path")
    void rejectsAbsolutePath() {
        assertThatThrownBy(() -> SourceFixtures.write(tempDir, "/etc/Order.java", "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be relative");
    }
}
