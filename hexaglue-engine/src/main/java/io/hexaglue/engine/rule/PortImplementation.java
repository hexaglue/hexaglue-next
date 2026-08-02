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
import io.hexaglue.engine.Rule;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.TypeNode;
import java.util.List;
import java.util.Set;

/**
 * Reads what fulfils an established way out as the driven adapter it is.
 *
 * <p>This is the reading a codebase already hexagonal depends on. Its adapters carry no framework
 * symbol worth recognizing — no {@code EntityManager}, no vendor template, sometimes nothing but
 * plain Java — so the outer ring cannot be read from what they hold. What places them is the port
 * they answer: once the boundary is known, the far side of it is the ring, and the type standing
 * there needs no marker of its own.</p>
 *
 * <p>The reading runs the wave backwards, from the boundary to the ring, which is why it only ever
 * fires on a later round than the one that established the port.</p>
 *
 * <p>A type the core already claims is left where it is. A domain type fulfilling a way out is a
 * frontier that is not one, and the audit has a finding for exactly that; moving it to the ring
 * would replace the anomaly with a tidy verdict and lose the very thing worth reporting.</p>
 *
 * @since 7.0.0
 */
public final class PortImplementation implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("W1-DR2");

    PortImplementation() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE);
    }

    @Override
    public void apply(Derivation derivation) {
        for (TypeNode type : derivation.perimeter().types()) {
            if (derivation
                    .kindOf(type.id())
                    .filter(ArchKind.DRIVEN_PORT::equals)
                    .isPresent()) {
                read(derivation, type.id());
            }
        }
    }

    private void read(Derivation derivation, TypeId port) {
        for (TypeNode implementer : Contracts.implementersOf(derivation, port)) {
            if (Contracts.claimedByTheCore(derivation, implementer.id())) {
                continue;
            }
            Contracts.speak(
                    derivation,
                    implementer.id(),
                    ArchKind.DRIVEN_ADAPTER,
                    "IMPLEMENTS_DRIVEN_PORT(" + port.qualifiedName() + ")",
                    "it answers " + port.simpleName() + ", which the hexagon calls to reach outside",
                    List.of(port),
                    ID);
        }
    }
}
