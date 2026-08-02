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

package io.hexaglue.knowledge;

import io.hexaglue.model.classification.EvidenceTier;
import java.util.List;
import java.util.Objects;

/**
 * What a pack can state about a symbol. Each fact is technical — it says what a framework does,
 * never what a type <em>is</em>: turning a fact into a verdict is the engine's work, and some
 * facts are deliberately never allowed to reach one.
 *
 * <p>A fact carries its own tier, because that is a property of the kind of knowledge and not of
 * the rule that reads it: an author declaring intent outranks a framework symbol, always and
 * everywhere.</p>
 *
 * @since 7.0.0
 */
public enum KnowledgeFact {

    /**
     * The author declared what the type is — a jMolecules annotation or interface, an in-house
     * marker a user pack names. The entry carries the declared kind.
     */
    DECLARED_KIND(EvidenceTier.DECLARED_INTENT, List.of()),

    /**
     * The type is a Spring Data repository. Its type arguments name the aggregate it manages and
     * the identifier of that aggregate — the single most decisive signal enterprise code offers.
     */
    SPRING_DATA_REPOSITORY(EvidenceTier.FRAMEWORK_KNOWLEDGE, List.of("subject", "id")),

    /**
     * The type is mapped to storage. This is a fact about persistence and never an evidence of
     * kind, positive or negative: a domain type reads the same whether or not someone annotated
     * it. What it does produce is a finding about the coupling.
     */
    PERSISTENCE_MODEL(EvidenceTier.FRAMEWORK_KNOWLEDGE, List.of()),

    /** The type is an entry point a framework calls from outside: a controller, a listener. */
    DRIVING_ENTRYPOINT(EvidenceTier.FRAMEWORK_KNOWLEDGE, List.of()),

    /** The type wears an application stereotype. It supports a reading, never decides one. */
    APPLICATION_STEREOTYPE(EvidenceTier.FRAMEWORK_KNOWLEDGE, List.of()),

    /** The type is framework plumbing: configuration, bootstrap, properties binding. */
    TECHNICAL(EvidenceTier.FRAMEWORK_KNOWLEDGE, List.of()),

    /**
     * The symbol is no pollution of a domain. Logging and validation are not infrastructure
     * leaking into the hexagon, and a purity report that says otherwise cries wolf.
     */
    NEUTRAL(EvidenceTier.FRAMEWORK_KNOWLEDGE, List.of()),

    /**
     * The symbol is an infrastructure tool. A type holding one reaches outside the hexagon,
     * whatever it calls itself.
     */
    INFRA_DEPENDENCY(EvidenceTier.FRAMEWORK_KNOWLEDGE, List.of()),

    /** The type was produced by a generator, and is nobody's architectural intent. */
    GENERATED_CODE(EvidenceTier.FRAMEWORK_KNOWLEDGE, List.of());

    private final EvidenceTier tier;

    // List.copyOf yields an unmodifiable list, which the checker cannot tell from a mutable one.
    @SuppressWarnings("ImmutableEnumChecker")
    private final List<String> captureNames;

    KnowledgeFact(EvidenceTier tier, List<String> captureNames) {
        this.tier = Objects.requireNonNull(tier);
        this.captureNames = List.copyOf(captureNames);
    }

    /**
     * Returns the signal tier an evidence built on this fact belongs to.
     *
     * @return the evidence tier
     */
    public EvidenceTier tier() {
        return tier;
    }

    /**
     * Returns the names this fact gives to the type arguments it captures, positionally: the first
     * name binds the first type argument of the matched supertype reference, and so on.
     *
     * @return the immutable capture names, empty for a fact that captures nothing
     */
    public List<String> captureNames() {
        return captureNames;
    }

    /**
     * Returns whether this fact captures type arguments, which only a supertype can offer.
     *
     * @return true when the fact names at least one capture
     */
    public boolean capturesTypeArguments() {
        return !captureNames.isEmpty();
    }

    /**
     * Returns whether an entry emitting this fact must name the kind it declares.
     *
     * @return true for the declared-intent fact
     */
    public boolean carriesDeclaredKind() {
        return this == DECLARED_KIND;
    }
}
