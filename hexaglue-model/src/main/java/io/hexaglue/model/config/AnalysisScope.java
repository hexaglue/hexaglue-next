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

package io.hexaglue.model.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The perimeter of the analysis: the base package anchoring relative readings (bounded contexts
 * among them) and the package prefixes delimiting what is analyzed. Prefixes are plain package
 * names — never globs, never simple names — so a scope reads without pattern semantics.
 *
 * <p>This record is shape only: interpreting the scope belongs to the engine, and binding it
 * from YAML belongs to the loader.</p>
 *
 * @param basePackage the package anchoring relative readings, when delimited
 * @param includePackages the package prefixes to analyze; empty means everything
 * @param excludePackages the package prefixes to leave out
 * @since 7.0.0
 */
public record AnalysisScope(Optional<String> basePackage, List<String> includePackages, List<String> excludePackages) {

    /**
     * Validates the prefixes and copies the lists.
     */
    public AnalysisScope {
        Objects.requireNonNull(basePackage, "basePackage must not be null");
        Objects.requireNonNull(includePackages, "includePackages must not be null");
        Objects.requireNonNull(excludePackages, "excludePackages must not be null");
        basePackage.ifPresent(pkg -> requirePackagePrefix(pkg, "basePackage"));
        includePackages.forEach(prefix -> requirePackagePrefix(prefix, "include package prefix"));
        excludePackages.forEach(prefix -> requirePackagePrefix(prefix, "exclude package prefix"));
        includePackages = List.copyOf(includePackages);
        excludePackages = List.copyOf(excludePackages);
    }

    private static void requirePackagePrefix(String value, String role) {
        Objects.requireNonNull(value, role + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(role + " must not be blank");
        }
        if (value.indexOf('*') >= 0) {
            throw new IllegalArgumentException(role + " must be a plain package prefix, not a glob pattern: " + value);
        }
    }

    /**
     * Returns the scope covering the whole reactor: no base package, no inclusion, no exclusion.
     *
     * @return the unrestricted scope
     */
    public static AnalysisScope everything() {
        return new AnalysisScope(Optional.empty(), List.of(), List.of());
    }
}
