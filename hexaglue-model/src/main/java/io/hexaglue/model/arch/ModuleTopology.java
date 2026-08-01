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

import io.hexaglue.model.TypeId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The architectural reading of the build layout: the modules of the analyzed reactor with their
 * roles, and the assignment of each analyzed type to its module. Empty on a single-module project.
 *
 * <p>Modules keep their declaration order, type assignments iterate in identity order, and
 * construction fails loudly on a duplicate module name, a doubly assigned type or an assignment to
 * an unregistered module. The topology may assign types beyond the classified perimeter — the
 * frontend describes the whole reactor, the perimeter is an engine concern.</p>
 *
 * @since 7.0.0
 */
public final class ModuleTopology {

    private static final ModuleTopology EMPTY = builder().build();

    private final List<ModuleDescriptor> modules;
    private final Map<String, ModuleDescriptor> modulesByName;
    private final SortedMap<TypeId, String> assignments;

    private ModuleTopology(Builder builder) {
        this.modules = List.copyOf(builder.modulesByName.values());
        this.modulesByName = Collections.unmodifiableMap(new LinkedHashMap<>(builder.modulesByName));
        this.assignments = Collections.unmodifiableSortedMap(new TreeMap<>(builder.assignments));
        this.assignments.forEach((typeId, moduleName) -> {
            if (!modulesByName.containsKey(moduleName)) {
                throw new IllegalArgumentException(typeId + " is assigned to unknown module " + moduleName);
            }
        });
    }

    /**
     * Creates a builder for a module topology.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the topology of a single-module project: no modules, no assignments.
     *
     * @return the empty topology
     */
    public static ModuleTopology empty() {
        return EMPTY;
    }

    /**
     * Returns the modules, in declaration order.
     *
     * @return the immutable list of module descriptors
     */
    public List<ModuleDescriptor> modules() {
        return modules;
    }

    /**
     * Returns the module with the given name, when registered.
     *
     * @param name the module name
     * @return the descriptor, or empty when unknown
     */
    public Optional<ModuleDescriptor> module(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return Optional.ofNullable(modulesByName.get(name));
    }

    /**
     * Returns the modules carrying the given role, in declaration order.
     *
     * @param role the architectural role to filter by
     * @return the immutable list of matching descriptors, possibly empty
     */
    public List<ModuleDescriptor> modulesByRole(ModuleRole role) {
        Objects.requireNonNull(role, "role must not be null");
        return modules.stream().filter(module -> module.role() == role).toList();
    }

    /**
     * Returns the module the given type is assigned to.
     *
     * @param typeId the type id
     * @return the descriptor of the owning module, or empty when the type is unassigned
     */
    public Optional<ModuleDescriptor> moduleOf(TypeId typeId) {
        Objects.requireNonNull(typeId, "typeId must not be null");
        return Optional.ofNullable(assignments.get(typeId)).map(modulesByName::get);
    }

    /**
     * Returns the types assigned to the given module, in identity order.
     *
     * @param moduleName the module name
     * @return the immutable list of assigned type ids, possibly empty
     */
    public List<TypeId> typesInModule(String moduleName) {
        Objects.requireNonNull(moduleName, "moduleName must not be null");
        return assignments.entrySet().stream()
                .filter(entry -> entry.getValue().equals(moduleName))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Returns the number of registered modules.
     *
     * @return the module count
     */
    public int size() {
        return modules.size();
    }

    /**
     * Returns whether this topology describes anything at all.
     *
     * @return true when there is no module and no assignment
     */
    public boolean isEmpty() {
        return modules.isEmpty() && assignments.isEmpty();
    }

    /**
     * Builder for {@link ModuleTopology} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final Map<String, ModuleDescriptor> modulesByName = new LinkedHashMap<>();
        private final Map<TypeId, String> assignments = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Registers a module.
         *
         * @param module the module descriptor
         * @return this builder
         * @throws IllegalArgumentException if a module with the same name is already registered
         */
        public Builder addModule(ModuleDescriptor module) {
            Objects.requireNonNull(module, "module must not be null");
            ModuleDescriptor previous = modulesByName.putIfAbsent(module.name(), module);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate module name: " + module.name());
            }
            return this;
        }

        /**
         * Assigns a type to a module. The module may be registered later; membership is verified
         * at build time.
         *
         * @param typeId the type id
         * @param moduleName the name of the owning module
         * @return this builder
         * @throws IllegalArgumentException if the type is already assigned
         */
        public Builder assign(TypeId typeId, String moduleName) {
            Objects.requireNonNull(typeId, "typeId must not be null");
            Objects.requireNonNull(moduleName, "moduleName must not be null");
            String previous = assignments.putIfAbsent(typeId, moduleName);
            if (previous != null) {
                throw new IllegalArgumentException(typeId + " is already assigned to module " + previous);
            }
            return this;
        }

        /**
         * Builds the topology.
         *
         * @return a new ModuleTopology
         * @throws IllegalArgumentException if an assignment points to an unregistered module
         */
        public ModuleTopology build() {
            return new ModuleTopology(this);
        }
    }
}
