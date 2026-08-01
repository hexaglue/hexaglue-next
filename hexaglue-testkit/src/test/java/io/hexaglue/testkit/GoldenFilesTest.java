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

class GoldenFilesTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("creates the golden file from the actual snapshot when absent")
    void createsGoldenWhenAbsent() throws Exception {
        GoldenFiles.assertMatches(tempDir, "snapshot.txt", "content-v1");

        assertThat(Files.readString(tempDir.resolve("snapshot.txt"))).isEqualTo("content-v1");
    }

    @Test
    @DisplayName("passes when the actual snapshot is identical to the golden file")
    void passesOnIdenticalContent() throws Exception {
        Files.writeString(tempDir.resolve("snapshot.txt"), "content-v1");

        GoldenFiles.assertMatches(tempDir, "snapshot.txt", "content-v1");
    }

    @Test
    @DisplayName("fails when the actual snapshot differs from the golden file")
    void failsOnDifferentContent() throws Exception {
        Files.writeString(tempDir.resolve("snapshot.txt"), "content-v1");

        assertThatThrownBy(() -> GoldenFiles.assertMatches(tempDir, "snapshot.txt", "content-v2"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("snapshot.txt");
    }

    @Test
    @DisplayName("creates parent directories of the golden file when needed")
    void createsParentDirectories() throws Exception {
        GoldenFiles.assertMatches(tempDir.resolve("nested/dir"), "snapshot.txt", "content-v1");

        assertThat(Files.readString(tempDir.resolve("nested/dir/snapshot.txt"))).isEqualTo("content-v1");
    }
}
