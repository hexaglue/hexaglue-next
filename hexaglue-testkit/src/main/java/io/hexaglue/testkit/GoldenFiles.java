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
 * <p>Two ways of doing it, and which one fits depends on how the golden files came about. For a
 * handful of fixtures a person wrote and will read in the diff, {@link #assertMatches} creates a
 * missing golden and passes, so regeneration is the delete-and-rerun idiom. For a corpus of
 * hundreds, that is a trap: a scenario nobody reviewed would record whatever the run produced and
 * report success. {@link #assertMatchesExisting} refuses to write one unless the run says outright
 * that recording is what it is for.
 *
 * @since 7.0.0
 */
public final class GoldenFiles {

    /**
     * System property that turns a run into a deliberate recording: with it set, a golden file that
     * {@link #assertMatchesExisting} finds missing is written instead of reported.
     *
     * @since 7.0.0
     */
    public static final String REGENERATE_PROPERTY = "hexaglue.golden.regenerate";

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

    /**
     * Asserts that {@code actual} is byte-identical to a golden file that already exists.
     *
     * <p>A missing golden is a failure, not a fixture to write: the whole point of a corpus of
     * goldens is that every one of them was looked at once. Recording a new one is a run of its
     * own, asked for with {@code -D}{@value #REGENERATE_PROPERTY}{@code =true}, whose output belongs
     * in a diff somebody reads.
     *
     * @param goldenDir the directory holding golden files, usually under {@code src/test/resources}
     * @param fileName the golden file name, e.g. {@code coffeeshop-arch-model.json}
     * @param actual the snapshot produced by the code under test
     * @throws UncheckedIOException if the golden file cannot be read or written
     */
    public static void assertMatchesExisting(Path goldenDir, String fileName, String actual) {
        Objects.requireNonNull(goldenDir, "goldenDir");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(actual, "actual");
        Path goldenPath = goldenDir.resolve(fileName);
        if (Files.exists(goldenPath)) {
            assertThat(actual)
                    .as(
                            "Golden file mismatch: %s (rerun with -D%s=true to record the new snapshot)",
                            goldenPath, REGENERATE_PROPERTY)
                    .isEqualTo(read(goldenPath));
        } else if (Boolean.getBoolean(REGENERATE_PROPERTY)) {
            create(goldenPath, actual);
        } else {
            throw new AssertionError("No golden file " + goldenPath + ": rerun with -D" + REGENERATE_PROPERTY
                    + "=true to record it, then read the diff");
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
