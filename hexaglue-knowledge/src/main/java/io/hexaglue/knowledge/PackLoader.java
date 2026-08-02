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
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

/**
 * Reads a pack document into a {@link KnowledgePack}, strictly.
 *
 * <p>Strictly means: an unknown key, a missing key, a shape that is not the expected one, a symbol
 * that is not named in full, a fact nobody consumes — each of them fails the read with a coded
 * diagnostic. A pack is knowledge stated as data, and data that is quietly ignored becomes an
 * engine that has silently grown weaker.</p>
 *
 * <p>A pack document is a mapping of three keys:</p>
 *
 * <pre>{@code
 * pack: spring-data
 * description: What Spring Data's interfaces say about a type.
 * entries:
 *   - supertype: org.springframework.data.repository.Repository
 *     emits: SPRING_DATA_REPOSITORY
 *   - annotation: org.jmolecules.ddd.annotation.AggregateRoot
 *     emits: DECLARED_KIND
 *     kind: AGGREGATE_ROOT
 * }</pre>
 *
 * <p>An entry names its symbol with exactly one of {@code annotation}, {@code supertype},
 * {@code type} or {@code package-prefix}, the fact with {@code emits}, and — for the
 * declared-intent fact only — the declared kind with {@code kind}. The type arguments a fact
 * captures are not written down: they belong to the fact's meaning, so no pack can bind them to
 * the wrong position.</p>
 *
 * @since 7.0.0
 */
public final class PackLoader {

    /** The document is not a readable pack: malformed YAML, empty, or not a mapping. */
    private static final IssueCode DOCUMENT_UNREADABLE = IssueCode.of("HG-KNOWLEDGE-001");

    /** The pack's structure cannot be bound: unknown or missing key, unexpected shape, duplicate. */
    private static final IssueCode STRUCTURE_INVALID = IssueCode.of("HG-KNOWLEDGE-002");

    /** A symbol is not named in full, and could match a type the pack never meant. */
    private static final IssueCode SYMBOL_NOT_QUALIFIED = IssueCode.of("HG-KNOWLEDGE-003");

    /** The fact cannot be honoured as stated: unknown, or at odds with its kind or its selector. */
    private static final IssueCode FACT_NOT_HONOURED = IssueCode.of("HG-KNOWLEDGE-004");

    private static final String PACK = "pack";
    private static final String DESCRIPTION = "description";
    private static final String ENTRIES = "entries";
    private static final String EMITS = "emits";
    private static final String KIND = "kind";

    /** The selector keys and the shape each one builds, in the order diagnostics list them. */
    private static final List<SelectorShape> SELECTOR_SHAPES = List.of(
            new SelectorShape("annotation", Selector.Annotated::new),
            new SelectorShape("supertype", Selector.Supertype::new),
            new SelectorShape("type", Selector.Type::new),
            new SelectorShape("package-prefix", Selector.PackagePrefix::new));

    private static final List<String> SELECTOR_KEYS =
            SELECTOR_SHAPES.stream().map(SelectorShape::key).sorted().toList();

    private static final List<String> PACK_KEYS =
            Stream.of(PACK, DESCRIPTION, ENTRIES).sorted().toList();

    private static final List<String> ENTRY_KEYS = Stream.concat(SELECTOR_KEYS.stream(), Stream.of(EMITS, KIND))
            .sorted()
            .toList();

    private static final String KNOWN_FACTS =
            Arrays.stream(KnowledgeFact.values()).map(Enum::name).collect(Collectors.joining(", "));

    private PackLoader() {}

    /**
     * Reads a pack from its YAML text.
     *
     * @param origin where the document comes from, named in every diagnostic
     * @param yaml the document text
     * @return the pack it states
     * @throws KnowledgeException when the document is not a readable, bindable pack
     */
    public static KnowledgePack load(String origin, String yaml) {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(yaml, "yaml must not be null");
        return bind(origin, parse(origin, yaml));
    }

