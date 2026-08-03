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

import io.hexaglue.model.arch.ArchModel;

/**
 * A backend that turns the classified model into something: documentation, a report, sources.
 *
 * <p>Contributing is a pure function of the model. Everything a plugin needs is in {@link
 * ArchModel} — the verdicts, their provenance, the indexes over the domain, the ports and the
 * composition — so a plugin that re-derives an architectural fact of its own is reading the wrong
 * source. If something is missing there, the model is what grows, not the plugin.</p>
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
     * @param model the classified model, complete and immutable
     * @param config the options stated for this plugin, opaque to every other stage
     * @param sinks where the contribution goes
     */
    void contribute(ArchModel model, PluginConfig config, Sinks sinks);
}
