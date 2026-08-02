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

import java.util.Objects;

/**
 * How a pack entry recognizes the symbol it knows about.
 *
 * <p>Four shapes, and no fifth: an annotation borne by the type, a supertype it inherits from, the
 * type itself, or the package it lives in. Every one of them names its symbol in full — a simple
 * name is refused at construction, because {@code Entity} is two unrelated concepts depending on
 * the package it comes from, and choosing the wrong one silently is exactly the failure this
 * hierarchy exists to prevent.</p>
 *
 * @since 7.0.0
 */
public sealed interface Selector {

    /**
     * Returns the symbol this selector matches on.
     *
     * @return the qualified name, or the package prefix
     */
    String symbol();

    /**
     * An annotation borne by the type, matched on its exact qualified name.
     *
     * @param qualifiedName the fully qualified annotation type name
     * @since 7.0.0
     */
    record Annotated(String qualifiedName) implements Selector {

        /**
         * Validates that the annotation is named in full.
         */
        public Annotated {
            requireQualifiedName(qualifiedName, "annotation");
        }

        @Override
        public String symbol() {
            return qualifiedName;
        }
    }

    /**
     * A type the subject inherits from, matched anywhere in its transitive supertype closure —
     * classpath included. Knowledge is stated on the root of a hierarchy, so a vendor interface
     * derived from it needs no entry of its own.
     *
     * @param qualifiedName the fully qualified supertype name
     * @since 7.0.0
     */
    record Supertype(String qualifiedName) implements Selector {

        /**
         * Validates that the supertype is named in full.
         */
        public Supertype {
            requireQualifiedName(qualifiedName, "supertype");
        }

        @Override
        public String symbol() {
            return qualifiedName;
        }
    }

    /**
     * The type itself, matched on its exact qualified name. This is how a tool type is known —
     * {@code EntityManager}, {@code RestTemplate} — so that holding one can be read as reaching
     * outside the hexagon.
     *
     * @param qualifiedName the fully qualified type name
     * @since 7.0.0
     */
    record Type(String qualifiedName) implements Selector {

        /**
         * Validates that the type is named in full.
         */
        public Type {
            requireQualifiedName(qualifiedName, "type");
        }

        @Override
        public String symbol() {
            return qualifiedName;
        }
    }

    /**
     * The package the type lives in, matched on the prefix and on segment boundaries: {@code feign}
     * matches {@code feign.Client}, never {@code feignedly.Client}.
     *
     * @param prefix the package prefix, without a trailing dot
     * @since 7.0.0
     */
    record PackagePrefix(String prefix) implements Selector {

        /**
         * Validates that the prefix is a package path.
         */
        public PackagePrefix {
            Objects.requireNonNull(prefix, "prefix must not be null");
            Symbols.packagePrefixProblem(prefix).ifPresent(problem -> {
                throw new IllegalArgumentException("package prefix " + problem);
            });
        }

        @Override
        public String symbol() {
            return prefix;
        }
    }

    private static void requireQualifiedName(String qualifiedName, String shape) {
        Objects.requireNonNull(qualifiedName, shape + " must not be null");
        Symbols.qualifiedNameProblem(qualifiedName).ifPresent(problem -> {
            throw new IllegalArgumentException(shape + " " + problem);
        });
    }
}
