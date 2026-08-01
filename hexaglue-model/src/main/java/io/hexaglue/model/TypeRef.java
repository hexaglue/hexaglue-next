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

package io.hexaglue.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The single, recursive reference to a type as it appears in a declaration.
 *
 * <p>One hierarchy covers every syntactic shape: named types with their type arguments, primitives,
 * arrays, wildcards and type variables — a wildcard ({@code ?}) is never conflated with a type
 * variable ({@code T}). {@code List<Order>} keeps {@code Order} as a full reference, recursively,
 * so downstream rules can read {@code extends JpaRepository<Order, OrderId>} without re-parsing
 * strings.</p>
 *
 * @since 7.0.0
 */
public sealed interface TypeRef {

    /**
     * Returns the qualified name of the referenced type: the fully qualified name for a named
     * reference, the keyword for a primitive, the component's qualified name for an array, the
     * variable name for a type variable and {@code ?} for a wildcard.
     *
     * @return the qualified name
     */
    String qualifiedName();

    /**
     * Returns the type arguments, in declaration order. Only a named reference can carry
     * arguments; every other shape answers an empty list.
     *
     * @return the immutable list of type arguments
     */
    List<TypeRef> typeArguments();

    /**
     * Returns the simple name: the segment after the last {@code .} or {@code $}.
     *
     * @return the simple name
     */
    default String simpleName() {
        return QualifiedNames.simpleName(qualifiedName());
    }

    /**
     * Returns the package of the referenced type, or an empty string when it has none (primitives,
     * wildcards, type variables, unpackaged types).
     *
     * @return the package name, possibly empty
     */
    default String packageName() {
        return this instanceof Named ? QualifiedNames.packageName(qualifiedName()) : "";
    }

    /**
     * Returns whether this reference is a primitive type (including {@code void}).
     *
     * @return true for a primitive reference
     */
    default boolean isPrimitive() {
        return this instanceof Primitive;
    }

    /**
     * Returns whether this reference is an array.
     *
     * @return true for an array reference
     */
    default boolean isArray() {
        return this instanceof Array;
    }

    /**
     * Returns whether this reference carries type arguments.
     *
     * @return true when at least one type argument is present
     */
    default boolean isParameterized() {
        return !typeArguments().isEmpty();
    }

    /**
     * Returns whether this reference is a JDK optional container ({@code Optional} and its
     * primitive specializations).
     *
     * @return true for an optional-like reference
     */
    default boolean isOptionalLike() {
        return isOptionalName(qualifiedName());
    }

    /**
     * Returns whether this reference is a JDK collection ({@code List}, {@code Set}, common
     * implementations, {@code Iterable}) or an array.
     *
     * @return true for a collection-like reference
     */
    default boolean isCollectionLike() {
        return isArray() || isCollectionName(qualifiedName());
    }

    /**
     * Returns whether this reference is a JDK map.
     *
     * @return true for a map-like reference
     */
    default boolean isMapLike() {
        return isMapName(qualifiedName());
    }

    /**
     * Returns whether this reference is a JDK stream.
     *
     * @return true for a stream-like reference
     */
    default boolean isStreamLike() {
        return isStreamName(qualifiedName());
    }

    /**
     * Unwraps one container level: the component of an array, or the first type argument of a
     * parameterized optional, collection or stream. Any other reference answers itself.
     *
     * @return the element reference, or this reference when there is nothing to unwrap
     */
    default TypeRef unwrapElement() {
        if (this instanceof Array array) {
            return array.component();
        }
        if ((isOptionalLike() || isCollectionLike() || isStreamLike()) && isParameterized()) {
            return typeArguments().get(0);
        }
        return this;
    }

    /**
     * Returns the first type argument, when present.
     *
     * @return the first type argument, or empty for a non-parameterized reference
     */
    default Optional<TypeRef> firstArgument() {
        List<TypeRef> arguments = typeArguments();
        return arguments.isEmpty() ? Optional.empty() : Optional.of(arguments.get(0));
    }

