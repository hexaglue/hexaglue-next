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

package io.hexaglue.engine.rule;

import io.hexaglue.engine.Derivation;
import io.hexaglue.engine.KindEvidence;
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Rule;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.ClassificationConfig;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reads what a type is called, against the vocabulary the user configured.
 *
 * <p>Naming is the weakest tier there is, and the only one that carries no evidence of anything
 * beyond habit. It is here because habits are informative — {@code OrderId} really is an
 * identifier, most of the time — and because leaving it out would mean either ignoring the signal
 * or, worse, smuggling it into a stronger rule. What it may never do is overturn a fact: the
 * weighing gives a whole tier of naming less weight than one framework or structural signal, so a
 * name can settle a tie and nothing else.</p>
 *
 * <p>This is the one place in the engine where a name is matched. The vocabulary itself lives in
 * the configuration, so a code base with its own conventions is understood by stating them rather
 * than by patching the tool — and a convention the user removes really does stop applying.</p>
 *
 * <p>Only the longest suffix a kind offers is read, so a type called
 * {@code OrderApplicationService} weighs the same as one called {@code OrderService}: a name is
 * one signal, however many ways the vocabulary can spell it.</p>
 *
 * @since 7.0.0
 */
public final class ConventionalName implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("NAMING");

    ConventionalName() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads what a type is called, against the vocabulary the user configured";
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE);
    }

    @Override
    public void apply(Derivation derivation) {
        ClassificationConfig vocabulary = derivation.config().classification();
        for (TypeNode type : derivation.perimeter().types()) {
            for (ArchKind kind : vocabulary.namingSuffixes().keySet()) {
                longestMatch(type.id().simpleName(), vocabulary.suffixesFor(kind))
                        .ifPresent(suffix -> speak(derivation, type, kind, suffix));
            }
        }
    }

    /**
     * Returns the longest suffix of the vocabulary the name carries.
     *
     * <p>The comparison is case sensitive and the name must be longer than the suffix: a type
     * called exactly {@code Id} is the convention itself rather than a type following it, and
     * {@code Grid} does not end with {@code Id}.</p>
     */
    private static Optional<String> longestMatch(String simpleName, List<String> suffixes) {
        return suffixes.stream()
                .filter(suffix -> simpleName.length() > suffix.length() && simpleName.endsWith(suffix))
                .max(Comparator.comparingInt(String::length));
    }

    private static void speak(Derivation derivation, TypeNode type, ArchKind kind, String suffix) {
        Evidence evidence = new Evidence(
                EvidenceTier.NAMING,
                EvidenceTier.NAMING.maxConfidence(),
                "NAME_SUFFIX(" + suffix + ")",
                type.id().simpleName() + " ends in " + suffix + ", which this code base uses for a " + kind,
                type.sourceLocation(),
                List.of());
        derivation.derive(KindEvidence.derived(type.id(), kind, evidence, 0, ID));
    }
}
