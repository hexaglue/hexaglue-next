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

import io.hexaglue.model.arch.Stability;
import java.util.List;
import java.util.Objects;

/**
 * What was measured about the shape of a codebase, handed over rather than recomputed.
 *
 * <p>A plugin that walked the references again to work out which packages depend on which would be
 * a second opinion on a question that already has an answer, and the two would part ways the day
 * one of them learned something. The engine measures once; this is the reading.</p>
 *
 * <p>This is data, not the machinery that produced it: what a backend gets is the measure, never
 * the means to re-measure.</p>
 *
 * @param packages how settled and how abstract every package is, in name order
 * @param cycles the knots of packages that depend on each other, each knot once
 * @since 7.0.0
 */
public record Measurements(List<Stability> packages, List<List<String>> cycles) {

    /**
     * Validates and copies both readings.
     */
    public Measurements {
        Objects.requireNonNull(packages, "packages must not be null");
        Objects.requireNonNull(cycles, "cycles must not be null");
        packages = List.copyOf(packages);
        cycles = cycles.stream().map(List::copyOf).toList();
    }

    /**
     * Returns nothing measured, for a run with no shape to speak of.
     *
     * @return empty measurements
     */
    public static Measurements none() {
        return new Measurements(List.of(), List.of());
    }

    /**
     * Returns the knots of packages that depend on each other, each knot once.
     *
     * @return the cycles, packages in name order
     */
    @Override
    public List<List<String>> cycles() {
        return List.copyOf(cycles);
    }
}
