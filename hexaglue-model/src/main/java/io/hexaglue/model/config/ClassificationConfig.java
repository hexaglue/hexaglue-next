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

package io.hexaglue.model.config;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * What the user states about the classification of their own types, out of the code.
 *
 * <p>This is one of the two ways to declare an intent — the other being an intent annotation on the
 * type itself. It carries the same weight: a declaration is the strongest evidence tier, so a type
 * named here is classified as stated whatever the heuristics would have concluded. It is also the
 * answer for a code base that cannot, or will not, take a dependency to annotate its domain.</p>
 *
 * <p>This record is shape only: reading the declarations belongs to the engine, binding them from
 * YAML to the loader.</p>
 *
 * @param explicit the declared kind per type, iterated in type identity order
 * @since 7.0.0
 */
public record ClassificationConfig(Map<TypeId, ArchKind> explicit) {

    /**
     * Validates the declarations and copies them into an identity-ordered view.
     */
    public ClassificationConfig {
        Objects.requireNonNull(explicit, "explicit must not be null");
        SortedMap<TypeId, ArchKind> ordered = new TreeMap<>();
        explicit.forEach((id, kind) -> {
            Objects.requireNonNull(id, "declared type must not be null");
            Objects.requireNonNull(kind, "declared kind must not be null");
            if (kind == ArchKind.UNCLASSIFIED) {
                throw new IllegalArgumentException(
                        id + " is declared as UNCLASSIFIED, which states no intent; exclude the type from the"
                                + " analysis scope instead");
            }
            ordered.put(id, kind);
        });
        explicit = Collections.unmodifiableSortedMap(ordered);
    }

    /**
     * Returns the configuration declaring nothing, where every verdict is inferred.
     *
     * @return the empty configuration
     */
    public static ClassificationConfig empty() {
        return new ClassificationConfig(Map.of());
    }

    /**
     * Returns the kind the user declared for the given type.
     *
     * @param id the type to look up
     * @return the declared kind, or empty when the user said nothing about this type
     */
    public Optional<ArchKind> declaredKind(TypeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(explicit.get(id));
    }
}
