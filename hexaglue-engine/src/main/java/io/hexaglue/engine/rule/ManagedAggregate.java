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
import io.hexaglue.engine.Predicate;
import io.hexaglue.engine.Relation;
import io.hexaglue.engine.Rule;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.classification.RuleId;
import java.util.List;
import java.util.Set;

/**
 * Reads the type a way out stores and retrieves as the aggregate it is.
 *
 * <p>An aggregate is not recognized by what it looks like — a class with fields looks like every
 * other class with fields — but by the lifecycle something else gives it. A way out whose whole
 * trade is keeping one type and handing it back is the statement that this type has a life of its
 * own outside a single call: it is loaded, changed and saved as a unit, which is exactly what an
 * aggregate root is.</p>
 *
 * <p>This is the structural twin of what a Spring Data declaration says in one line. There the
 * framework names the subject; here the signatures do. The reading is the same and so is the
 * relation it consumes, so a codebase that writes its own repositories is read as well as one that
 * inherits them — and neither needs the word {@code Repository} anywhere.</p>
 *
 * @since 7.0.0
 */
public final class ManagedAggregate implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R1b");

    ManagedAggregate() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads the type a way out stores and retrieves as the aggregate it is";
    }

    @Override
    public Set<Predicate> reads() {
        return Lifecycle.SOURCES;
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE);
    }

    @Override
    public void apply(Derivation derivation) {
        for (Relation tie : Lifecycle.storageTies(derivation)) {
            Contracts.speak(
                    derivation,
                    tie.object(),
                    ArchKind.AGGREGATE_ROOT,
                    "MANAGED_BY(" + tie.subject().qualifiedName() + ")",
                    tie.subject().simpleName() + " keeps it and hands it back, which is a lifecycle of its own",
                    List.of(tie.subject()),
                    ID);
        }
    }
}
