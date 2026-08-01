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

import io.hexaglue.model.EnumSets;
import io.hexaglue.model.TypeId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The immutable base of syntactic facts produced by the frontend: type nodes (stubs included),
 * modules, typed edges with provenance, the precomputed supertype closure and, when the
 * capability was requested, method-body facts.
 *
 * <p>Types iterate in identity order and per-type edge lists are indexed at construction, so any
 * traversal of the model is deterministic without further sorting. Construction fails loudly on a
 * duplicate type id or on body facts present without their capability.</p>
 *
 * @since 7.0.0
 */
public final class CodeModel {

    private final Map<TypeId, TypeNode> typesById;
    private final List<ModuleNode> modules;
    private final List<Edge> edges;
    private final Map<TypeId, List<Edge>> edgesBySource;
    private final Map<TypeId, List<Edge>> edgesByTarget;
    private final Map<TypeId, List<TypeId>> supertypeClosure;
    private final Set<CodeModelCapability> capabilities;
    private final List<MethodBodyFacts> bodyFacts;

    private CodeModel(Builder builder) {
        this.typesById = Collections.unmodifiableSortedMap(indexTypes(builder.types));
        this.modules = List.copyOf(builder.modules);
        this.edges = List.copyOf(builder.edges);
        this.edgesBySource = indexEdges(this.edges, true);
        this.edgesByTarget = indexEdges(this.edges, false);
        this.supertypeClosure = copyClosure(builder.supertypeClosure);
        this.capabilities = EnumSets.ordered(builder.capabilities);
        this.bodyFacts = List.copyOf(builder.bodyFacts);
        if (!this.bodyFacts.isEmpty() && !this.capabilities.contains(CodeModelCapability.METHOD_BODIES)) {
            throw new IllegalArgumentException("body facts present without the METHOD_BODIES capability");
        }
    }

    private static SortedMap<TypeId, TypeNode> indexTypes(List<TypeNode> types) {
        SortedMap<TypeId, TypeNode> byId = new TreeMap<>();
        for (TypeNode type : types) {
            TypeNode previous = byId.putIfAbsent(type.id(), type);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate type id: " + type.id());
            }
        }
        return byId;
    }

    private static Map<TypeId, List<Edge>> indexEdges(List<Edge> edges, boolean bySource) {
        SortedMap<TypeId, List<Edge>> collecting = new TreeMap<>();
        for (Edge edge : edges) {
            TypeId key = bySource ? edge.source() : edge.target();
            collecting.computeIfAbsent(key, unused -> new ArrayList<>()).add(edge);
        }
        SortedMap<TypeId, List<Edge>> immutable = new TreeMap<>();
        collecting.forEach((key, list) -> immutable.put(key, List.copyOf(list)));
        return Collections.unmodifiableSortedMap(immutable);
    }

    private static Map<TypeId, List<TypeId>> copyClosure(Map<TypeId, List<TypeId>> closure) {
        SortedMap<TypeId, List<TypeId>> copy = new TreeMap<>();
        closure.forEach((key, supertypes) -> copy.put(key, List.copyOf(supertypes)));
        return Collections.unmodifiableSortedMap(copy);
    }

    /**
     * Creates a builder for a code model.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns every type node, in identity order.
     *
     * @return the immutable list of types
     */
    public List<TypeNode> types() {
        return List.copyOf(typesById.values());
    }

    /**
     * Returns the type node with the given id, when present.
     *
     * @param id the type id
     * @return the node, or empty when unknown
     */
    public Optional<TypeNode> type(TypeId id) {
        return Optional.ofNullable(typesById.get(id));
    }

    /**
     * Returns the modules of the analyzed reactor, in the order the frontend declared them.
     *
     * @return the immutable list of modules
     */
    public List<ModuleNode> modules() {
        return modules;
    }

    /**
     * Returns every edge, in the order the frontend declared them.
     *
     * @return the immutable list of edges
     */
    public List<Edge> edges() {
        return edges;
    }

    /**
     * Returns the edges starting from the given type.
     *
     * @param source the source type id
     * @return the immutable list of outgoing edges, possibly empty
     */
    public List<Edge> edgesFrom(TypeId source) {
        return edgesBySource.getOrDefault(source, List.of());
    }

    /**
     * Returns the edges pointing to the given type.
     *
     * @param target the target type id
     * @return the immutable list of incoming edges, possibly empty
     */
    public List<Edge> edgesTo(TypeId target) {
        return edgesByTarget.getOrDefault(target, List.of());
    }

    /**
     * Returns the precomputed transitive supertypes of the given type, classpath included.
     *
     * @param id the type id
     * @return the immutable supertype list, possibly empty
     */
    public List<TypeId> supertypesOf(TypeId id) {
        return supertypeClosure.getOrDefault(id, List.of());
    }

    /**
     * Returns the capabilities the frontend ran with.
     *
     * @return the immutable capability set
     */
    public Set<CodeModelCapability> capabilities() {
        return capabilities;
    }

    /**
     * Returns the method-body facts, empty unless the METHOD_BODIES capability was requested.
     *
     * @return the immutable list of body facts
     */
    public List<MethodBodyFacts> bodyFacts() {
        return bodyFacts;
    }

    /**
     * Builder for {@link CodeModel} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final List<TypeNode> types = new ArrayList<>();
        private final List<ModuleNode> modules = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final Map<TypeId, List<TypeId>> supertypeClosure = new LinkedHashMap<>();
        private final Set<CodeModelCapability> capabilities = EnumSet.noneOf(CodeModelCapability.class);
        private final List<MethodBodyFacts> bodyFacts = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a type node.
         *
         * @param type the type node
         * @return this builder
         */
        public Builder addType(TypeNode type) {
            types.add(Objects.requireNonNull(type, "type must not be null"));
            return this;
        }

        /**
         * Adds a module node.
         *
         * @param module the module node
         * @return this builder
         */
        public Builder addModule(ModuleNode module) {
            modules.add(Objects.requireNonNull(module, "module must not be null"));
            return this;
        }

        /**
         * Adds an edge.
         *
         * @param edge the edge
         * @return this builder
         */
        public Builder addEdge(Edge edge) {
            edges.add(Objects.requireNonNull(edge, "edge must not be null"));
            return this;
        }

        /**
         * Records the precomputed transitive supertypes of a type.
         *
         * @param id the type id
         * @param supertypes the transitive supertypes, nearest first
         * @return this builder
         */
        public Builder supertypes(TypeId id, List<TypeId> supertypes) {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(supertypes, "supertypes must not be null");
            supertypeClosure.put(id, List.copyOf(supertypes));
            return this;
        }

        /**
         * Declares a frontend capability.
         *
         * @param capability the capability that ran
         * @return this builder
         */
        public Builder capability(CodeModelCapability capability) {
            capabilities.add(Objects.requireNonNull(capability, "capability must not be null"));
            return this;
        }

        /**
         * Adds the facts of one method body.
         *
         * @param facts the body facts
         * @return this builder
         */
        public Builder addBodyFacts(MethodBodyFacts facts) {
            bodyFacts.add(Objects.requireNonNull(facts, "facts must not be null"));
            return this;
        }

        /**
         * Builds the code model.
         *
         * @return a new CodeModel
         */
        public CodeModel build() {
            return new CodeModel(this);
        }
    }
}
