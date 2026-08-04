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

import io.hexaglue.spi.Document;
import io.hexaglue.spi.PluginRun;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Puts on disk what the backends produced, and says what the run refused.
 *
 * <p>A plugin never touches the disk itself: it hands over a relative path and a body, and the
 * writing happens here, under one directory. That is what makes the confinement a property of the
 * shape rather than of every backend's good behaviour — and it is why the two goals that run
 * backends share this rather than each having its own idea of where output goes.</p>
 */
final class Documents {

    private Documents() {}

    /**
     * Says what the run refused before saying what it produced: a document missing because a plugin
     * failed is worth hearing about at the moment it is missing.
     *
     * @param run what the backends produced and what the run refused
     * @param log where to say it
     */
    static void report(PluginRun run, Log log) {
        Objects.requireNonNull(run, "run must not be null");
        Diagnostics.report(run.diagnostics(), log);
        if (!run.skipped().isEmpty()) {
            log.warn("HexaGlue skipped " + String.join(", ", run.skipped()));
        }
        // A goal that reports does not compile anything, so the types a generating backend handed
        // over have nowhere to go here. Dropping them without a word would look like a backend that
        // generated nothing.
        if (!run.sources().isEmpty()) {
            log.warn(
                    "HexaGlue was handed " + run.sources().size()
                            + " generated type(s) by a backend, which this goal does not write: run hexaglue:generate for those");
        }
    }

    /**
     * Writes the documents under one directory. The paths were confined when they were built, so
     * nothing here can land outside it.
     *
     * @param documents what the backends produced
     * @param root the directory everything is written under
     * @throws MojoExecutionException when a document cannot be written
     */
    static void write(List<Document> documents, Path root) throws MojoExecutionException {
        Objects.requireNonNull(documents, "documents must not be null");
        Objects.requireNonNull(root, "root must not be null");
        for (Document document : documents) {
            Path target = root.resolve(document.path());
            Path directory = target.getParent();
            try {
                Files.createDirectories(directory == null ? root : directory);
                Files.writeString(target, document.content(), StandardCharsets.UTF_8);
            } catch (IOException unwritable) {
                throw new MojoExecutionException(
                        target + " could not be written: " + unwritable.getMessage(), unwritable);
            }
        }
    }
}
