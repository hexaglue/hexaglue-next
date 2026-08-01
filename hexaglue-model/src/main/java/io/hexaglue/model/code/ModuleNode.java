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

package io.hexaglue.model.code;

import java.util.Objects;
import java.util.Optional;

/**
 * A build module of the analyzed reactor. Types point to their module through
 * {@code TypeNode#moduleName()}; the architectural reading of modules (roles, topology) belongs to
 * the architectural model.
 *
 * @param name the module name (typically the Maven artifactId)
 * @param basePackage the module's base package, when known
 * @since 7.0.0
 */
public record ModuleNode(String name, Optional<String> basePackage) {

    /**
     * Validates the name.
     */
    public ModuleNode {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(basePackage, "basePackage must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /**
     * Creates a module node without a known base package.
     *
     * @param name the module name
     * @return a new ModuleNode
     */
    public static ModuleNode of(String name) {
        return new ModuleNode(name, Optional.empty());
    }
}
