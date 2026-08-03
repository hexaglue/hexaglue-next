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
 * Reads a part of an aggregate that carries no identity as the value it is.
 *
 * <p>Something an aggregate is made of, which cannot be told apart from another one holding the
 * same attributes, is a value — and it stays a value however it is written. A part with setters
 * everywhere is a value object the codebase implemented badly, not a different kind of thing:
 * identification says what the type is, conformity says what is wrong with it, and refusing to
 * identify it would leave the audit nothing to report. That is the same separation that lets an
 * aggregate annotated for persistence remain an aggregate.</p>
 *
 * @since 7.0.0
 */
public final class OwnedValue implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("R3b");

    OwnedValue() {
        // Stateless: everything a rule needs comes from the derivation it is handed.
    }

    @Override
    public RuleId id() {
        return ID;
    }

    @Override
    public String title() {
        return "reads a part of an aggregate that carries no identity as the value it is";
    }

    @Override
    public Set<Predicate> writes() {
        return Set.of(Predicate.EVIDENCE, Predicate.RELATION);
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
            if (!Lifecycle.carriesIdentity(derivation, part)) {
                Contracts.speak(
                        derivation,
                        part,
                        ArchKind.VALUE_OBJECT,
                        "OWNED_BY(" + owner.id().qualifiedName() + ")",
                        owner.id().simpleName() + " is made of it and it carries no identity of its own",
                        List.of(owner.id()),
                        ID);
                Lifecycle.tie(derivation, owner, part, ID);
            }
        }
    }
}
