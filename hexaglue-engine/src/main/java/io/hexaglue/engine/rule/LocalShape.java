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
import java.util.List;
import java.util.Set;

/**
 * Reads what the shape of a declaration says, on its own, about the kind of type it is.
 *
 * <p>Three shapes carry meaning. A declaration whose state cannot change is a value: a record, an
 * enum, a class whose every field is final. A declaration wrapping exactly one value is how an
 * identity is usually written. And a class holding no state at all says nothing — vacuous
 * immutability is not a signal, it is the absence of one.</p>
 *
 * <p>A wrapper is deliberately read as <em>both</em> an identifier and a value object, because
 * nothing structural separates {@code OrderId} from {@code Email}: they are the same declaration.
 * Emitting one and hiding the other would be a guess dressed as a fact. The two compete, the
 * verdict stays undecided with both readings kept, and a stronger signal settles it — the
 * repository that looks an aggregate up by it, or the naming vocabulary.</p>
 *
 * <p>Everything here is local structure (S4): strong enough to decide when nothing contradicts
 * it, never strong enough to overturn what a framework or an author declared.</p>
 *
 * @since 7.0.0
 */
public final class LocalShape implements Rule {

    /** The published identifier of this rule. */
    public static final RuleId ID = RuleId.of("SHAPE");

    LocalShape() {
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
            if (Shapes.isImmutable(type)) {
                speak(derivation, type, ArchKind.VALUE_OBJECT, "IMMUTABLE_SHAPE", reasonForImmutability(type));
            }
            // Only an immutable wrapper reads as an identity: something that can change is
            // neither a value nor the identity of anything.
            if (Shapes.readsAsIdentity(type)) {
                speak(
                        derivation,
                        type,
                        ArchKind.IDENTIFIER,
                        "SINGLE_VALUE_WRAPPER",
                        "it wraps exactly one value, "
                                + Shapes.state(type).get(0).type().toDisplayString()
                                + ", which is how an identity is written");
            }
        }
    }

    private static String reasonForImmutability(TypeNode type) {
        return switch (type.nature()) {
            case RECORD -> "it is a record, so its state cannot change";
            case ENUM -> "it is an enum, a closed set of values";
            default -> "every field it declares is final, so its state cannot change";
        };
    }

    private static void speak(Derivation derivation, TypeNode type, ArchKind kind, String fact, String why) {
        Evidence evidence = new Evidence(
                EvidenceTier.LOCAL_STRUCTURE,
                EvidenceTier.LOCAL_STRUCTURE.maxConfidence(),
                fact + "(" + type.id().qualifiedName() + ")",
                type.id().qualifiedName() + " reads as a " + kind + " because " + why,
                type.sourceLocation(),
                List.of());
        derivation.derive(KindEvidence.derived(type.id(), kind, evidence, 0, ID));
    }
}
