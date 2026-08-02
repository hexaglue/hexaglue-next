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

import io.hexaglue.testkit.SourceFixtures;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Compiles Java sources into a class directory, so a test can hand the frontend a real classpath
 * instead of pretending one exists. Reading a framework hierarchy out of bytecode is the point of
 * the supertype closure, and only real class files exercise it.
 */
final class CompiledClasspath {

    private CompiledClasspath() {}

    /**
     * Compiles the given sources and returns the directory holding the resulting classes.
     *
     * @param workDir a directory the compilation can write into
     * @param sourcesByPath source file content by path relative to the source root
     * @return the class output directory, usable as a classpath entry
     */
    static Path of(Path workDir, Map<String, String> sourcesByPath) {
        Path sourceRoot = workDir.resolve("classpath-src");
        Path classes = workDir.resolve("classpath-classes");
        List<Path> files = new ArrayList<>();
        sourcesByPath.forEach((path, content) -> files.add(SourceFixtures.write(sourceRoot, path, content)));
        try {
            Files.createDirectories(classes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create " + classes, e);
        }
        compile(files, classes);
        return classes;
    }

    private static void compile(List<Path> files, Path classes) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("no system Java compiler available");
        }
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(files);
            boolean compiled = compiler.getTask(null, fileManager, null, List.of("-d", classes.toString()), null, units)
                    .call();
            if (!compiled) {
                throw new IllegalStateException("failed to compile the classpath fixtures");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to compile the classpath fixtures", e);
        }
    }
}
