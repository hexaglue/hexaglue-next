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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Everything HexaGlue knows about one framework, in the order it was written down.
 *
 * <p>A pack is closed over a single framework so that a user can read what is claimed about it,
 * disagree, and ship a pack of their own for their in-house conventions. Saying the same thing
 * twice is refused: duplicated knowledge is knowledge that will diverge.</p>
 *
 * @param id the pack identity, a slug that can also name a resource, e.g. {@code spring-data}
 * @param description what this pack is about, in one sentence
 * @param entries the statements, in declaration order
 * @since 7.0.0
 */
public record KnowledgePack(String id, String description, List<KnowledgeEntry> entries) {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    /**
     * Validates the identity, the description and the absence of duplicates, and copies the
     * entries.
     */
    public KnowledgePack {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        if (!SLUG.matcher(id).matches()) {
            throw new IllegalArgumentException("pack id must be a lowercase slug, but was: '" + id + "'");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("pack " + id + " must say what it is about");
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("pack " + id + " states nothing");
        }
        entries = List.copyOf(entries);
        rejectDuplicates(id, entries);
    }

    /**
     * Rejects a statement made twice. The selector itself is the key, not its symbol alone: the
     * same name can legitimately be known both as an annotation and as a type.
     */
    private static void rejectDuplicates(String id, List<KnowledgeEntry> entries) {
        Set<String> seen = new LinkedHashSet<>();
        for (KnowledgeEntry entry : entries) {
            if (!seen.add(entry.fact() + "@" + entry.selector())) {
                throw new IllegalArgumentException("pack " + id + " states twice: " + entry.fact() + " on "
                        + entry.selector().symbol());
            }
        }
    }
}
