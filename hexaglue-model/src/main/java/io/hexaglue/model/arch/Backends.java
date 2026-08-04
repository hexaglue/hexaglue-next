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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * What the backends a build installed state they will write.
 *
 * <p>A hole the core leaves for the world to fill is normally a thing to report: nothing in the
 * sources fills it, and the core cannot run. On a project that generates its adapters the same
 * sources are right — what fills the hole is written by the build, and a check condemning it would
 * be condemning the very thing the project chose to do.</p>
 *
 * <p>This is a <strong>declaration</strong> and never an observation. It is read from the manifests
 * of the backends the project put on its classpath, before any of them runs, so what the checks
 * make of an architecture never depends on what a run happened to produce — two readings of the
 * same sources judge alike. Nothing here is read by a rule either: what a backend will write is not
 * evidence of what a type is.</p>
 *
 * @param declared what each backend states it will write, by backend identifier
 * @since 7.0.0
 */
public record Backends(Map<String, Set<PortFamily>> declared) {

    /**
     * Validates and copies the declarations, in identifier order so a message reads the same on
     * every run.
     */
    public Backends {
        Objects.requireNonNull(declared, "declared must not be null");
        SortedMap<String, Set<PortFamily>> ordered = new TreeMap<>();
        declared.forEach((backend, families) -> ordered.put(
                Objects.requireNonNull(backend, "a backend identifier must not be null"), Set.copyOf(families)));
        declared = Collections.unmodifiableSortedMap(ordered);
    }

    /**
     * Returns what a build with no backend installed, or none writing adapters, declares.
     *
     * @return the empty declaration
     */
    public static Backends none() {
        return new Backends(Map.of());
    }

    /**
     * Returns the backends that will write an adapter for the given port.
     *
     * @param port the port in question
     * @return their identifiers in alphabetical order, empty when the build writes none
     */
    public List<String> covering(PortType port) {
        Objects.requireNonNull(port, "port must not be null");
        return declared.entrySet().stream()
                .filter(declaration -> declaration.getValue().stream().anyMatch(family -> family.covers(port)))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