    /**
     * Returns the canonical, deterministic rendering of this reference, recursively:
     * {@code java.util.List<com.acme.Order>}, {@code int[][]}, {@code ? extends com.acme.Shape}.
     *
     * @return the display string
     */
    String toDisplayString();

    /**
     * Creates a reference from a qualified name, recognizing primitive keywords.
     *
     * @param qualifiedName the fully qualified name or primitive keyword
     * @return a primitive reference for a keyword, a named reference otherwise
     */
    static TypeRef of(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        return isPrimitiveName(qualifiedName) ? new Primitive(qualifiedName) : new Named(qualifiedName, List.of());
    }

    /**
     * Creates a parameterized named reference.
     *
     * @param qualifiedName the fully qualified name of the parameterized type
     * @param typeArguments the type arguments, at least one
     * @return a named reference carrying the arguments in order
     */
    static TypeRef parameterized(String qualifiedName, TypeRef... typeArguments) {
        if (typeArguments.length == 0) {
            throw new IllegalArgumentException("parameterized requires at least one type argument; use of() otherwise");
        }
        return new Named(qualifiedName, List.of(typeArguments));
    }

    /**
     * Creates an array reference.
     *
     * @param component the component type
     * @param dimensions the number of dimensions, at least 1
     * @return an array reference
     */
    static TypeRef array(TypeRef component, int dimensions) {
        return new Array(component, dimensions);
    }

    /**
     * Creates an unbounded wildcard ({@code ?}).
     *
     * @return a wildcard reference without bounds
     */
    static TypeRef wildcard() {
        return new Wildcard(Optional.empty(), Optional.empty());
    }

    /**
     * Creates an upper-bounded wildcard ({@code ? extends Bound}).
     *
     * @param bound the upper bound
     * @return a wildcard reference with an upper bound
     */
    static TypeRef wildcardExtends(TypeRef bound) {
        return new Wildcard(Optional.of(bound), Optional.empty());
    }

    /**
     * Creates a lower-bounded wildcard ({@code ? super Bound}).
     *
     * @param bound the lower bound
     * @return a wildcard reference with a lower bound
     */
    static TypeRef wildcardSuper(TypeRef bound) {
        return new Wildcard(Optional.empty(), Optional.of(bound));
    }

    /**
     * Creates a type variable reference ({@code T}).
     *
     * @param name the variable name
     * @return a type variable reference
     */
    static TypeRef typeVariable(String name) {
        return new TypeVariable(name);
    }

    private static boolean isPrimitiveName(String name) {
        return switch (name) {
            case "boolean", "byte", "short", "int", "long", "float", "double", "char", "void" -> true;
            default -> false;
        };
    }

    private static boolean isOptionalName(String name) {
        return switch (name) {
            case "java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble" ->
                true;
            default -> false;
        };
    }

    private static boolean isCollectionName(String name) {
        return switch (name) {
            case "java.util.List",
                    "java.util.Set",
                    "java.util.Collection",
                    "java.util.Iterable",
                    "java.lang.Iterable",
                    "java.util.ArrayList",
                    "java.util.LinkedList",
                    "java.util.HashSet",
                    "java.util.TreeSet",
                    "java.util.Queue",
                    "java.util.Deque" -> true;
            default -> false;
        };
    }

    private static boolean isMapName(String name) {
        return switch (name) {
            case "java.util.Map", "java.util.HashMap", "java.util.TreeMap", "java.util.LinkedHashMap" -> true;
            default -> false;
        };
    }

    private static boolean isStreamName(String name) {
        return "java.util.stream.Stream".equals(name);
    }

    /**
     * A primitive type reference, including {@code void}.
     *
     * <p>The name must be a Java primitive keyword.</p>
     *
     * @param name the primitive keyword (e.g. {@code int})
     * @since 7.0.0
     */
    record Primitive(String name) implements TypeRef {

        /**
         * Validates that the name is a Java primitive keyword.
         */
        public Primitive {
            Objects.requireNonNull(name, "name must not be null");
            if (!isPrimitiveName(name)) {
                throw new IllegalArgumentException("not a primitive keyword: " + name);
            }
        }

