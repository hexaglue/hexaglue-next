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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

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
}
