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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("held to a golden that must already be there")
    class HeldToAnExistingGolden {

        private String recording;

        /** The switch is a property of the whole run, so each case states which run it is. */
        @BeforeEach
        void rememberTheRun() {
            recording = System.getProperty(GoldenFiles.REGENERATE_PROPERTY);
            System.clearProperty(GoldenFiles.REGENERATE_PROPERTY);
        }

        @AfterEach
        void restoreTheRun() {
            if (recording == null) {
                System.clearProperty(GoldenFiles.REGENERATE_PROPERTY);
            } else {
                System.setProperty(GoldenFiles.REGENERATE_PROPERTY, recording);
            }
        }

        @Test
        @DisplayName("passes when the snapshot is what the golden file records")
        void passesOnIdenticalContent() throws Exception {
            Files.writeString(tempDir.resolve("snapshot.txt"), "content-v1");

            GoldenFiles.assertMatchesExisting(tempDir, "snapshot.txt", "content-v1");
        }

        @Test
        @DisplayName("fails when the snapshot has moved away from it")
        void failsOnDifferentContent() throws Exception {
            Files.writeString(tempDir.resolve("snapshot.txt"), "content-v1");

            assertThatThrownBy(() -> GoldenFiles.assertMatchesExisting(tempDir, "snapshot.txt", "content-v2"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("snapshot.txt");
        }

        @Test
        @DisplayName("fails when there is no golden yet, rather than passing on one it just wrote")
        void failsWhenTheGoldenIsMissing() {
            assertThatThrownBy(() -> GoldenFiles.assertMatchesExisting(tempDir, "snapshot.txt", "content-v1"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(GoldenFiles.REGENERATE_PROPERTY);
        }

        @Test
        @DisplayName("and records it when the run says outright that is what it is for")
        void recordsItOnADeliberateRegeneration() throws Exception {
            System.setProperty(GoldenFiles.REGENERATE_PROPERTY, "true");

            GoldenFiles.assertMatchesExisting(tempDir, "snapshot.txt", "content-v1");

            assertThat(Files.readString(tempDir.resolve("snapshot.txt"))).isEqualTo("content-v1");
        }
    }
}
