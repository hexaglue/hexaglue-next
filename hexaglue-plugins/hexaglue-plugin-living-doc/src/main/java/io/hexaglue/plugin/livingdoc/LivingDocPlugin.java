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

import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.spi.Document;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.PluginConfig;
import io.hexaglue.spi.PluginManifest;
import io.hexaglue.spi.Sinks;
import java.util.List;

/**
 * Documentation that cannot drift from the code it describes.
 *
 * <p>Three pages, all derived from the classified model: the way in, the domain, the boundary.
 * Nothing is read from anywhere else — no convention over the file tree, no second reading of the
 * source — so a page is wrong only if the model is, and then it is the model that gets fixed.</p>
 *
 * @since 7.0.0
 */
public final class LivingDocPlugin implements HexaGluePlugin {

    /** The identifier other plugins depend on to run after this one. */
    public static final String ID = "io.hexaglue.living-doc";

    /**
     * Creates the plugin. A host discovers backends by loading them as services, which needs a
     * constructor that is public and takes nothing.
     */
    // Written out rather than left implicit: the strict Javadoc of a published module documents
    // every constructor it exposes, and a service is instantiated through this one.
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public LivingDocPlugin() {
        // Nothing to hold: a contribution is a function of the model it is handed.
    }

    @Override
    public PluginManifest manifest() {
        return new PluginManifest(ID, List.of(), LivingDocOptions.KEYS);
    }

    @Override
    public void contribute(ArchModel model, PluginConfig config, Sinks sinks) {
        LivingDocOptions options = LivingDocOptions.from(config);
        emit(sinks, options, OverviewDocument.NAME, new OverviewDocument(model).render());
        emit(sinks, options, DomainDocument.NAME, new DomainDocument(model, options).render());
        emit(sinks, options, PortsDocument.NAME, new PortsDocument(model, options).render());
    }

    private static void emit(Sinks sinks, LivingDocOptions options, String name, String content) {
        sinks.documents().emit(new Document(options.pathOf(name), content));
    }
}
