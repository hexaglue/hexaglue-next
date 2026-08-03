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

/**
 * A backend that turns the classified model into something: documentation, a report, sources.
 *
 * <p>Contributing is a pure function of what it is handed. Everything a plugin needs is in the
 * {@link Contribution} — the verdicts, their provenance, the indexes, and what the checks
 * concluded — so a plugin that re-derives an architectural fact of its own is reading the wrong
 * source. If something is missing there, the contribution is what grows, not the plugin.</p>
 *
 * <p>A plugin emits into the sinks and never writes anything itself. The host decides where the
 * output goes, which is what confines it.</p>
 *
 * @since 7.0.0
 */
public interface HexaGluePlugin {

    /**
     * Returns what this plugin declares about itself.
     *
     * @return the manifest, never null
     */
    PluginManifest manifest();

    /**
     * Contributes to the run.
     *
     * @param contribution what this plugin reads and where its result goes
     */
    void contribute(Contribution contribution);
}
