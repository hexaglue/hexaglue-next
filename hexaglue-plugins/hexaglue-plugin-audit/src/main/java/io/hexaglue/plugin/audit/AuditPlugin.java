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

import io.hexaglue.spi.Contribution;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.PluginManifest;
import java.util.List;

/**
 * The report on an architecture: what it is, what holds, what does not, and what it would take.
 *
 * <p>It decides nothing. The kinds were decided by the engine, the violations were found by the
 * engine, the packages were measured by the engine — this reads all three and writes them down.
 * A report that judged on its own would be a second judge, and the day it disagreed with the gate
 * a build would pass while its own report condemned it.</p>
 *
 * @since 7.0.0
 */
public final class AuditPlugin implements HexaGluePlugin {

    /** The identifier other plugins depend on to run after this one. */
    public static final String ID = "io.hexaglue.audit";

    /**
     * Creates the plugin. A host discovers backends by loading them as services, which needs a
     * constructor that is public and takes nothing.
     */
    // Written out rather than left implicit: the strict Javadoc of a published module documents
    // every constructor it exposes, and a service is instantiated through this one.
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public AuditPlugin() {
        // Nothing to hold: a contribution is a function of what it is handed.
    }

    @Override
    public PluginManifest manifest() {
        return new PluginManifest(ID, List.of(), AuditOptions.KEYS);
    }

    @Override
    public void contribute(Contribution contribution) {
        AuditOptions options = AuditOptions.from(contribution.config());
        AuditReport report =
                new AuditReport(contribution.model(), contribution.findings(), contribution.measurements(), options);
        contribution.emit(options.pathOf(AuditReport.NAME), report.render());
        if (options.json()) {
            contribution.emit(options.pathOf(JsonReport.NAME), JsonReport.render(report));
        }
    }
}
