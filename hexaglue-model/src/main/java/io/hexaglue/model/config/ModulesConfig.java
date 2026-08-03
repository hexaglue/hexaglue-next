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

import io.hexaglue.model.arch.ModuleRole;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * What role each module of a reactor plays, as the project declares it.
 *
 * <p>The role is declared and never guessed. A build module is named by whoever laid the build out,
 * and the words they chose — {@code -infra}, {@code -core}, {@code -api} — are habits of a team, not
 * facts about an architecture: a table of suffixes deciding where the domain lives would be an
 * opinion nobody can configure away, and it would be wrong on the first project that names its
 * modules after its bounded contexts.</p>
 *
 * <p>A module absent from here is a module with no declared role. It is not given one: there is no
 * neutral value to fall back on, and inventing a default would put a reading in the model that
 * nobody stated. The analysis says so instead, and everything reading roles simply has less to
 * read.</p>
 *
 * <p>This record is shape only: reading the roles belongs to the engine, binding them from a
 * document belongs to the host.</p>
 *
 * @param roles the architectural role of each module, by module name
 * @since 7.0.0
 */
public record ModulesConfig(Map<String, ModuleRole> roles) {

    /**
     * Validates the declarations and copies them into a name-ordered view.
     */
    public ModulesConfig {
        Objects.requireNonNull(roles, "roles must not be null");
        SortedMap<String, ModuleRole> declared = new TreeMap<>();
        roles.forEach((name, role) -> {
            Objects.requireNonNull(name, "module name must not be null");
            Objects.requireNonNull(role, "module role must not be null");
            if (name.isBlank()) {
                throw new IllegalArgumentException("a module name must not be blank, got a role stated for one");
            }
            declared.put(name, role);
        });
        roles = Collections.unmodifiableSortedMap(declared);
    }

    /**
     * Returns the documented default posture: no role declared, which is what a single-module
     * project has to say about modules.
     *
     * @return the default configuration
     */
    public static ModulesConfig defaults() {
        return new ModulesConfig(Map.of());
    }

    /**
     * Returns the role the project declared for the given module.
     *
     * @param moduleName the module to look up
     * @return the declared role, or empty when the project said nothing about this module
     */
    public Optional<ModuleRole> roleOf(String moduleName) {
        Objects.requireNonNull(moduleName, "moduleName must not be null");
        return Optional.ofNullable(roles.get(moduleName));
    }
}
