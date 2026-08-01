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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Golden-file assertion: compares an actual snapshot against a versioned reference file.
 *
 * <p>If the golden file does not exist yet, it is created from the actual snapshot and the
 * assertion passes. Regeneration therefore follows the delete-and-rerun idiom: remove the golden
 * file, rerun the test, review the recreated file in the diff.
 *
 * @since 7.0.0
 */
public final class GoldenFiles {

    private GoldenFiles() {}

    /**
     * Asserts that {@code actual} is byte-identical to the golden file {@code fileName} under
     * {@code goldenDir}, creating the golden file from {@code actual} if it does not exist.
     *
     * @param goldenDir the directory holding golden files, usually under {@code src/test/resources}
     * @param fileName the golden file name, e.g. {@code coffeeshop-arch-model.json}
     * @param actual the snapshot produced by the code under test
     * @throws UncheckedIOException if the golden file cannot be read or written
     */
    public static void assertMatches(Path goldenDir, String fileName, String actual) {
        Objects.requireNonNull(goldenDir, "goldenDir");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(actual, "actual");
        Path goldenPath = goldenDir.resolve(fileName);
        if (Files.exists(goldenPath)) {
            String expected = read(goldenPath);
            assertThat(actual)
                    .as("Golden file mismatch: %s (delete the file and rerun to regenerate)", goldenPath)
                    .isEqualTo(expected);
        } else {
            create(goldenPath, actual);
        }
    }

    private static String read(Path goldenPath) {
        try {
            return Files.readString(goldenPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read golden file " + goldenPath, e);
        }
    }

    private static void create(Path goldenPath, String content) {
        try {
            Files.createDirectories(Objects.requireNonNull(goldenPath.getParent(), "golden parent directory"));
            Files.writeString(goldenPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create golden file " + goldenPath, e);
        }
    }
}
