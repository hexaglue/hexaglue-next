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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * What the user states about the classification of their own types, out of the code: the kinds
 * they declare outright, and the naming conventions their code base follows.
 *
 * <p>A declaration is the strongest evidence tier, so a type named here is classified as stated
 * whatever the heuristics would have concluded. It is also the answer for a code base that
 * cannot, or will not, take a dependency to annotate its domain.</p>
 *
 * <p>The vocabulary sits here rather than in the engine because a naming convention is a property
 * of a code base, not of the tool: a team writing {@code OrderRef} instead of {@code OrderId} says
 * so, and is understood. It is also the single place a suffix may be written down — a rule
 * matching a name anywhere else would be an opinion nobody can configure away.</p>
 *
 * <p>This record is shape only: reading the declarations belongs to the engine, binding them from
 * a document belongs to the host.</p>
 *
 * @param explicit the kind the user states for a type, by type identity
 * @param namingSuffixes the suffixes that suggest a kind, by kind
 * @since 7.0.0
 */
public record ClassificationConfig(Map<TypeId, ArchKind> explicit, Map<ArchKind, List<String>> namingSuffixes) {

    /**
     * Validates the declarations and copies them into ordered views.
     */
    public ClassificationConfig {
        Objects.requireNonNull(explicit, "explicit must not be null");
        Objects.requireNonNull(namingSuffixes, "namingSuffixes must not be null");
        explicit = copyDeclarations(explicit);
        namingSuffixes = copySuffixes(namingSuffixes);
    }

    private static Map<TypeId, ArchKind> copyDeclarations(Map<TypeId, ArchKind> explicit) {
        SortedMap<TypeId, ArchKind> declared = new TreeMap<>();
        explicit.forEach((id, kind) -> {
            Objects.requireNonNull(id, "declared type must not be null");
            Objects.requireNonNull(kind, "declared kind must not be null");
            if (kind == ArchKind.UNCLASSIFIED) {
                throw new IllegalArgumentException(
                        id + " is declared as UNCLASSIFIED, which states no intent; exclude the type from the"
                                + " analysis scope instead");
            }
            declared.put(id, kind);
        });
        return Collections.unmodifiableSortedMap(declared);
    }

    private static Map<ArchKind, List<String>> copySuffixes(Map<ArchKind, List<String>> suffixes) {
        Map<ArchKind, List<String>> ordered = new EnumMap<>(ArchKind.class);
        suffixes.forEach((kind, forKind) -> {
            Objects.requireNonNull(kind, "kind must not be null");
            Objects.requireNonNull(forKind, "suffixes must not be null");
            if (kind == ArchKind.UNCLASSIFIED) {
                throw new IllegalArgumentException("no suffix suggests UNCLASSIFIED, which is the absence of a kind");
            }
            forKind.forEach(suffix -> {
                if (suffix == null || suffix.isBlank()) {
                    throw new IllegalArgumentException("a naming suffix must not be blank, got one for " + kind);
                }
            });
            ordered.put(kind, List.copyOf(forKind));
        });
        return Collections.unmodifiableMap(ordered);
    }

    /**
     * Returns the documented default posture: the user declares no kind, and their code base is
     * taken to follow the conventional vocabulary.
     *
     * @return the default configuration
     */
    public static ClassificationConfig defaults() {
        return new ClassificationConfig(Map.of(), defaultNamingSuffixes());
    }

    /**
     * Returns the vocabulary the engine reads when the user states none.
     *
     * <p>It is deliberately short. Every entry is a convention strong enough that a reader of the
     * code would draw the same conclusion, and nothing weaker is here: a default that guessed
     * would be the engine holding an opinion under cover of a convention. {@code Service} suggests
     * the application layer alone — a domain service wears the same suffix, and what tells the two
     * apart is what the type does, not what it is called.</p>
     *
     * @return the shipped naming vocabulary
     */
    public static Map<ArchKind, List<String>> defaultNamingSuffixes() {
        Map<ArchKind, List<String>> suffixes = new EnumMap<>(ArchKind.class);
        suffixes.put(ArchKind.IDENTIFIER, List.of("Id", "Identifier"));
        suffixes.put(ArchKind.DOMAIN_EVENT, List.of("Event"));
        suffixes.put(ArchKind.DRIVEN_PORT, List.of("Repository", "Gateway"));
        suffixes.put(ArchKind.DRIVING_PORT, List.of("UseCase"));
        suffixes.put(ArchKind.COMMAND_HANDLER, List.of("CommandHandler"));
        suffixes.put(ArchKind.QUERY_HANDLER, List.of("QueryHandler"));
        suffixes.put(ArchKind.APPLICATION_SERVICE, List.of("Service", "ApplicationService"));
        return Collections.unmodifiableMap(suffixes);
    }

    /**
     * Returns the configuration that states nothing at all, vocabulary included — the posture of
     * a run that must not read a single name.
     *
     * @return the silent configuration
     */
    public static ClassificationConfig silent() {
        return new ClassificationConfig(Map.of(), Map.of());
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

    /**
     * Returns the suffixes that suggest the given kind.
     *
     * @param kind the kind to look up
     * @return the suffixes, in declaration order, empty when no suffix suggests that kind
     */
    public List<String> suffixesFor(ArchKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        return namingSuffixes.getOrDefault(kind, List.of());
    }
}
