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

package io.hexaglue.maven;

import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

/**
 * A YAML mapping read strictly: every access states what shape it expects, and a document that does
 * not have that shape fails with a coded diagnostic naming the file and the key.
 *
 * <p>It knows nothing of what the keys mean — that belongs to whoever binds them. Separating the
 * two is what keeps the meaning of a configuration readable in one place: the vocabulary of the
 * document on one side, the mechanics of reading it on the other.</p>
 */
final class YamlDocument {

    /** The document is not readable: malformed YAML, or not a mapping. */
    static final IssueCode DOCUMENT_UNREADABLE = IssueCode.of("HG-CONFIG-001");

    /** The document's structure cannot be bound: unknown key, or a value of the wrong shape. */
    static final IssueCode STRUCTURE_INVALID = IssueCode.of("HG-CONFIG-002");

    private final String origin;
    private final Map<String, Object> fields;

    private YamlDocument(String origin, Map<String, Object> fields) {
        this.origin = origin;
        this.fields = fields;
    }

    /**
     * Reads a document. A document that states nothing is an empty mapping, not a failure: a file
     * left blank configures nothing, which is exactly what having no file means.
     *
     * @param origin where the document comes from, named in every diagnostic
     * @param yaml the document text
     * @return the mapping it states
     */
    static YamlDocument parse(String origin, String yaml) {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(yaml, "yaml must not be null");
        LoadSettings settings = LoadSettings.builder()
                .setLabel(origin)
                .setAllowDuplicateKeys(false)
                .build();
        Object document;
        try {
            document = new Load(settings).loadFromString(yaml);
        } catch (YamlEngineException malformed) {
            throw failure(origin, DOCUMENT_UNREADABLE, "is not readable YAML: " + malformed.getMessage(), malformed);
        }
        if (document == null) {
            return new YamlDocument(origin, Map.of());
        }
        return new YamlDocument(
                origin,
                mapping(document)
                        .orElseThrow(() ->
                                failure(origin, DOCUMENT_UNREADABLE, "is not a configuration: it is not a mapping")));
    }

    /** Returns where this document comes from, as diagnostics name it. */
    String origin() {
        return origin;
    }

    /** Returns whether the document states nothing at all. */
    boolean isEmpty() {
        return fields.isEmpty();
    }

    /**
     * Returns the sub-mapping under a key, checked against the keys it may state. An absent key is
     * an empty mapping: what a document does not state, it leaves to the defaults.
     *
     * @param key the key holding the sub-mapping
     * @param known the keys that sub-mapping may state
     * @return the sub-mapping, empty when unstated
     */
    YamlDocument block(String key, List<String> known) {
        Object stated = fields.get(key);
        if (stated == null) {
            return new YamlDocument(origin, Map.of());
        }
        YamlDocument block = new YamlDocument(
                origin,
                mapping(stated)
                        .orElseThrow(() -> failure(
                                origin, STRUCTURE_INVALID, "must state '" + key + "' as a mapping of its settings")));
        block.rejectUnknownKeys("'" + key + "'", known);
        return block;
    }

    /**
     * Fails when the mapping states a key nobody reads.
     *
     * @param subject how to name this mapping in the diagnostic
     * @param known the keys that are read
     */
    void rejectUnknownKeys(String subject, List<String> known) {
        List<String> unknown = fields.keySet().stream()
                .filter(key -> !known.contains(key))
                .sorted()
                .toList();
        if (!unknown.isEmpty()) {
            throw failure(
                    origin,
                    STRUCTURE_INVALID,
                    "gives " + subject + " keys nobody reads: " + unknown + "; known keys are " + known);
        }
    }

    /**
     * Returns the text under a key.
     *
     * @param key the key to read
     * @return the text, empty when the key is unstated
     */
    Optional<String> text(String key) {
        return Optional.ofNullable(fields.get(key)).map(value -> scalar(key, value));
    }

    /**
     * Returns the list of texts under a key.
     *
     * @param key the key to read
     * @return the texts, empty when the key is unstated
     */
    List<String> texts(String key) {
        Object stated = fields.get(key);
        if (stated == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        list(stated)
                .orElseThrow(() -> failure(origin, STRUCTURE_INVALID, "must state '" + key + "' as a list"))
                .forEach(item -> values.add(scalar(key, item)));
        return values;
    }

    /**
     * Returns the flag under a key.
     *
     * @param key the key to read
     * @param fallback what an unstated key means
     * @return the flag
     */
    boolean flag(String key, boolean fallback) {
        Object stated = fields.get(key);
        if (stated == null) {
            return fallback;
        }
        if (stated instanceof Boolean flag) {
            return flag;
        }
        throw failure(origin, STRUCTURE_INVALID, "must state '" + key + "' as true or false, but stated " + stated);
    }

    /**
     * Returns the entries of a mapping of text to text under a key.
     *
     * @param key the key holding the mapping
     * @return the entries in document order, empty when the key is unstated
     */
    Map<String, String> entries(String key) {
        Object stated = fields.get(key);
        if (stated == null) {
            return Map.of();
        }
        Map<String, String> entries = new LinkedHashMap<>();
        mapping(stated)
                .orElseThrow(() -> failure(origin, STRUCTURE_INVALID, "must state '" + key + "' as a mapping"))
                .forEach((name, value) -> entries.put(name, scalar(key + "." + name, value)));
        return entries;
    }

    /**
     * Returns the lists of texts a mapping of text to list holds under a key.
     *
     * @param key the key holding the mapping
     * @return the lists by entry name, in document order, empty when the key is unstated
     */
    Map<String, List<String>> listEntries(String key) {
        Object stated = fields.get(key);
        if (stated == null) {
            return Map.of();
        }
        Map<String, List<String>> entries = new LinkedHashMap<>();
        mapping(stated)
                .orElseThrow(() -> failure(origin, STRUCTURE_INVALID, "must state '" + key + "' as a mapping"))
                .forEach((name, value) -> {
                    List<String> items = new ArrayList<>();
                    list(value)
                            .orElseThrow(() -> failure(
                                    origin, STRUCTURE_INVALID, "must state '" + key + "." + name + "' as a list"))
                            .forEach(item -> items.add(scalar(key + "." + name, item)));
                    entries.put(name, List.copyOf(items));
                });
        return entries;
    }

    /**
     * Builds a failure about this document.
     *
     * @param code the published code of the cause
     * @param message what follows the document's name
     * @return the exception to throw
     */
    ConfigException failure(IssueCode code, String message) {
        return failure(origin, code, message);
    }

    /**
     * Builds a failure about this document, keeping what raised it.
     *
     * @param code the published code of the cause
     * @param message what follows the document's name
     * @param cause what refused the value
     * @return the exception to throw
     */
    ConfigException failure(IssueCode code, String message, Throwable cause) {
        return failure(origin, code, message, cause);
    }

    private String scalar(String key, Object value) {
        if (value instanceof String text) {
            return text;
        }
        throw failure(
                origin,
                STRUCTURE_INVALID,
                "must state '" + key + "' as text, but stated "
                        + (value == null ? "nothing" : value.getClass().getSimpleName()));
    }

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

    private static ConfigException failure(String origin, IssueCode code, String message) {
        return new ConfigException(diagnostic(origin, code, message));
    }

    private static ConfigException failure(String origin, IssueCode code, String message, Throwable cause) {
        return new ConfigException(diagnostic(origin, code, message), cause);
    }

    private static Diagnostic diagnostic(String origin, IssueCode code, String message) {
        return Diagnostic.builder(code, DiagnosticSeverity.ERROR, origin + " " + message)
                .build();
    }
}