        @Override
        public String qualifiedName() {
            return name;
        }

        @Override
        public List<TypeRef> typeArguments() {
            return List.of();
        }

        @Override
        public String toDisplayString() {
            return name;
        }
    }

    /**
     * A reference to a named type (class, interface, record, enum or annotation), with its type
     * arguments when parameterized.
     *
     * <p>The qualified name must be non-blank; the argument list is defensively copied.</p>
     *
     * @param qualifiedName the fully qualified name (e.g. {@code java.util.List})
     * @param typeArguments the type arguments in declaration order, possibly empty
     * @since 7.0.0
     */
    record Named(String qualifiedName, List<TypeRef> typeArguments) implements TypeRef {

        /**
         * Validates the qualified name and defensively copies the type arguments.
         */
        public Named {
            Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
            Objects.requireNonNull(typeArguments, "typeArguments must not be null");
            if (qualifiedName.isBlank()) {
                throw new IllegalArgumentException("qualifiedName must not be blank");
            }
            typeArguments = List.copyOf(typeArguments);
        }

        @Override
        public String toDisplayString() {
            return typeArguments.isEmpty()
                    ? qualifiedName
                    : qualifiedName
                            + typeArguments.stream()
                                    .map(TypeRef::toDisplayString)
                                    .collect(Collectors.joining(", ", "<", ">"));
        }
    }

    /**
     * An array reference: a component type and a number of dimensions.
     *
     * <p>The dimension count must be at least 1.</p>
     *
     * @param component the component type of the array
     * @param dimensions the number of dimensions ({@code >= 1})
     * @since 7.0.0
     */
    record Array(TypeRef component, int dimensions) implements TypeRef {

        /**
         * Validates the component and the dimension count.
         */
        public Array {
            Objects.requireNonNull(component, "component must not be null");
            if (dimensions < 1) {
                throw new IllegalArgumentException("dimensions must be >= 1, got " + dimensions);
            }
        }

        @Override
        public String qualifiedName() {
            return component.qualifiedName();
        }

        @Override
        public List<TypeRef> typeArguments() {
            return List.of();
        }

        @Override
        public String toDisplayString() {
            return component.toDisplayString() + "[]".repeat(dimensions);
        }
    }

    /**
     * A wildcard type argument ({@code ?}), optionally bounded in one direction.
     *
     * <p>A wildcard can carry an upper bound or a lower bound, never both.</p>
     *
     * @param upperBound the {@code extends} bound, when present
     * @param lowerBound the {@code super} bound, when present
     * @since 7.0.0
     */
    record Wildcard(Optional<TypeRef> upperBound, Optional<TypeRef> lowerBound) implements TypeRef {

        /**
         * Validates that at most one bound is present.
         */
        public Wildcard {
            Objects.requireNonNull(upperBound, "upperBound must not be null");
            Objects.requireNonNull(lowerBound, "lowerBound must not be null");
            if (upperBound.isPresent() && lowerBound.isPresent()) {
                throw new IllegalArgumentException("a wildcard cannot have both an upper and a lower bound");
            }
        }

        @Override
        public String qualifiedName() {
            return "?";
        }

        @Override
        public List<TypeRef> typeArguments() {
            return List.of();
        }

        @Override
        public String toDisplayString() {
            return upperBound
                    .map(bound -> "? extends " + bound.toDisplayString())
                    .or(() -> lowerBound.map(bound -> "? super " + bound.toDisplayString()))
                    .orElse("?");
        }
    }

    /**
     * A type variable reference ({@code T}), distinct from a wildcard.
     *
     * <p>The variable name must be non-blank.</p>
     *
     * @param name the declared variable name
     * @since 7.0.0
     */
    record TypeVariable(String name) implements TypeRef {

        /**
         * Validates that the variable name is non-blank.
         */
        public TypeVariable {
            Objects.requireNonNull(name, "name must not be null");
            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
        }

        @Override
        public String qualifiedName() {
            return name;
        }

        @Override
        public List<TypeRef> typeArguments() {
            return List.of();
        }

        @Override
        public String toDisplayString() {
            return name;
        }
    }
}
