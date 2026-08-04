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

package io.hexaglue.spi;

import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The options stated for one plugin, opaque to every other stage.
 *
 * <p>Only the plugin knows its own vocabulary, so the typed configuration of the model carries
 * none of this. What the contract owes instead is the strictness the configuration loader applies
 * one stage earlier: an option the plugin cannot read is an error naming the key and the value,
 * never a default applied in silence. An option nobody stated is the only case where the plugin's
 * own fallback applies.</p>
 *
 * @since 7.0.0
 */
public final class PluginConfig {

    /** A stated option has a shape the plugin cannot read. */
    private static final IssueCode OPTION_MALFORMED = IssueCode.of("HG-PLUGIN-005");

    private final String pluginId;
    private final Map<String, String> options;

    private PluginConfig(String pluginId, Map<String, String> options) {
        this.pluginId = pluginId;
        this.options = options;
    }

    /**
     * Creates the configuration of one plugin.
     *
     * @param pluginId the plugin the options belong to
     * @param options the stated options, by key
     * @return a new configuration
     */
    public static PluginConfig of(String pluginId, Map<String, String> options) {
        Objects.requireNonNull(pluginId, "pluginId must not be null");
        Objects.requireNonNull(options, "options must not be null");
        return new PluginConfig(pluginId, Collections.unmodifiableSortedMap(new TreeMap<>(options)));
    }

    /**
     * Creates the configuration of a plugin nobody configured.
     *
     * @param pluginId the plugin the options belong to
     * @return a new empty configuration
     */
    public static PluginConfig empty(String pluginId) {
        return of(pluginId, Map.of());
    }

    /**
     * Returns the value stated for a key.
     *
     * @param key the option key
     * @return the stated value, empty if the key was not stated
     */
    public Optional<String> text(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(options.get(key));
    }

    /**
     * Returns a stated flag, or the plugin's own fallback when the key was not stated.
     *
     * @param key the option key
     * @param fallback the value to use when the key was not stated
     * @return the flag
     * @throws PluginConfigException if the stated value is neither {@code true} nor {@code false}
     */
    public boolean flag(String key, boolean fallback) {
        Optional<String> stated = text(key);
        if (stated.isEmpty()) {
            return fallback;
        }
        String value = stated.get();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw malformed(key, value, "true or false");
    }

    /**
     * Returns a stated whole number, or the plugin's own fallback when the key was not stated.
     *
     * @param key the option key
     * @param fallback the value to use when the key was not stated
     * @return the number
     * @throws PluginConfigException if the stated value is not a whole number
     */
    public int number(String key, int fallback) {
        Optional<String> stated = text(key);
        if (stated.isEmpty()) {
            return fallback;
        }
        String value = stated.get();
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException notANumber) {
            throw new PluginConfigException(refusal(key, value, "a whole number"), notANumber);
        }
    }

    /**
     * Returns a stated choice among a fixed set of names, or the plugin's own fallback when the key
     * was not stated.
     *
     * <p>What is accepted is named in the refusal, in declaration order: an author who mistypes a
     * strategy is told which ones exist rather than being handed the exception a bare
     * {@code valueOf} throws, which names neither the option nor the alternatives.</p>
     *
     * @param key the option key
     * @param choices the type whose constants are the accepted values
     * @param fallback the value to use when the key was not stated
     * @param <E> the enumeration of accepted values
     * @return the chosen value
     * @throws PluginConfigException if the stated value is not one of the accepted names
     */
    public <E extends Enum<E>> E choice(String key, Class<E> choices, E fallback) {
        Objects.requireNonNull(choices, "choices must not be null");
        Objects.requireNonNull(fallback, "fallback must not be null");
        Optional<String> stated = text(key);
        if (stated.isEmpty()) {
            return fallback;
        }
        String value = stated.get().trim();
        for (E candidate : choices.getEnumConstants()) {
            if (candidate.name().equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        throw malformed(key, value, "one of " + names(choices));
    }

    private static <E extends Enum<E>> String names(Class<E> choices) {
        return Arrays.stream(choices.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
    }

    private PluginConfigException malformed(String key, String value, String expected) {
        return new PluginConfigException(refusal(key, value, expected));
    }

    private Diagnostic refusal(String key, String value, String expected) {
        return Diagnostic.builder(
                        OPTION_MALFORMED,
                        DiagnosticSeverity.WARNING,
                        "plugin " + pluginId + ": option " + key + " expects " + expected + ", got: " + value)
                .build();
    }
}
