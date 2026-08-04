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

import io.hexaglue.model.arch.Backends;
import io.hexaglue.model.arch.PortFamily;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

/**
 * Finding the backends a build put on its classpath.
 *
 * <p>A plugin is installed by being there: a project adds the dependency, and the run picks it up.
 * Nothing lists them anywhere, because a list is a second place to keep in step with the first.</p>
 *
 * <p>Loading is where a stale or half-built plugin blows up, so it blows up here rather than in the
 * middle of a run: a service that cannot even be constructed is left out, named, and the others go
 * ahead.</p>
 *
 * @since 7.0.0
 */
public final class PluginDiscovery {

    private PluginDiscovery() {}

    /**
     * Returns the plugins a classpath offers, in identifier order so a run does not depend on the
     * order a class loader happens to hand them over in.
     *
     * @param loader where to look
     * @return the plugins found, empty when none are installed
     */
    public static List<HexaGluePlugin> on(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader must not be null");
        List<HexaGluePlugin> plugins = new ArrayList<>();
        // A provider that fails to load is exactly what the executor is built to survive; iterating
        // by hand is what lets the ones after it still be found.
        for (ServiceLoader.Provider<HexaGluePlugin> provider :
                ServiceLoader.load(HexaGluePlugin.class, loader).stream().toList()) {
            plugins.add(provider.get());
        }
        plugins.sort(
                (left, right) -> left.manifest().id().compareTo(right.manifest().id()));
        return List.copyOf(plugins);
    }

    /**
     * Returns what the given backends state they will write, for the checks to read.
     *
     * <p>Read from the manifests and never from a run: this is handed to the analysis before any
     * backend contributes anything, so a build judges its sources the same way whether or not the
     * generation that follows succeeds.</p>
     *
     * @param plugins the backends installed on this build
     * @return what they declare, empty when none writes an adapter
     */
    public static Backends declaredBy(List<HexaGluePlugin> plugins) {
        Objects.requireNonNull(plugins, "plugins must not be null");
        Map<String, Set<PortFamily>> declared = new TreeMap<>();
        for (HexaGluePlugin plugin : plugins) {
            PluginManifest manifest = plugin.manifest();
            if (!manifest.produces().isEmpty()) {
                declared.merge(manifest.id(), manifest.produces(), PluginDiscovery::both);
            }
        }
        return new Backends(declared);
    }

    /** Two backends under one identifier is the executor's problem to report, not a reason to fail here. */
    private static Set<PortFamily> both(Set<PortFamily> first, Set<PortFamily> second) {
        Set<PortFamily> union = new LinkedHashSet<>(first);
        union.addAll(second);
        return union;
    }
}
