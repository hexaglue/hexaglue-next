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

import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Everything the packs know, asked one type at a time.
 *
 * <p>The answer is a list of findings and nothing else: no verdict, no ranking, no arbitration
 * between a persistence mapping and a declared intent. Weighing them is the engine's work, and
 * keeping that separation is what lets a pack be read, argued with and replaced without touching
 * the reasoning.</p>
 *
 * <p>Findings come out in pack order, then in the order each pack states its entries — the same
 * answer, in the same order, on every run.</p>
 *
 * @since 7.0.0
 */
public final class FrameworkKnowledge {

    private final List<KnowledgePack> packs;

    private FrameworkKnowledge(List<KnowledgePack> packs) {
        this.packs = List.copyOf(packs);
    }

    /**
     * Assembles the knowledge of several packs, in the order they are given.
     *
     * @param packs the packs, in the order their findings will be reported
     * @return the assembled knowledge
     * @throws IllegalArgumentException when no pack is given, or two packs share an identity
     */
    public static FrameworkKnowledge of(List<KnowledgePack> packs) {
        Objects.requireNonNull(packs, "packs must not be null");
        if (packs.isEmpty()) {
            throw new IllegalArgumentException("framework knowledge without a pack knows nothing");
        }
        Set<String> identities = new LinkedHashSet<>();
        for (KnowledgePack pack : packs) {
            if (!identities.add(pack.id())) {
                throw new IllegalArgumentException("two packs claim the identity: " + pack.id());
            }
        }
        return new FrameworkKnowledge(packs);
    }

    /**
     * Returns the packs, in the order they were assembled.
     *
     * @return the immutable pack list
     */
    public List<KnowledgePack> packs() {
        return packs;
    }

    /**
     * Returns everything the packs recognize on one type.
     *
     * @param model the code model the type belongs to, read for the supertype closure
     * @param type the type under examination
     * @return the findings, in pack then entry order, possibly empty
     */
    public List<KnowledgeFinding> factsFor(CodeModel model, TypeNode type) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(type, "type must not be null");
        List<KnowledgeFinding> findings = new ArrayList<>();
        for (KnowledgePack pack : packs) {
            for (KnowledgeEntry entry : pack.entries()) {
                if (entry.selector().matches(model, type)) {
                    findings.add(new KnowledgeFinding(pack.id(), entry, captures(entry, model, type)));
                }
            }
        }
        return List.copyOf(findings);
    }

    /**
     * Binds the type arguments the declaration named to the names the fact gives them. The cast is
     * safe by the entry's own invariant: only a supertype can carry a capturing fact.
     */
    private static Map<String, TypeRef> captures(KnowledgeEntry entry, CodeModel model, TypeNode type) {
        if (!entry.fact().capturesTypeArguments()) {
            return Map.of();
        }
        Selector.Supertype supertype = (Selector.Supertype) entry.selector();
        Optional<TypeRef> route = supertype.declaredRouteIn(model, type);
        if (route.isEmpty()) {
            return Map.of();
        }
        List<TypeRef> arguments = route.orElseThrow().typeArguments();
        List<String> names = entry.fact().captureNames();
        Map<String, TypeRef> captured = new TreeMap<>();
        for (int position = 0; position < Math.min(names.size(), arguments.size()); position++) {
            captured.put(names.get(position), arguments.get(position));
        }
        return captured;
    }
}
