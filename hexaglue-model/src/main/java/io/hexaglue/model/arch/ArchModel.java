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
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.ProofNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The classified architectural model: the single source of truth every plugin consumes. It holds
 * the {@link ArchType} verdicts, exposes typed provenance access — {@link #classificationOf} and
 * {@link #explain} — and precomputed indexes over the domain, the ports, the composition and the
 * module topology.
 *
 * <p>Every type present carries its complete verdict by construction, unclassified fallback
 * included, so no analyzed type is ever silently absent. Types iterate in identity order and the
 * indexes derive from that order, so any traversal is deterministic. Construction fails loudly on
 * a duplicate type id.</p>
 *
 * @since 7.0.0
 */
public final class ArchModel {

    private final Map<TypeId, ArchType> typesById;
    private final ModuleTopology moduleTopology;
    private final DomainIndex domainIndex;
    private final PortIndex portIndex;
    private final CompositionIndex compositionIndex;

    private ArchModel(Builder builder) {
        this.typesById = Collections.unmodifiableSortedMap(indexTypes(builder.types));
        this.moduleTopology = builder.moduleTopology;
        this.domainIndex = new DomainIndex(this.typesById);
        this.portIndex = new PortIndex(this.typesById);
        this.compositionIndex = new CompositionIndex(this.typesById);
    }

    private static SortedMap<TypeId, ArchType> indexTypes(List<ArchType> types) {
        SortedMap<TypeId, ArchType> byId = new TreeMap<>();
        for (ArchType type : types) {
            ArchType previous = byId.putIfAbsent(type.id(), type);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate type id: " + type.id());
            }
        }
        return byId;
    }

    /**
     * Creates a builder for an architectural model.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns every classified type, in identity order.
     *
     * @return the immutable list of types
     */
    public List<ArchType> types() {
        return List.copyOf(typesById.values());
    }

    /**
     * Returns the classified type with the given id, when present.
     *
     * @param id the type id
     * @return the type, or empty when unknown
     */
    public Optional<ArchType> type(TypeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(typesById.get(id));
    }

    /**
     * Returns every type matching the given arch type, in identity order. This is the idiomatic
     * plugin access: {@code model.all(AggregateRoot.class)} — sealed branches such as
     * {@code DomainType} match too.
     *
     * @param <T> the arch type to select
     * @param type the class of the arch type to select
     * @return the stream of matching types
     */
    public <T extends ArchType> Stream<T> all(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return typesById.values().stream().filter(type::isInstance).map(type::cast);
    }

    /**
     * Returns the complete verdict on the given type.
     *
     * @param id the type id
     * @return the classification, or empty when the id is unknown
     */
    public Optional<Classification> classificationOf(TypeId id) {
        return type(id).map(ArchType::classification);
    }

    /**
     * Returns the proof tree explaining how the verdict on the given type was derived.
     *
     * @param id the type id
     * @return the proof, or empty when the id is unknown
     */
    public Optional<ProofNode> explain(TypeId id) {
        return classificationOf(id).map(Classification::proof);
    }

    /**
     * Returns the typed access to the domain side of the model.
     *
     * @return the domain index
     */
    public DomainIndex domainIndex() {
        return domainIndex;
    }

    /**
     * Returns the typed access to the ports of the model.
     *
     * @return the port index
     */
    public PortIndex portIndex() {
        return portIndex;
    }

    /**
     * Returns the navigation over the compositional facts of the model.
     *
     * @return the composition index
     */
    public CompositionIndex compositionIndex() {
        return compositionIndex;
    }

    /**
     * Returns the architectural reading of the build layout, empty on a single-module project.
     *
     * @return the module topology
     */
    public ModuleTopology moduleTopology() {
        return moduleTopology;
    }

    /**
     * Builder for {@link ArchModel} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final List<ArchType> types = new ArrayList<>();
        private ModuleTopology moduleTopology = ModuleTopology.empty();

        private Builder() {}

        /**
         * Adds a classified type.
         *
         * @param type the classified type
         * @return this builder
         */
        public Builder addType(ArchType type) {
            types.add(Objects.requireNonNull(type, "type must not be null"));
            return this;
        }

        /**
         * Sets the module topology.
         *
         * @param moduleTopology the topology of the analyzed reactor
         * @return this builder
         */
        public Builder moduleTopology(ModuleTopology moduleTopology) {
            this.moduleTopology = Objects.requireNonNull(moduleTopology, "moduleTopology must not be null");
            return this;
        }

        /**
         * Builds the architectural model.
         *
         * @return a new ArchModel
         */
        public ArchModel build() {
            return new ArchModel(this);
        }
    }
}
