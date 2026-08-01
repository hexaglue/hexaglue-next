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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Writes Java source fixtures into a directory tree, typically a JUnit {@code @TempDir}.
 *
 * <p>Fixtures are plain source files handed to the analysis frontend; they are parsed, never
 * compiled, so they only need to be syntactically valid Java.
 *
 * @since 7.0.0
 */
public final class SourceFixtures {

    private SourceFixtures() {}

    /**
     * Writes {@code content} at {@code relativePath} under {@code root}, creating parent
     * directories as needed.
     *
     * @param root the directory receiving the fixture tree
     * @param relativePath the file path relative to {@code root}, e.g. {@code com/acme/Order.java}
     * @param content the source file content
     * @return the path of the written file
     * @throws IllegalArgumentException if {@code relativePath} is blank or absolute
     * @throws UncheckedIOException if the file cannot be written
     */
    public static Path write(Path root, String relativePath, String content) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(content, "content");
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
        if (Path.of(relativePath).isAbsolute()) {
            throw new IllegalArgumentException("relativePath must be relative, got: " + relativePath);
        }
        Path filePath = root.resolve(relativePath);
        try {
            Files.createDirectories(Objects.requireNonNull(filePath.getParent(), "fixture parent directory"));
            Files.writeString(filePath, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write fixture " + relativePath + " under " + root, e);
        }
        return filePath;
    }
}
