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

package io.hexaglue.knowledge;

import java.util.List;

/**
 * The packs HexaGlue ships with.
 *
 * <p>They are read through the same strict reader as any user pack, so what the tool claims about
 * Spring or Jakarta is stated in a file a reader can open, disagree with, and replace.</p>
 *
 * <p>Order matters only for reporting: intent comes first, then the application frameworks, then
 * what every classpath carries. No pack outranks another — weighing signals is the engine's
 * work.</p>
 *
 * @since 7.0.0
 */
public final class KnowledgePacks {

    private static final String LOCATION = "io/hexaglue/knowledge/packs/";

    private static final List<String> SHIPPED = List.of("jmolecules", "spring", "jakarta", "platform");

    private static final FrameworkKnowledge EMBEDDED = FrameworkKnowledge.of(SHIPPED.stream()
            .map(id -> PackLoader.loadResource(LOCATION + id + ".yaml"))
            .toList());

    private KnowledgePacks() {}

    /**
     * Returns the knowledge of every shipped pack.
     *
     * @return the embedded framework knowledge
     */
    public static FrameworkKnowledge embedded() {
        return EMBEDDED;
    }
}
