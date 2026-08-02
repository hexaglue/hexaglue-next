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

import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
     * Returns whether the given type bears this selector's symbol. Each shape knows what it looks
     * at, so recognizing a symbol is one implementation and not a switch every reader repeats.
     *
     * @param model the code model the type belongs to, read for the supertype closure
     * @param type the type under examination
     * @return true when the symbol is borne
     */
    boolean matches(CodeModel model, TypeNode type);

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

        @Override
        public boolean matches(CodeModel model, TypeNode type) {
            return type.hasAnnotation(qualifiedName);
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

        @Override
        public boolean matches(CodeModel model, TypeNode type) {
            return model.supertypesOf(type.id()).contains(TypeId.of(qualifiedName));
        }

        /**
         * Returns the supertype the type declares to reach this symbol: the reference written in
         * the source, which is where the type arguments are — {@code JpaRepository<Order, OrderId>}
         * on the way to {@code Repository}. The superclass is examined before the interfaces, and
         * the first route wins, so a type reaching one symbol by two paths still captures once.
         *
         * @param model the code model, read for the closure behind each declared supertype
         * @param type the type under examination
         * @return the declared reference leading to this symbol, empty when none does
         */
        Optional<TypeRef> declaredRouteIn(CodeModel model, TypeNode type) {
            return Stream.concat(type.superClass().stream(), type.interfaces().stream())
                    .filter(declared -> leadsToSymbol(model, declared))
                    .findFirst();
        }

        private boolean leadsToSymbol(CodeModel model, TypeRef declared) {
            return declared.qualifiedName().equals(qualifiedName)
                    || model.supertypesOf(TypeId.of(declared.qualifiedName())).contains(TypeId.of(qualifiedName));
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

        @Override
        public boolean matches(CodeModel model, TypeNode type) {
            return type.id().qualifiedName().equals(qualifiedName);
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

        @Override
        public boolean matches(CodeModel model, TypeNode type) {
            String qualifiedName = type.id().qualifiedName();
            return qualifiedName.equals(prefix) || qualifiedName.startsWith(prefix + ".");
        }
    }

    private static void requireQualifiedName(String qualifiedName, String shape) {
        Objects.requireNonNull(qualifiedName, shape + " must not be null");
        Symbols.qualifiedNameProblem(qualifiedName).ifPresent(problem -> {
            throw new IllegalArgumentException(shape + " " + problem);
        });
    }
}
