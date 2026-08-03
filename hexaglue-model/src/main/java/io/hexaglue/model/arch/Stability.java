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

import java.util.Objects;

/**
 * How settled a package is, and how abstract.
 *
 * <p>A package that many others depend on and that depends on nothing is hard to change: whatever
 * it does, it does for everyone. That is <em>stable</em> — a property of position in the graph, not
 * a compliment. The two measures matter together: a stable package should be abstract, so that
 * what depends on it depends on a shape rather than on a decision, and an unstable one is free to
 * be concrete because nothing is holding on to it.</p>
 *
 * <p>{@code distance} says how far a package sits from that line. Zero is on it; one is as far as
 * it gets — either concrete and depended upon by everything, or abstract and used by nobody.</p>
 *
 * @param packageName the package measured
 * @param efferent how many packages of the perimeter it depends on
 * @param afferent how many packages of the perimeter depend on it
 * @param instability efferent / (efferent + afferent), 0 when nothing goes either way
 * @param abstractness the share of its types that are interfaces or abstract classes
 * @param distance how far it is from the line where abstractness balances instability
 * @since 7.0.0
 */
public record Stability(
        String packageName, int efferent, int afferent, double instability, double abstractness, double distance) {

    /**
     * Validates the package and the counts.
     */
    public Stability {
        Objects.requireNonNull(packageName, "packageName must not be null");
        if (efferent < 0 || afferent < 0) {
            throw new IllegalArgumentException("coupling counts must not be negative: " + efferent + ", " + afferent);
        }
    }

    /**
     * Measures a package from its couplings and the shape of its types.
     *
     * @param packageName the package measured
     * @param efferent how many packages it depends on
     * @param afferent how many packages depend on it
     * @param abstractTypes how many of its types are interfaces or abstract classes
     * @param totalTypes how many types it holds
     * @return the measure
     */
    public static Stability of(String packageName, int efferent, int afferent, int abstractTypes, int totalTypes) {
        int coupling = efferent + afferent;
        double instability = coupling == 0 ? 0.0 : (double) efferent / coupling;
        double abstractness = totalTypes == 0 ? 0.0 : (double) abstractTypes / totalTypes;
        return new Stability(
                packageName, efferent, afferent, instability, abstractness, Math.abs(abstractness + instability - 1));
    }
}
