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
import io.hexaglue.engine.RelationKind;
import io.hexaglue.engine.Rule;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Set;

/**
 * States which aggregate a way in is about, when its use cases are about one.
 *
 * <p>A driving port is the outside asking something of the hexagon, and what it asks about is
 * almost always a single thing with a life of its own — placing an order, cancelling it, reading it
 * back. Naming that thing is what lets a backend give the port a resource to hang its operations
 * on; without it, everything a generator writes for the port has to be named after the port.</p>
 *
 * <p>The reading is <em>the aggregates its signatures name</em>, either side. It deliberately does
 * <strong>not</strong> mirror the convergence a way out is read by: a way out receives the
 * aggregate in order to keep it, so it both takes and answers with it, while a way in receives an
 * identity or plain values and answers with the aggregate. Requiring both sides here was measured
 * against real ports and found every one of them wanting.</p>
 *
 * <p>Exactly one, or nothing. A port speaking of two aggregates is not speaking of either, and
 * electing one of them would be a guess the sources never made — the backend that needs a subject
 * can say it found none, which is a sentence an author can act on.</p>
 *
 * @since 7.0.0
 */
public final class ExposedAggregate implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R9");

    ExposedAggregate() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "states which aggregate a way in is about, when its use cases are about one";
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.RELATION);
    }

    @Override
    public void apply(Derivation derivation) {
        for (TypeNode port : derivation.perimeter().types()) {
            if (derivation
                    .kindOf(port.id())
                    .filter(ArchKind.DRIVING_PORT::equals)
                    .isPresent()) {
                read(derivation, port);
            }
        }
    }

    private void read(Derivation derivation, TypeNode port) {
        List<TypeId> aggregates = Signatures.mentioned(derivation, port).stream()
                .distinct()
                .filter(named -> derivation
                        .kindOf(named)
                        .filter(ArchKind.AGGREGATE_ROOT::equals)
                        .isPresent())
                .toList();
        if (aggregates.size() == 1) {
            derivation.derive(Relation.derived(port.id(), RelationKind.CONCERNS, aggregates.get(0), ID));
        }
    }
}
