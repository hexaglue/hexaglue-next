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

package io.hexaglue.plugin.audit;

import io.hexaglue.spi.PluginConfig;
import java.util.Objects;
import java.util.Set;

/**
 * What the author asked the report for.
 *
 * <p>There is deliberately no option here for failing a build. Whether a finding stops a build is
 * decided once, by the engine, from {@code validation.findings} — a report that could also decide
 * it would be a second answer to the same question, and the day the two disagreed a build would
 * pass while its own report condemned it. This plugin displays.</p>
 *
 * @param outputDirectory the directory the report goes under, relative to the host's output root
 * @param json whether to write the report as JSON alongside the markdown
 * @param diagrams whether to draw diagrams
 * @since 7.0.0
 */
public record AuditOptions(String outputDirectory, boolean json, boolean diagrams) {

    /** The option keys this plugin answers to. */
    static final Set<String> KEYS = Set.of("outputDirectory", "writeJson", "generateDiagrams");

    private static final String DEFAULT_OUTPUT_DIRECTORY = "audit";

    /**
     * Validates the options.
     */
    public AuditOptions {
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        if (outputDirectory.isBlank()) {
            throw new IllegalArgumentException("outputDirectory must not be blank");
        }
    }

    /**
     * Returns what the plugin does when nobody says otherwise.
     *
     * @return the default options
     */
    public static AuditOptions defaults() {
        return new AuditOptions(DEFAULT_OUTPUT_DIRECTORY, true, true);
    }

    /**
     * Reads the options an author stated, falling back to the defaults for what they left alone.
     *
     * @param config the stated options
     * @return the options
     */
    public static AuditOptions from(PluginConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        AuditOptions defaults = defaults();
        return new AuditOptions(
                config.text("outputDirectory").orElse(defaults.outputDirectory()),
                config.flag("writeJson", defaults.json()),
                config.flag("generateDiagrams", defaults.diagrams()));
    }

    /**
     * Returns the path of a document of this plugin.
     *
     * @param name the document file name
     * @return the path, under the output directory
     */
    String pathOf(String name) {
        return outputDirectory + "/" + name;
    }
}
