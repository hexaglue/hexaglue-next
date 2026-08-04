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

package io.hexaglue.spi;

import io.hexaglue.model.arch.PortFamily;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * What a plugin declares about itself before it runs: who it is, what must run first, and which
 * options it answers to.
 *
 * <p>The options are declared rather than discovered so that a misspelt key is refused with the
 * vocabulary that would have been right, the way the configuration loader refuses an unknown key
 * one stage earlier. A plugin that reads an option it never declared reads nothing.</p>
 *
 * <p>What it produces is declared for the same reason, one stage later: on a project whose adapters
 * are generated, a port nothing implements is not a hole but a hole this build fills, and the
 * checks have no other way of telling the two apart. It is stated before the plugin runs and by
 * family rather than by port, so that what a backend does not cover keeps being reported.</p>
 *
 * @param id the plugin identifier, in reverse domain notation
 * @param dependsOn the identifiers of the plugins that must run first, in declaration order
 * @param options the option keys this plugin answers to, iterated in alphabetical order
 * @param produces the families of port this plugin writes adapters for
 * @since 7.0.0
 */
public record PluginManifest(String id, List<String> dependsOn, Set<String> options, Set<PortFamily> produces) {

    /**
     * Validates the identifier and copies the collections.
     */
    public PluginManifest {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(dependsOn, "dependsOn must not be null");
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(produces, "produces must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("plugin id must not be blank");
        }
        dependsOn = List.copyOf(dependsOn);
        if (Set.copyOf(dependsOn).size() != dependsOn.size()) {
            throw new IllegalArgumentException("plugin " + id + " declares the same dependency twice: " + dependsOn);
        }
        options = Collections.unmodifiableSortedSet(new TreeSet<>(options));
        produces = Set.copyOf(produces);
    }

    /**
     * Creates the manifest of a plugin that writes no adapter, which is every plugin that reads the
     * model without filling anything the core left open.
     *
     * @param id the plugin identifier
     * @param dependsOn the identifiers of the plugins that must run first
     * @param options the option keys this plugin answers to
     */
    public PluginManifest(String id, List<String> dependsOn, Set<String> options) {
        this(id, dependsOn, options, Set.of());
    }

    /**
     * Creates the manifest of a plugin that depends on nothing and reads no option.
     *
     * @param id the plugin identifier
     * @return a new manifest
     */
    public static PluginManifest of(String id) {
        return new PluginManifest(id, List.of(), Set.of());
    }
}
