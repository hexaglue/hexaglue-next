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

package io.hexaglue.plugin.livingdoc;

import io.hexaglue.spi.PluginConfig;
import java.util.Objects;
import java.util.Set;

/**
 * What the author asked this plugin for.
 *
 * <p>The output directory is a prefix on the paths the plugin emits, not a place on a disk: where
 * the documents finally land is the host's decision, and a plugin that named an absolute path
 * would take that decision away from it.</p>
 *
 * @param outputDirectory the directory the documents go under, relative to the host's output root
 * @param diagrams whether to draw diagrams
 * @param propertiesPerDiagram how many properties of a type a diagram shows before it stops
 * @param provenance whether each type says what it was classified on
 * @since 7.0.0
 */
public record LivingDocOptions(String outputDirectory, boolean diagrams, int propertiesPerDiagram, boolean provenance) {

    /** The option keys this plugin answers to. */
    static final Set<String> KEYS =
            Set.of("outputDirectory", "generateDiagrams", "propertiesPerDiagram", "includeProvenance");

    private static final String DEFAULT_OUTPUT_DIRECTORY = "living-doc";
    private static final int DEFAULT_PROPERTIES_PER_DIAGRAM = 5;

    /**
     * Validates the options.
     */
    public LivingDocOptions {
        Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
        if (outputDirectory.isBlank()) {
            throw new IllegalArgumentException("outputDirectory must not be blank");
        }
        if (propertiesPerDiagram < 0) {
            throw new IllegalArgumentException(
                    "propertiesPerDiagram must not be negative, got: " + propertiesPerDiagram);
        }
    }

    /**
     * Returns what the plugin does when nobody says otherwise: documents under {@code living-doc},
     * diagrams drawn, five properties each, and every verdict saying what it rests on.
     *
     * @return the default options
     */
    public static LivingDocOptions defaults() {
        return new LivingDocOptions(DEFAULT_OUTPUT_DIRECTORY, true, DEFAULT_PROPERTIES_PER_DIAGRAM, true);
    }

    /**
     * Reads the options an author stated, falling back to the defaults for what they left alone.
     *
     * @param config the stated options
     * @return the options
     */
    public static LivingDocOptions from(PluginConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        LivingDocOptions defaults = defaults();
        return new LivingDocOptions(
                config.text("outputDirectory").orElse(defaults.outputDirectory()),
                config.flag("generateDiagrams", defaults.diagrams()),
                config.number("propertiesPerDiagram", defaults.propertiesPerDiagram()),
                config.flag("includeProvenance", defaults.provenance()));
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
