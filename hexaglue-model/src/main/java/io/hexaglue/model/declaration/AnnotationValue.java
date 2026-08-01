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

package io.hexaglue.model.declaration;

import io.hexaglue.model.TypeRef;
import java.util.List;
import java.util.Objects;

/**
 * The typed value of an annotation attribute: primitive, string, enum constant, class reference,
 * nested annotation or array of the above. Values are never stringified — {@code @Table(name)} or
 * a nested {@code @JoinColumn} stay readable as data all the way to the plugins.
 *
 * @since 7.0.0
 */
public sealed interface AnnotationValue {

    /**
     * Returns the kind of this value, for switch-based dispatch on Java 17.
     *
     * @return the value kind
     */
    Kind kind();

    /**
     * Kinds of annotation values, mirroring the sealed hierarchy.
     */
    enum Kind {
        /** A boxed primitive value. */
        PRIMITIVE,
        /** A string value. */
        STRING,
        /** An enum constant value. */
        ENUM,
        /** A class reference value. */
        CLASS,
        /** A nested annotation value. */
        ANNOTATION,
        /** An array of values. */
        ARRAY
    }

    /**
     * Creates a primitive value.
     *
     * @param value the boxed primitive (Boolean, Character or any Number)
     * @return an AnnotationValue
     */
    static AnnotationValue ofPrimitive(Object value) {
        return new PrimitiveValue(value);
    }

    /**
     * Creates a string value.
     *
     * @param value the string
     * @return an AnnotationValue
     */
    static AnnotationValue ofString(String value) {
        return new StringValue(value);
    }

    /**
     * Creates an enum constant value.
     *
     * @param enumType the fully qualified enum type name
     * @param constantName the constant name
     * @return an AnnotationValue
     */
    static AnnotationValue ofEnum(String enumType, String constantName) {
        return new EnumValue(enumType, constantName);
    }

    /**
     * Creates a class reference value.
     *
     * @param typeRef the referenced type
     * @return an AnnotationValue
     */
    static AnnotationValue ofClass(TypeRef typeRef) {
        return new ClassRefValue(typeRef);
    }

    /**
     * Creates a nested annotation value.
     *
     * @param annotation the nested annotation
     * @return an AnnotationValue
     */
    static AnnotationValue ofAnnotation(Annotation annotation) {
        return new NestedAnnotationValue(annotation);
    }

    /**
     * Creates an array value.
     *
     * @param values the array elements, in declaration order
     * @return an AnnotationValue
     */
    static AnnotationValue ofArray(List<AnnotationValue> values) {
        return new ArrayValue(values);
    }

    /**
     * A boxed primitive value (int, boolean, char, etc.).
     *
     * <p>The value must be a {@link Boolean}, a {@link Character} or a {@link Number}.</p>
     *
     * @param value the boxed primitive value
     * @since 7.0.0
     */
    record PrimitiveValue(Object value) implements AnnotationValue {

        /**
         * Validates that the value is a boxed primitive.
         */
        public PrimitiveValue {
            Objects.requireNonNull(value, "value must not be null");
            if (!(value instanceof Boolean || value instanceof Character || value instanceof Number)) {
                throw new IllegalArgumentException(
                        "not a boxed primitive: " + value.getClass().getName());
            }
        }

        @Override
        public Kind kind() {
            return Kind.PRIMITIVE;
        }

        /**
         * Returns the value as an int.
         *
         * @return the int value
         */
        public int asInt() {
            return ((Number) value).intValue();
        }

        /**
         * Returns the value as a long.
         *
         * @return the long value
         */
        public long asLong() {
            return ((Number) value).longValue();
        }

        /**
         * Returns the value as a double.
         *
         * @return the double value
         */
        public double asDouble() {
            return ((Number) value).doubleValue();
        }

        /**
         * Returns the value as a boolean.
         *
         * @return the boolean value
         */
        public boolean asBoolean() {
            return (Boolean) value;
        }

        /**
         * Returns the value as a char.
         *
         * @return the char value
         */
        public char asChar() {
            return (Character) value;
        }
    }

    /**
     * A string value.
     *
     * @param value the string value
     * @since 7.0.0
     */
    record StringValue(String value) implements AnnotationValue {

        /**
         * Validates the value.
         */
        public StringValue {
            Objects.requireNonNull(value, "value must not be null");
        }

        @Override
        public Kind kind() {
            return Kind.STRING;
        }
    }

    /**
     * An enum constant value.
     *
     * @param enumType the fully qualified enum type name
     * @param constantName the enum constant name
     * @since 7.0.0
     */
    record EnumValue(String enumType, String constantName) implements AnnotationValue {

        /**
         * Validates the type and constant names.
         */
        public EnumValue {
            Objects.requireNonNull(enumType, "enumType must not be null");
            Objects.requireNonNull(constantName, "constantName must not be null");
            if (enumType.isBlank() || constantName.isBlank()) {
                throw new IllegalArgumentException("enumType and constantName must not be blank");
            }
        }

        @Override
        public Kind kind() {
            return Kind.ENUM;
        }
    }

    /**
     * A class reference value ({@code @Annotation(Some.class)}).
     *
     * @param typeRef the referenced type
     * @since 7.0.0
     */
    record ClassRefValue(TypeRef typeRef) implements AnnotationValue {

        /**
         * Validates the reference.
         */
        public ClassRefValue {
            Objects.requireNonNull(typeRef, "typeRef must not be null");
        }

        @Override
        public Kind kind() {
            return Kind.CLASS;
        }

        /**
         * Returns the qualified name of the referenced class.
         *
         * @return the qualified class name
         */
        public String qualifiedName() {
            return typeRef.qualifiedName();
        }
    }

    /**
     * A nested annotation value ({@code @Table(indexes = @Index(...))}).
     *
     * @param annotation the nested annotation, with its own typed values
     * @since 7.0.0
     */
    record NestedAnnotationValue(Annotation annotation) implements AnnotationValue {

        /**
         * Validates the nested annotation.
         */
        public NestedAnnotationValue {
            Objects.requireNonNull(annotation, "annotation must not be null");
        }

        @Override
        public Kind kind() {
            return Kind.ANNOTATION;
        }
    }

    /**
     * An array of annotation values, in declaration order.
     *
     * @param values the array elements
     * @since 7.0.0
     */
    record ArrayValue(List<AnnotationValue> values) implements AnnotationValue {

        /**
         * Defensively copies the elements.
         */
        public ArrayValue {
            Objects.requireNonNull(values, "values must not be null");
            values = List.copyOf(values);
        }

        @Override
        public Kind kind() {
            return Kind.ARRAY;
        }

        /**
         * Returns the string elements of this array, ignoring non-string elements.
         *
         * @return the string values, in order
         */
        public List<String> asStrings() {
            return values.stream()
                    .filter(StringValue.class::isInstance)
                    .map(value -> ((StringValue) value).value())
                    .toList();
        }
    }
}
