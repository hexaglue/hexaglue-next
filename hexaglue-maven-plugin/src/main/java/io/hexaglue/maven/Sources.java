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

import io.hexaglue.spi.SourceFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Puts on disk the types the backends wrote, under one root the compiler is then told about.
 *
 * <p>A backend never touches the disk: it hands over a package, a name and a body, and the writing
 * happens here. The names were held to being Java identifiers when the file was built, so nothing
 * that arrives here can walk out of the directory it is written under.</p>
 *
 * <p>Generated types go under the build directory and never into the project's own sources. A
 * generator writing where an author writes turns every regeneration into a diff to review, and the
 * first hand edit into something the next run silently undoes.</p>
 */
final class Sources {

    private Sources() {}

    /**
     * Returns the types this project is the one to write.
     *
     * <p>A backend may route what it writes to another module of the build. This goal runs inside
     * one module and writes into that module's build directory, so what was addressed elsewhere is
     * not this run's to place — it is left to the run of the module it names, and said rather than
     * dropped.</p>
     *
     * @param sources everything the backends handed over
     * @param module the artifact identifier of the project being built
     * @return the sources belonging to this module, those stating no module included
     */
    static List<SourceFile> addressedTo(List<SourceFile> sources, String module) {
        Objects.requireNonNull(sources, "sources must not be null");
        Objects.requireNonNull(module, "module must not be null");
        return sources.stream()
                .filter(source -> source.module().map(module::equals).orElse(true))
                .toList();
    }

    /**
     * Returns the types addressed to another module than the one being built.
     *
     * @param sources everything the backends handed over
     * @param module the artifact identifier of the project being built
     * @return the sources this run is not the one to write
     */
    static List<SourceFile> addressedElsewhere(List<SourceFile> sources, String module) {
        Objects.requireNonNull(sources, "sources must not be null");
        Objects.requireNonNull(module, "module must not be null");
        return sources.stream()
                .filter(source ->
                        source.module().map(named -> !named.equals(module)).orElse(false))
                .toList();
    }

    /**
     * Writes the given types under one root.
     *
     * @param sources the types to write
     * @param root the source root everything is written under
     * @throws MojoExecutionException when a type cannot be written
     */
    static void write(List<SourceFile> sources, Path root) throws MojoExecutionException {
        Objects.requireNonNull(sources, "sources must not be null");
        Objects.requireNonNull(root, "root must not be null");
        for (SourceFile source : sources) {
            Path target = root.resolve(source.path());
            Path directory = target.getParent();
            try {
                Files.createDirectories(directory == null ? root : directory);
                Files.writeString(target, source.content(), StandardCharsets.UTF_8);
            } catch (IOException unwritable) {
                throw new MojoExecutionException(
                        target + " could not be written: " + unwritable.getMessage(), unwritable);
            }
        }
    }
}