    /**
     * Reads a pack from a classpath resource.
     *
     * @param resourceName the resource path, e.g. {@code io/hexaglue/knowledge/packs/spring.yaml}
     * @return the pack it states
     * @throws KnowledgeException when the resource is absent, unreadable, or not a bindable pack
     */
    public static KnowledgePack loadResource(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName must not be null");
        // Read through the class rather than a class loader: a pack travels with the module that
        // ships it, wherever that module ends up being loaded from.
        try (InputStream resource = PackLoader.class.getResourceAsStream("/" + resourceName)) {
            if (resource == null) {
                throw failure(DOCUMENT_UNREADABLE, resourceName, "is not on the classpath");
            }
            return load(resourceName, new String(resource.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw failure(DOCUMENT_UNREADABLE, resourceName, "cannot be read: " + unreadable.getMessage(), unreadable);
        }
    }

    private static Object parse(String origin, String yaml) {
        LoadSettings settings = LoadSettings.builder()
                .setLabel(origin)
                .setAllowDuplicateKeys(false)
                .build();
        Object document;
        try {
            document = new Load(settings).loadFromString(yaml);
        } catch (YamlEngineException malformed) {
            throw failure(DOCUMENT_UNREADABLE, origin, "is not readable YAML: " + malformed.getMessage(), malformed);
        }
        if (document == null) {
            throw failure(DOCUMENT_UNREADABLE, origin, "states nothing");
        }
        return document;
    }

    private static KnowledgePack bind(String origin, Object document) {
        Map<String, Object> root = mapping(document)
                .orElseThrow(
                        () -> failure(DOCUMENT_UNREADABLE, origin, "is not a pack: the document is not a mapping"));
        rejectUnknownKeys(origin, "the pack", root, PACK_KEYS);

        String id = string(origin, root, PACK);
        String description = string(origin, root, DESCRIPTION);
        List<Object> declared = Optional.ofNullable(root.get(ENTRIES))
                .flatMap(PackLoader::list)
                .orElseThrow(() -> failure(origin, "must list its entries under '" + ENTRIES + "'"));

        List<KnowledgeEntry> entries = new ArrayList<>();
        for (int index = 0; index < declared.size(); index++) {
            entries.add(entry(origin + " entry " + (index + 1), declared.get(index)));
        }
        try {
            return new KnowledgePack(id, description, entries);
        } catch (IllegalArgumentException refused) {
            throw failure(origin, reason(refused), refused);
        }
    }

    private static KnowledgeEntry entry(String origin, Object declared) {
        Map<String, Object> fields = mapping(declared).orElseThrow(() -> failure(origin, "is not a mapping"));
        rejectUnknownKeys(origin, "the entry", fields, ENTRY_KEYS);

        Selector selector = selector(origin, fields);
        KnowledgeFact fact = fact(origin, string(origin, fields, EMITS));
        Optional<ArchKind> declaredKind = kind(origin, fields);
        try {
            return new KnowledgeEntry(selector, fact, declaredKind);
        } catch (IllegalArgumentException refused) {
            throw failure(FACT_NOT_HONOURED, origin, reason(refused), refused);
        }
    }

    private static Selector selector(String origin, Map<String, Object> fields) {
        List<SelectorShape> present = SELECTOR_SHAPES.stream()
                .filter(shape -> fields.containsKey(shape.key()))
                .toList();
        if (present.size() != 1) {
            throw failure(
                    origin,
                    "must name its symbol with exactly one of " + SELECTOR_KEYS + ", but had "
                            + present.stream().map(SelectorShape::key).sorted().toList());
        }
        SelectorShape shape = present.get(0);
        String symbol = string(origin, fields, shape.key());
        try {
            return shape.build().apply(symbol);
        } catch (IllegalArgumentException refused) {
            throw failure(SYMBOL_NOT_QUALIFIED, origin, reason(refused), refused);
        }
    }

    private static KnowledgeFact fact(String origin, String name) {
        return Arrays.stream(KnowledgeFact.values())
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow(() -> failure(
                        FACT_NOT_HONOURED,
                        origin,
                        "emits '" + name + "', which is not a fact; known facts are " + KNOWN_FACTS));
    }

    private static Optional<ArchKind> kind(String origin, Map<String, Object> fields) {
        if (!fields.containsKey(KIND)) {
            return Optional.empty();
        }
        String name = string(origin, fields, KIND);
        return Optional.of(Arrays.stream(ArchKind.values())
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow(() -> failure(FACT_NOT_HONOURED, origin, "declares '" + name + "', which is not a kind")));
    }

    private static void rejectUnknownKeys(
            String origin, String subject, Map<String, Object> fields, List<String> known) {
        List<String> unknown = fields.keySet().stream()
                .filter(key -> !known.contains(key))
                .sorted()
                .toList();
        if (!unknown.isEmpty()) {
            throw failure(origin, "gives " + subject + " keys nobody reads: " + unknown + "; known keys are " + known);
        }
    }

    private static String string(String origin, Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value instanceof String text) {
            return text;
        }
        if (value == null) {
            throw failure(origin, "has no '" + key + "'");
        }
        throw failure(
                origin,
                "must give '" + key + "' as text, but gave " + value.getClass().getSimpleName());
    }

    /**
     * Reads a YAML mapping into a string-keyed map. A document that is not a mapping, or that keys
     * on something other than text, answers empty: what that means is for the caller to word.
     */
    private static Optional<Map<String, Object>> mapping(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        for (Map.Entry<?, ?> field : map.entrySet()) {
            if (!(field.getKey() instanceof String key)) {
                return Optional.empty();
            }
            fields.put(key, field.getValue());
        }
        return Optional.of(fields);
    }

    private static Optional<List<Object>> list(Object value) {
        return value instanceof List<?> items ? Optional.of(List.copyOf(items)) : Optional.empty();
    }

    /** The wording of an invariant the shapes defend themselves, reused rather than restated here. */
    private static String reason(IllegalArgumentException refused) {
        return Optional.ofNullable(refused.getMessage()).orElseGet(refused::toString);
    }

    private static KnowledgeException failure(String origin, String message) {
        return failure(STRUCTURE_INVALID, origin, message);
    }

    private static KnowledgeException failure(String origin, String message, Throwable cause) {
        return failure(STRUCTURE_INVALID, origin, message, cause);
    }

    private static KnowledgeException failure(IssueCode code, String origin, String message) {
        return new KnowledgeException(diagnostic(code, origin, message));
    }

    private static KnowledgeException failure(IssueCode code, String origin, String message, Throwable cause) {
        return new KnowledgeException(diagnostic(code, origin, message), cause);
    }

    private static Diagnostic diagnostic(IssueCode code, String origin, String message) {
        return Diagnostic.builder(code, DiagnosticSeverity.ERROR, origin + " " + message)
                .build();
    }

    /** One way of naming a symbol, and how to build the selector that matches it. */
    private record SelectorShape(String key, Function<String, Selector> build) {}
}
