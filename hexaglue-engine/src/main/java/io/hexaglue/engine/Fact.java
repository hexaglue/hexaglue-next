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
import io.hexaglue.model.classification.ProofNode;

/**
 * Something the engine holds true about one type.
 *
 * <p>A fact is identified by its {@link #render() rendering} and by nothing else: two rules
 * reaching the same conclusion state the same fact, and the base holds it once. The proof is how
 * the fact was reached, not what it is — which is why it takes no part in the identity, and why
 * the route that arrived first is the one the base keeps.</p>
 *
 * @since 7.0.0
 */
public sealed interface Fact permits KnowledgeAssertion, KindEvidence, Relation {

    /**
     * Returns the predicate this fact belongs to. Rules declare the predicates they read and
     * write, and the saturation loop schedules them on that declaration.
     *
     * @return the predicate
     */
    Predicate predicate();

    /**
     * Returns the type this fact is about, which is the key the base indexes it under. A fact
     * relating two types is stated about the first one.
     *
     * @return the subject type id
     */
    TypeId subject();

    /**
     * Returns the canonical rendering of this fact — its identity, and the line the proof tree
     * shows.
     *
     * @return the rendering, stable across runs
     */
    String render();

    /**
     * Returns how this fact was reached: a leaf for something observed, a derivation for
     * something a rule concluded from premises.
     *
     * @return the proof
     */
    ProofNode proof();
}
