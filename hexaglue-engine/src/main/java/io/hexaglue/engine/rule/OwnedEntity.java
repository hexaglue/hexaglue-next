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
 * Reads a part of an aggregate that carries an identity of its own as the entity it is.
 *
 * <p>What separates an entity from a value inside an aggregate is not its size or its mutability
 * but whether it can be told apart from another one just like it. A part keeping something the
 * analysis already reads as an identity can be; a part without one is the sum of its attributes and
 * nothing more. The identity has to be established elsewhere — declared, or read from the key a way
 * out searches by — because a field that merely looks like an identity is the very duel this wave
 * refuses to settle by position.</p>
 *
 * <p>The reading runs down as far as the composition goes: an entity owns parts in its turn, and
 * the round after it is placed those parts are read the same way.</p>
 *
 * @since 7.0.0
 */
public final class OwnedEntity implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R3a");

    OwnedEntity() {
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
        for (TypeNode owner : derivation.perimeter().types()) {
            if (Lifecycle.owns(derivation, owner.id())) {
                read(derivation, owner);
            }
        }
    }

    private void read(Derivation derivation, TypeNode owner) {
        for (TypeId part : Lifecycle.partsOf(derivation, owner)) {
            if (Lifecycle.carriesIdentity(derivation, part)) {
                Contracts.speak(
                        derivation,
                        part,
                        ArchKind.ENTITY,
                        "OWNED_BY(" + owner.id().qualifiedName() + ")",
                        owner.id().simpleName() + " is made of it and it carries an identity of its own",
                        List.of(owner.id()),
                        ID);
            }
        }
    }
}
