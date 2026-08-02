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

import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.classification.ProofNode;
import io.hexaglue.model.classification.RuleId;
import java.util.Objects;

/**
 * What kind of way out a driven port is: storage, publication, or something else entirely.
 *
 * <p>The direction of a port is one question and its trade is another. Knowing that an interface is
 * how the hexagon reaches outward says nothing about whether a generator should write persistence
 * behind it or a message broker, and a consumer left to guess would guess from the name — which is
 * precisely what the engine exists to stop doing. So the trade is stated as a fact of its own, read
 * from the shape of the signatures, and travels with the verdict.</p>
 *
 * @param subject the driven port this role belongs to
 * @param role the functional family the signatures read as
 * @param proof how the role was reached
 * @since 7.0.0
 */
public record PortRole(TypeId subject, DrivenPortType role, ProofNode proof) implements Fact {

    /**
     * Validates that every component is present.
     */
    public PortRole {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(proof, "proof must not be null");
    }

    /**
     * Creates a role a rule concluded, with the proof of how it did.
     *
     * @param subject the driven port
     * @param role the functional family read for it
     * @param rule the rule concluding it
     * @param premises the facts the rule read to conclude
     * @return a new PortRole
     */
    public static PortRole derived(TypeId subject, DrivenPortType role, RuleId rule, ProofNode... premises) {
        return new PortRole(subject, role, ProofNode.derived(rule, render(subject, role), premises));
    }

    @Override
    public Predicate predicate() {
        return Predicate.PORT_ROLE;
    }

    @Override
    public String render() {
        return render(subject, role);
    }

    private static String render(TypeId subject, DrivenPortType role) {
        return "PORT_ROLE(" + subject.qualifiedName() + ") = " + role;
    }
}
