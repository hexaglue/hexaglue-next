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
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.model.classification.Basis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * How a whole run went: how many types each kind reached, what stayed out and why, and how much of
 * the result was declared rather than deduced.
 *
 * <p>This is what a host says once, at the end, when explaining one type at a time would say
 * nothing about the shape of the answer. It is also what the reliability section of a report
 * aggregates, which is why it counts rather than renders: the counts are the data, and turning
 * them into lines is {@link Explanation}'s job.</p>
 *
 * <p>Tallies list only what was actually reached — a kind nothing matched is absent rather than
 * present at zero, so the lines a host prints stay about the code that was read.</p>
 *
 * @param kinds how many types reached each kind, in the order the vocabulary declares them
 * @param unclassified how many types stayed out under each category, in category order
 * @param declared how many verdicts the sources stated outright
 * @param inferred how many verdicts the engine deduced
 * @param ambiguous how many decisions kept competing candidates
 * @since 7.0.0
 */
public record Outcome(
        List<Tally<ArchKind>> kinds,
        List<Tally<UnclassifiedCategory>> unclassified,
        int declared,
        int inferred,
        int ambiguous) {

    /**
     * Validates the counts and copies the tallies.
     */
    public Outcome {
        Objects.requireNonNull(kinds, "kinds must not be null");
        Objects.requireNonNull(unclassified, "unclassified must not be null");
        if (declared < 0 || inferred < 0 || ambiguous < 0) {
            throw new IllegalArgumentException("counts must be >= 0");
        }
        kinds = List.copyOf(kinds);
        unclassified = List.copyOf(unclassified);
    }

    /**
     * Counts a classified model.
     *
     * @param model the model to count
     * @return the outcome of the run that produced it
     */
    public static Outcome of(ArchModel model) {
        Objects.requireNonNull(model, "model must not be null");
        Map<ArchKind, Integer> byKind = new EnumMap<>(ArchKind.class);
        Map<UnclassifiedCategory, Integer> byCategory = new EnumMap<>(UnclassifiedCategory.class);
        int declared = 0;
        int ambiguous = 0;
        for (ArchType type : model.types()) {
            byKind.merge(type.kind(), 1, Integer::sum);
            if (type instanceof UnclassifiedType fallback) {
                byCategory.merge(fallback.category(), 1, Integer::sum);
            }
            if (type.classification().basis() == Basis.DECLARED) {
                declared++;
            }
            if (type.classification().isAmbiguous()) {
                ambiguous++;
            }
        }
        return new Outcome(
                tallies(byKind), tallies(byCategory), declared, model.types().size() - declared, ambiguous);
    }

    /**
     * Returns how many types the run classified, fallback included.
     *
     * @return the total number of types
     */
    public int types() {
        return declared + inferred;
    }

    private static <T> List<Tally<T>> tallies(Map<T, Integer> counts) {
        List<Tally<T>> tallies = new ArrayList<>();
        for (Map.Entry<T, Integer> counted : counts.entrySet()) {
            tallies.add(new Tally<>(counted.getKey(), counted.getValue()));
        }
        return tallies;
    }

    /**
     * How many types one value of a vocabulary accounts for.
     *
     * @param <T> the vocabulary being counted
     * @param subject the value counted
     * @param count how many types it accounts for, always at least one
     * @since 7.0.0
     */
    public record Tally<T>(T subject, int count) {

        /**
         * Validates that a tally exists because something was counted.
         */
        public Tally {
            Objects.requireNonNull(subject, "subject must not be null");
            if (count < 1) {
                throw new IllegalArgumentException("a tally is only recorded for what was reached, got " + count);
            }
        }
    }
}
