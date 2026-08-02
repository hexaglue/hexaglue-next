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

package io.hexaglue.model.arch;

import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeRef;
import java.util.List;

/**
 * An adapter read from the analyzed sources: the ring around the hexagon, where the technical world
 * meets the ports.
 *
 * <p>Adapters are classified so that every type of the perimeter receives a verdict — an audit of an
 * application under migration must be able to name its controllers and its persistence code instead
 * of leaving them to be guessed from package names. They are not a generation target: an adapter
 * emitted by a plugin is an output of the pipeline, never an input of classification.</p>
 *
 * @since 7.0.0
 */
public sealed interface AdapterType extends ArchType permits DrivingAdapter, DrivenAdapter {

    /**
     * Returns the side of the hexagon this adapter stands on.
     *
     * @return the direction
     */
    PortDirection direction();

    /**
     * Returns the ports this adapter is wired to, whichever side it stands on.
     *
     * @return the wired ports, in the order the engine established them
     */
    List<TypeRef> ports();

    /**
     * Returns whether the engine tied this adapter to at least one port of the model. An adapter
     * wired to none reaches the hexagon another way, or does not reach it at all — both are worth
     * reporting.
     *
     * @return true when at least one port is wired
     */
    default boolean isConnected() {
        return !ports().isEmpty();
    }
}
