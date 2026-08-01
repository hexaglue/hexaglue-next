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
import java.util.Optional;

/**
 * The architectural identity of a build module: its name, its {@link ModuleRole} and its base
 * package when one is known. Physical layout (directories, source roots) is deliberately absent —
 * the model carries no I/O concern; routing generated files to a directory belongs to the sinks
 * and their configuration.
 *
 * @param name the module name, typically the Maven artifactId
 * @param role the architectural role of the module
 * @param basePackage the module's base package, when known
 * @since 7.0.0
 */
public record ModuleDescriptor(String name, ModuleRole role, Optional<String> basePackage) {

    /**
     * Validates the name.
     */
    public ModuleDescriptor {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(basePackage, "basePackage must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /**
     * Creates a descriptor without a known base package.
     *
     * @param name the module name
     * @param role the architectural role of the module
     * @return a new ModuleDescriptor
     */
    public static ModuleDescriptor of(String name, ModuleRole role) {
        return new ModuleDescriptor(name, role, Optional.empty());
    }
}
