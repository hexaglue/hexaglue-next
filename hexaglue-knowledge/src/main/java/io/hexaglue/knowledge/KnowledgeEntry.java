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

import io.hexaglue.model.ArchKind;
import java.util.Objects;
import java.util.Optional;

/**
 * One statement of a pack: this symbol carries this fact.
 *
 * <p>Construction refuses the statements that cannot mean anything — an intent without the kind it
 * declares, a kind on a fact that carries none, a fact capturing type arguments bound to a symbol
 * that has none.</p>
 *
 * @param selector how the symbol is recognized
 * @param fact what the symbol carries
 * @param declaredKind the kind the author declares, present exactly for the declared-intent fact
 * @since 7.0.0
 */
public record KnowledgeEntry(Selector selector, KnowledgeFact fact, Optional<ArchKind> declaredKind) {

    /**
     * Validates that the fact, the selector and the declared kind agree.
     */
    public KnowledgeEntry {
        Objects.requireNonNull(selector, "selector must not be null");
        Objects.requireNonNull(fact, "fact must not be null");
        Objects.requireNonNull(declaredKind, "declaredKind must not be null");
        if (fact.carriesDeclaredKind() != declaredKind.isPresent()) {
            throw new IllegalArgumentException(
                    fact.carriesDeclaredKind()
                            ? fact + " must name the kind it declares"
                            : fact + " carries no kind, but " + declaredKind.orElseThrow() + " was named");
        }
        if (declaredKind.filter(kind -> kind == ArchKind.UNCLASSIFIED).isPresent()) {
            throw new IllegalArgumentException(
                    "UNCLASSIFIED is the absence of a verdict, not an intent an author can declare");
        }
        if (fact.capturesTypeArguments() && !(selector instanceof Selector.Supertype)) {
            throw new IllegalArgumentException(
                    fact + " captures " + fact.captureNames() + ", which only a supertype reference carries");
        }
    }

    /**
     * Creates an entry for a fact that carries no kind.
     *
     * @param selector how the symbol is recognized
     * @param fact what the symbol carries
     * @return a new entry
     */
    public static KnowledgeEntry of(Selector selector, KnowledgeFact fact) {
        return new KnowledgeEntry(selector, fact, Optional.empty());
    }

    /**
     * Creates an entry declaring the kind an author intends.
     *
     * @param selector how the intent marker is recognized
     * @param kind the declared kind
     * @return a new entry emitting {@link KnowledgeFact#DECLARED_KIND}
     */
    public static KnowledgeEntry declaring(Selector selector, ArchKind kind) {
        return new KnowledgeEntry(selector, KnowledgeFact.DECLARED_KIND, Optional.of(kind));
    }
}
