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

package io.hexaglue.plugin.jpa;

import io.hexaglue.spi.PluginConfig;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a project can ask of the JPA backend.
 *
 * <p>Every option is read once, here, and turned into something typed: a strategy that is not one
 * of the five is refused with the five named, rather than reaching a generator as a word it will
 * fail on later. The defaults stated in the constants are the defaults this class documents —
 * there is no second list of them to fall out of step with.</p>
 *
 * @param entitySuffix what a generated entity is called after the type it stores
 * @param embeddableSuffix what a generated embeddable is called after the value it stores
 * @param repositorySuffix what a generated Spring Data interface is called after the aggregate it serves
 * @param tablePrefix what every table of this project is prefixed with
 * @param embeddables whether values are generated as embeddables at all
 * @param repositories whether repository ports are served by a generated Spring Data interface
 * @param identity who decides an identity the domain does not
 * @param targetModule the module generated types are routed to, empty when the build says nothing
 * @since 7.0.0
 */
public record JpaOptions(
        String entitySuffix,
        String embeddableSuffix,
        String repositorySuffix,
        String tablePrefix,
        boolean embeddables,
        boolean repositories,
        IdentityStrategy identity,
        Optional<String> targetModule) {

    /** The option keys this plugin answers to. */
    static final Set<String> KEYS = Set.of(
            "entitySuffix",
            "embeddableSuffix",
            "repositorySuffix",
            "tablePrefix",
            "generateEmbeddables",
            "generateRepositories",
            "idStrategy",
            "targetModule");

    private static final String DEFAULT_ENTITY_SUFFIX = "Entity";
    private static final String DEFAULT_EMBEDDABLE_SUFFIX = "Embeddable";
    private static final String DEFAULT_REPOSITORY_SUFFIX = "JpaRepository";

    /**
     * Validates the options.
     */
    public JpaOptions {
        Objects.requireNonNull(entitySuffix, "entitySuffix must not be null");
        Objects.requireNonNull(embeddableSuffix, "embeddableSuffix must not be null");
        Objects.requireNonNull(repositorySuffix, "repositorySuffix must not be null");
        Objects.requireNonNull(tablePrefix, "tablePrefix must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        Objects.requireNonNull(targetModule, "targetModule must not be null");
        requireNamePart(entitySuffix, "entitySuffix");
        requireNamePart(embeddableSuffix, "embeddableSuffix");
        requireNamePart(repositorySuffix, "repositorySuffix");
        targetModule.ifPresent(module -> {
            if (module.isBlank()) {
                throw new IllegalArgumentException("targetModule must not be blank when stated");
            }
        });
    }

    /**
     * Returns what the backend does when the build asks for nothing.
     *
     * @return the default options
     */
    public static JpaOptions defaults() {
        return new JpaOptions(
                DEFAULT_ENTITY_SUFFIX,
                DEFAULT_EMBEDDABLE_SUFFIX,
                DEFAULT_REPOSITORY_SUFFIX,
                "",
                true,
                true,
                IdentityStrategy.ASSIGNED,
                Optional.empty());
    }

    /**
     * Reads what the build states, falling back on the defaults for whatever it leaves out.
     *
     * @param config the options stated for this plugin
     * @return the options this run works under
     */
    public static JpaOptions from(PluginConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        JpaOptions defaults = defaults();
        return new JpaOptions(
                config.text("entitySuffix").orElse(defaults.entitySuffix()),
                config.text("embeddableSuffix").orElse(defaults.embeddableSuffix()),
                config.text("repositorySuffix").orElse(defaults.repositorySuffix()),
                config.text("tablePrefix").orElse(defaults.tablePrefix()),
                config.flag("generateEmbeddables", defaults.embeddables()),
                config.flag("generateRepositories", defaults.repositories()),
                config.choice("idStrategy", IdentityStrategy.class, defaults.identity()),
                config.text("targetModule"));
    }

    /**
     * Returns what the entity storing the given type is called.
     *
     * @param typeName the simple name of the domain type
     * @return the simple name of the generated entity
     */
    public String entityFor(String typeName) {
        return typeName + entitySuffix;
    }

    /**
     * Returns what the embeddable storing the given value is called.
     *
     * @param typeName the simple name of the domain value
     * @return the simple name of the generated embeddable
     */
    public String embeddableFor(String typeName) {
        return typeName + embeddableSuffix;
    }

    /**
     * Returns what the Spring Data interface serving the given aggregate is called.
     *
     * @param typeName the simple name of the aggregate
     * @return the simple name of the generated interface
     */
    public String repositoryFor(String typeName) {
        return typeName + repositorySuffix;
    }

    /**
     * A suffix becomes part of a Java type name, so what would not be one is refused here rather
     * than at the point where a file is asked for and the name has already travelled.
     */
    private static void requireNamePart(String suffix, String what) {
        for (int index = 0; index < suffix.length(); index++) {
            if (!Character.isJavaIdentifierPart(suffix.charAt(index))) {
                throw new IllegalArgumentException(what + " is not usable in a type name: " + suffix);
            }
        }
    }
}
