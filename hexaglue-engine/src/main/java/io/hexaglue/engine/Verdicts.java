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

package io.hexaglue.engine;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.Classification;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * What the engine concluded about every type of the perimeter, at one point of the analysis.
 *
 * <p>Verdicts sit outside the fact base on purpose. Facts only ever accumulate, and a verdict is
 * not that kind of statement: a type read as a value object before a repository was found to
 * manage it becomes an aggregate, and the earlier reading has to disappear rather than pile up
 * next to the new one. Recomputing the whole set from the evidences of the round, instead of
 * adding to it, is what keeps a superseded reading from surviving in the model.</p>
 *
 * @since 7.0.0
 */
public final class Verdicts {

    private static final Verdicts NONE = new Verdicts(Map.of());

    private final SortedMap<TypeId, Classification> byType;

    private Verdicts(Map<TypeId, Classification> byType) {
        this.byType = new TreeMap<>(byType);
    }

    /**
     * Returns the empty set of verdicts, which is what the first round reads.
     *
     * @return no verdicts
     */
    public static Verdicts none() {
        return NONE;
    }

    /**
     * Collects verdicts by type.
     *
     * @param byType the classification of each type
     * @return the verdicts, in identity order
     */
    static Verdicts of(Map<TypeId, Classification> byType) {
        Objects.requireNonNull(byType, "byType must not be null");
        return byType.isEmpty() ? NONE : new Verdicts(byType);
    }

    /**
     * Returns the verdict on one type.
     *
     * @param id the type id
     * @return the classification, or empty when no verdict was reached on that type
     */
    public Optional<Classification> verdict(TypeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(byType.get(id));
    }

    /**
     * Returns the kind decided for one type, which is what a propagation rule asks for.
     *
     * @param id the type id
     * @return the kind, or empty when no verdict was reached on that type
     */
    public Optional<ArchKind> kindOf(TypeId id) {
        return verdict(id).map(Classification::kind);
    }

    /**
     * Returns the types a verdict was reached on, in identity order.
     *
     * @return the immutable list of subjects
     */
    public List<TypeId> subjects() {
        return List.copyOf(byType.keySet());
    }

    /**
     * Returns how many verdicts were reached.
     *
     * @return the verdict count
     */
    public int size() {
        return byType.size();
    }

    /**
     * Returns the types the two sets do not agree on, subjects known to only one of them
     * included.
     *
     * @param other the verdicts to compare against
     * @return the disagreeing subjects, in identity order
     */
    public List<TypeId> differencesWith(Verdicts other) {
        Objects.requireNonNull(other, "other must not be null");
        SortedMap<TypeId, Classification> merged = new TreeMap<>(byType);
        merged.putAll(other.byType);
        return merged.keySet().stream()
                .filter(id -> !Objects.equals(byType.get(id), other.byType.get(id)))
                .toList();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Verdicts verdicts && byType.equals(verdicts.byType);
    }

    @Override
    public int hashCode() {
        return byType.hashCode();
    }
}
