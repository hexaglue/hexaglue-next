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

import java.util.Objects;

/**
 * The families of fact the engine derives. A rule names the predicates it reads and the ones it
 * writes, and the saturation loop re-runs a rule only when a predicate it reads has grown — so a
 * predicate is the unit of change the loop watches, and the declaration order below is the order
 * the base answers in.
 *
 * @since 7.0.0
 */
public enum Predicate {

    /** What the knowledge packs recognize on a type: a framework symbol and the fact it carries. */
    KNOWLEDGE(KnowledgeAssertion.class),

    /** A signal supporting one kind for one type, at one tier of the hierarchy. */
    EVIDENCE(KindEvidence.class),

    /** A tie between two types: which aggregate a port manages, what carries an identity. */
    RELATION(Relation.class),

    /** The trade a driven port plies: storage, publication, or a call to something else. */
    PORT_ROLE(PortRole.class);

    // Class is immutable; the checker only sees a field of a non-final type.
    @SuppressWarnings("ImmutableEnumChecker")
    private final Class<? extends Fact> factType;

    Predicate(Class<? extends Fact> factType) {
        this.factType = Objects.requireNonNull(factType);
    }

    /**
     * Returns the fact shape this predicate holds.
     *
     * @return the fact type
     */
    public Class<? extends Fact> factType() {
        return factType;
    }

    /**
     * Returns the predicate holding the given fact shape.
     *
     * @param factType the fact type to look up
     * @return the predicate
     * @throws IllegalArgumentException when no predicate holds that shape
     */
    static Predicate of(Class<? extends Fact> factType) {
        Objects.requireNonNull(factType, "factType must not be null");
        for (Predicate predicate : values()) {
            if (predicate.factType.equals(factType)) {
                return predicate;
            }
        }
        throw new IllegalArgumentException("no predicate holds facts of shape " + factType.getSimpleName());
    }
}
