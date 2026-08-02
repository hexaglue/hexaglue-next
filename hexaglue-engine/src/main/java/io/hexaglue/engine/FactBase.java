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

import io.hexaglue.model.TypeId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Everything the engine holds true, indexed by predicate and by subject.
 *
 * <p>The base only ever grows, and it holds each fact once: that is what makes the saturation
 * loop terminate, and what lets two rules reach the same conclusion without the conclusion being
 * counted twice when the decision weighs it.</p>
 *
 * <p>Nothing here is ordered by insertion. Facts come back sorted by subject and then by their
 * rendering, so the order a rule happened to run in never reaches the output.</p>
 *
 * @since 7.0.0
 */
public final class FactBase {

    private final Map<Predicate, NavigableMap<TypeId, NavigableMap<String, Fact>>> facts =
            new EnumMap<>(Predicate.class);

    private int size;

    FactBase() {
        // Empty until a rule derives something.
    }

    /**
     * Records a fact, unless the base already holds it.
     *
     * @param fact the fact to record
     * @return true when the fact was new, false when it was already held
     */
    public boolean add(Fact fact) {
        Objects.requireNonNull(fact, "fact must not be null");
        NavigableMap<String, Fact> known = facts.computeIfAbsent(fact.predicate(), predicate -> new TreeMap<>())
                .computeIfAbsent(fact.subject(), subject -> new TreeMap<>());
        boolean added = known.putIfAbsent(fact.render(), fact) == null;
        if (added) {
            size++;
        }
        return added;
    }

    /**
     * Returns every fact of the given shape, in subject then rendering order.
     *
     * @param <F> the fact shape
     * @param factType the fact shape to select
     * @return the immutable list of facts, possibly empty
     */
    public <F extends Fact> List<F> all(Class<F> factType) {
        List<F> selected = new ArrayList<>();
        for (NavigableMap<String, Fact> bySubject : bySubject(factType).values()) {
            bySubject.values().forEach(fact -> selected.add(factType.cast(fact)));
        }
        return List.copyOf(selected);
    }

    /**
     * Returns what is known about one subject, in rendering order.
     *
     * @param <F> the fact shape
     * @param subject the type to look up
     * @param factType the fact shape to select
     * @return the immutable list of facts, possibly empty
     */
    public <F extends Fact> List<F> about(TypeId subject, Class<F> factType) {
        Objects.requireNonNull(subject, "subject must not be null");
        NavigableMap<String, Fact> known = bySubject(factType).get(subject);
        if (known == null) {
            return List.of();
        }
        return known.values().stream().map(factType::cast).toList();
    }

    /**
     * Returns how many facts the base holds.
     *
     * @return the fact count
     */
    public int size() {
        return size;
    }

    private NavigableMap<TypeId, NavigableMap<String, Fact>> bySubject(Class<? extends Fact> factType) {
        Objects.requireNonNull(factType, "factType must not be null");
        return facts.getOrDefault(Predicate.of(factType), new TreeMap<>());
    }
}
