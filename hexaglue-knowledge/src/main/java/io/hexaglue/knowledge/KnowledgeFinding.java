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
import io.hexaglue.model.TypeRef;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * What a pack recognized on one type: the statement that matched, and the type arguments the
 * declaration named.
 *
 * <p>The finding keeps the entry rather than a copy of its parts, so an explanation can say which
 * pack claimed what, on which symbol — the provenance of a fact is as much a deliverable as the
 * fact itself.</p>
 *
 * @param packId the pack that states this
 * @param entry the statement that matched
 * @param captures the type arguments the fact names, by capture name, in name order
 * @since 7.0.0
 */
public record KnowledgeFinding(String packId, KnowledgeEntry entry, Map<String, TypeRef> captures) {

    /**
     * Validates the attribution and the captures, and copies the captures into a name-ordered map.
     */
    public KnowledgeFinding {
        Objects.requireNonNull(packId, "packId must not be null");
        Objects.requireNonNull(entry, "entry must not be null");
        Objects.requireNonNull(captures, "captures must not be null");
        if (packId.isBlank()) {
            throw new IllegalArgumentException("a finding must name the pack that states it");
        }
        for (String name : captures.keySet()) {
            if (!entry.fact().captureNames().contains(name)) {
                throw new IllegalArgumentException(entry.fact() + " names no capture '" + name + "'");
            }
        }
        captures = Collections.unmodifiableSortedMap(new TreeMap<>(captures));
    }

    /**
     * Returns the fact the matched entry states.
     *
     * @return the fact
     */
    public KnowledgeFact fact() {
        return entry.fact();
    }

    /**
     * Returns the kind the author declared, for a declared-intent fact.
     *
     * @return the declared kind, empty for every other fact
     */
    public Optional<ArchKind> declaredKind() {
        return entry.declaredKind();
    }

    /**
     * Returns the framework symbol that matched.
     *
     * @return the qualified name, or the package prefix
     */
    public String symbol() {
        return entry.selector().symbol();
    }

    /**
     * Returns the type argument bound to a capture name.
     *
     * @param name the capture name, as the fact names it
     * @return the captured reference, empty when the declaration named none
     */
    public Optional<TypeRef> capture(String name) {
        return Optional.ofNullable(captures.get(name));
    }
}
